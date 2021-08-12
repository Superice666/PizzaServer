package io.github.willqi.pizzaserver.server;

import io.github.willqi.pizzaserver.api.Server;
import io.github.willqi.pizzaserver.api.event.EventManager;
import io.github.willqi.pizzaserver.api.player.Player;
import io.github.willqi.pizzaserver.api.plugin.PluginManager;
import io.github.willqi.pizzaserver.api.scheduler.Scheduler;
import io.github.willqi.pizzaserver.api.utils.Logger;
import io.github.willqi.pizzaserver.api.world.blocks.BlockRegistry;
import io.github.willqi.pizzaserver.server.network.BedrockNetworkServer;
import io.github.willqi.pizzaserver.server.event.ImplEventManager;
import io.github.willqi.pizzaserver.server.network.BedrockClientSession;
import io.github.willqi.pizzaserver.server.network.handlers.LoginPacketHandler;
import io.github.willqi.pizzaserver.server.network.protocol.ServerProtocol;
import io.github.willqi.pizzaserver.server.packs.ImplResourcePackManager;
import io.github.willqi.pizzaserver.server.player.ImplPlayer;
import io.github.willqi.pizzaserver.server.plugin.ImplPluginManager;
import io.github.willqi.pizzaserver.server.scheduler.ImplScheduler;
import io.github.willqi.pizzaserver.server.utils.Config;
import io.github.willqi.pizzaserver.server.utils.ImplLogger;
import io.github.willqi.pizzaserver.server.utils.TimeUtils;
import io.github.willqi.pizzaserver.server.world.ImplWorldManager;
import io.github.willqi.pizzaserver.server.world.blocks.ImplBlockRegistry;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ImplServer implements Server {

    private static Server INSTANCE;

    private final BedrockNetworkServer network = new BedrockNetworkServer(this);
    private final PluginManager pluginManager = new ImplPluginManager(this);
    private final ImplResourcePackManager dataPackManager = new ImplResourcePackManager(this);
    private final ImplWorldManager worldManager = new ImplWorldManager(this);
    private final EventManager eventManager = new ImplEventManager(this);

    private final Set<ImplScheduler> syncedSchedulers = Collections.synchronizedSet(new HashSet<>());
    private final ImplScheduler scheduler = new ImplScheduler(this, 1);

    private final BlockRegistry blockRegistry = new ImplBlockRegistry();

    private final Logger logger = new ImplLogger("Server");

    private final Set<BedrockClientSession> sessions = Collections.synchronizedSet(new HashSet<>());

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private int targetTps;
    private int currentTps;
    private volatile boolean running;
    private final String rootDirectory;

    private int maximumPlayersAllowed;
    private String motd;

    private String ip;
    private int port;

    private ServerConfig config;


    public ImplServer(String rootDirectory) {
        INSTANCE = this;
        this.rootDirectory = rootDirectory;

        this.getLogger().info("Setting up PizzaServer instance.");
        Runtime.getRuntime().addShutdownHook(new ServerExitListener());

        // Load required data/files
        this.setupFiles();

        this.getLogger().info("Internal setup complete.");
        // TODO: load plugins
    }

    /**
     * Start ticking the Minecraft server.
     * Does not create a new thread and will block the thread that calls this method until shutdown.
     */
    public void boot() {
        ServerProtocol.loadVersions();
        this.getResourcePackManager().loadPacks();
        this.setTargetTps(20);

        this.getWorldManager().loadWorlds();

        try {
            this.getNetwork().boot(this.getIp(), this.getPort());
        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }
        this.running = true;
        this.scheduler.startScheduler();

        int currentTps = 0;
        long nextTpsRecording = 0;
        long sleepTime = 0;    // The amount of nanoseconds to sleep for
                               // This fluctuates depending on if we were at a slower/faster tps before
        while (this.running) {
            long idealNanoSleepPerTick = TimeUtils.secondsToNanoSeconds(1) / this.targetTps;

            // Figure out how long it took to tick
            long startTickTime = System.nanoTime();
            this.tick();
            currentTps++;
            long timeTakenToTick = System.nanoTime() - startTickTime;

            // Sleep for the ideal time but take into account the time spent running the tick
            sleepTime += idealNanoSleepPerTick - timeTakenToTick;
            long sleepStart = System.nanoTime();
            try {
                Thread.sleep(Math.max(TimeUtils.nanoSecondsToMilliseconds(sleepTime), 0));
            } catch (InterruptedException exception) {
                exception.printStackTrace();
                this.stop();
                return;
            }
            sleepTime -= System.nanoTime() - sleepStart;    // How long did it actually take to sleep?
                                                            // If we didn't sleep for the correct amount,
                                                            // take that into account for the next sleep by
                                                            // leaving extra/less for the next sleep.

            // Record TPS every second
            if (System.nanoTime() > nextTpsRecording) {
                this.currentTps = currentTps;
                currentTps = 0;
                nextTpsRecording = System.nanoTime() + TimeUtils.secondsToNanoSeconds(1);
            }
        }
        this.stop();
    }

    private void tick() {
        synchronized (this.sessions) {
            // Process all packets that are outgoing and incoming
            Iterator<BedrockClientSession> sessions = this.sessions.iterator();
            while (sessions.hasNext()) {
                BedrockClientSession session = sessions.next();
                try {
                    session.processPackets();
                } catch (Exception exception) {
                    session.disconnect();
                    this.getLogger().error("Disconnecting session due to failure in processing packets", exception);
                }

                // check if the client disconnected
                if (session.isDisconnected()) {
                    sessions.remove();
                    ImplPlayer player = session.getPlayer();
                    if (player != null) {
                        player.onDisconnect();
                        this.getNetwork().updatePong();
                    }
                }
            }
        }

        this.getWorldManager().tick();

        for (ImplScheduler scheduler : this.syncedSchedulers) {
            try {
                scheduler.serverTick();
            } catch (Exception exception) {
                this.getLogger().error("Failed to tick scheduler", exception);
            }
        }
    }

    /**
     * The server will stop after the current tick finishes.
     */
    private void stop() {
        this.getLogger().info("Stopping server...");

        for (BedrockClientSession session : this.sessions) {
            if (session.getPlayer() != null) {
                session.getPlayer().disconnect("Server Stopped");
            } else {
                session.disconnect();
            }
        }

        this.getNetwork().stop();
        this.getWorldManager().unloadWorlds();

        // We're done stop operations. Exit program.
        this.shutdownLatch.countDown();
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public BedrockNetworkServer getNetwork() {
        return this.network;
    }

    public void registerSession(BedrockClientSession session) {
        session.setPacketHandler(new LoginPacketHandler(this, session));
        this.sessions.add(session);
    }

    @Override
    public String getMotd() {
        return this.motd;
    }

    @Override
    public void setMotd(String motd) {
        this.motd = motd;
    }

    @Override
    public Set<Player> getPlayers() {
        return this.sessions.stream()
                .map(BedrockClientSession::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public int getPlayerCount() {
        return this.getPlayers().size();
    }

    @Override
    public void setMaximumPlayerCount(int players) {
        this.maximumPlayersAllowed = players;
    }

    @Override
    public int getMaximumPlayerCount() {
        return this.maximumPlayersAllowed;
    }

    @Override
    public int getTargetTps() {
        return this.targetTps;
    }

    @Override
    public void setTargetTps(int newTps) {
        this.targetTps = newTps;
    }

    @Override
    public int getCurrentTps() {
        return this.currentTps;
    }

    @Override
    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    @Override
    public ImplResourcePackManager getResourcePackManager() {
        return this.dataPackManager;
    }

    @Override
    public ImplWorldManager getWorldManager() {
        return this.worldManager;
    }

    @Override
    public EventManager getEventManager() {
        return this.eventManager;
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return this.blockRegistry;
    }

    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    public Set<Scheduler> getSyncedSchedulers() {
        return Collections.unmodifiableSet(this.syncedSchedulers);
    }

    public void syncScheduler(ImplScheduler scheduler) {
        if(scheduler.isRunning()) this.syncedSchedulers.add(scheduler);
    }

    public boolean desyncScheduler(ImplScheduler scheduler) {
        return this.syncedSchedulers.remove(scheduler);
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public String getRootDirectory() {
        return this.rootDirectory;
    }

    public ServerConfig getConfig() {
        return this.config;
    }

    public static Server getInstance() {
        return INSTANCE;
    }

    /**
     * Called to load and setup required files/classes.
     */
    private void setupFiles() {
        try {
            new File(this.getRootDirectory() + "/plugins").mkdirs();
            new File(this.getRootDirectory() + "/worlds").mkdirs();
            new File(this.getRootDirectory() + "/players").mkdirs();
            new File(this.getRootDirectory() + "/resourcepacks").mkdirs();
            new File(this.getRootDirectory() + "/behaviorpacks").mkdirs();
        } catch (SecurityException exception) {
            throw new RuntimeException(exception);
        }

        File propertiesFile = new File(this.getRootDirectory() + "/server.yml");
        Config config = new Config();

        try {
            InputStream propertiesStream;
            if (propertiesFile.exists()) {
                propertiesStream = new FileInputStream(propertiesFile);
            } else {
                propertiesStream = this.getClass().getResourceAsStream("/server.yml");
            }
            config.load(propertiesStream);
            if (!propertiesFile.exists()) {
                config.save(new FileOutputStream(propertiesFile));
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.config = new ServerConfig(config);

        this.ip = this.config.getIp();
        this.port = this.config.getPort();

        this.setMotd(this.config.getMotd());
        this.setMaximumPlayerCount(this.config.getMaximumPlayers());
        this.dataPackManager.setPacksRequired(this.config.arePacksForced());
    }


    private class ServerExitListener extends Thread {

        @Override
        public void run() {
            if (ImplServer.this.running) {
                ImplServer.this.running = false;
                try {
                    ImplServer.this.shutdownLatch.await();
                } catch (InterruptedException exception) {
                    ImplServer.getInstance().getLogger().error("Exit listener exception", exception);
                }
            }
        }
    }

}
