package io.github.willqi.pizzaserver.server.world.chunks;

import io.github.willqi.pizzaserver.api.player.Player;
import io.github.willqi.pizzaserver.api.world.chunks.Chunk;
import io.github.willqi.pizzaserver.api.world.chunks.ChunkManager;
import io.github.willqi.pizzaserver.commons.utils.Check;
import io.github.willqi.pizzaserver.commons.utils.ReadWriteKeyLock;
import io.github.willqi.pizzaserver.commons.utils.Tuple;
import io.github.willqi.pizzaserver.format.api.chunks.BedrockChunk;
import io.github.willqi.pizzaserver.server.player.ImplPlayer;
import io.github.willqi.pizzaserver.server.world.ImplWorld;
import io.github.willqi.pizzaserver.server.world.chunks.processing.ChunkQueue;
import io.github.willqi.pizzaserver.server.world.chunks.processing.requests.PlayerChunkRequest;
import io.github.willqi.pizzaserver.server.world.chunks.processing.requests.UnloadChunkRequest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImplChunkManager implements ChunkManager {

    private final ImplWorld world;
    private final Map<Tuple<Integer, Integer>, ImplChunk> chunks = new ConcurrentHashMap<>();
    private final ReadWriteKeyLock<Tuple<Integer, Integer>> lock = new ReadWriteKeyLock<>();

    private final ChunkQueue chunkQueue = new ChunkQueue(this);


    public ImplChunkManager(ImplWorld world) {
        this.world = world;
    }

    /**
     * Tick all chunks and the chunk queue
     */
    public void tick() {
        this.chunkQueue.tick();
        for (ImplChunk chunk : this.chunks.values()) {
            chunk.tick();
        }
    }

    @Override
    public boolean isChunkLoaded(int x, int z) {
        Tuple<Integer, Integer> key = new Tuple<>(x, z);
        this.lock.readLock(key);
        try {
            return this.chunks.containsKey(key);
        } finally {
            this.lock.readUnlock(key);
        }
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return this.getChunk(x, z, true);
    }

    @Override
    public Chunk getChunk(int x, int z, boolean loadFromProvider) {
        Tuple<Integer, Integer> key = new Tuple<>(x, z);
        this.lock.readLock(key);

        try {
            this.chunks.computeIfAbsent(key, v -> {
                if (!loadFromProvider) {
                    return null;
                }

                // Load chunk from provider
                ImplChunk chunk = null;
                try {
                    BedrockChunk internalChunk = this.getWorld().getProvider().getChunk(x, z);

                    chunk = new ImplChunk.Builder()
                            .setWorld(world)
                            .setX(internalChunk.getX())
                            .setZ(internalChunk.getZ())
                            .setSubChunks(internalChunk.getSubChunks())
                            .build();
                } catch (IOException exception) {
                    this.getWorld().getServer().getLogger().error(String.format("Failed to retrieve chunk (%s, %s) from provider", x, z), exception);
                }
                return chunk;
            });

            return this.chunks.getOrDefault(key, null);
        } finally {
            this.lock.readUnlock(key);
        }
    }

    @Override
    public void unloadChunk(int x, int z) {
        this.unloadChunk(x, z, false, false);
    }

    @Override
    public void unloadChunk(int x, int z, boolean async, boolean force) {
        if (async) {
            this.chunkQueue.addRequest(new UnloadChunkRequest(x, z));
        } else {
            Tuple<Integer, Integer> key = new Tuple<>(x, z);
            this.lock.writeLock(key);

            try {
                Chunk chunk = this.chunks.getOrDefault(key, null);
                if (Check.isNull(chunk) || (!chunk.canBeClosed() && !force)) {
                    return;
                }

                this.chunks.remove(key);
                chunk.close();
            } finally {
                this.lock.writeUnlock(key);
            }
        }
    }

    @Override
    public void sendPlayerChunk(Player player, int x, int z) {
        this.sendPlayerChunk(player, x, z, false);
    }

    @Override
    public void sendPlayerChunk(Player player, int x, int z, boolean async) {
        if (async) {
            this.chunkQueue.addRequest(new PlayerChunkRequest((ImplPlayer)player, x, z));
        } else {
            Tuple<Integer, Integer> key = new Tuple<>(x, z);
            this.lock.readLock(key);
            try {
                Chunk chunk = this.getChunk(x, z);
                chunk.sendTo(player);
            } finally {
                this.lock.readUnlock(key);
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.chunkQueue.close();
        for (Chunk chunk : this.chunks.values()) {
            this.unloadChunk(chunk.getX(), chunk.getZ(), false, true);
        }
    }

    @Override
    public ImplWorld getWorld() {
        return this.world;
    }

}
