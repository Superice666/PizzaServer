package io.github.willqi.pizzaserver.server.entity.inventory;

import io.github.willqi.pizzaserver.api.entity.Entity;
import io.github.willqi.pizzaserver.api.entity.inventory.Inventory;
import io.github.willqi.pizzaserver.api.entity.inventory.InventorySlotType;
import io.github.willqi.pizzaserver.api.event.type.inventory.InventoryCloseEvent;
import io.github.willqi.pizzaserver.api.event.type.inventory.InventoryOpenEvent;
import io.github.willqi.pizzaserver.api.item.ItemRegistry;
import io.github.willqi.pizzaserver.api.item.ItemStack;
import io.github.willqi.pizzaserver.api.level.world.blocks.types.BlockTypeID;
import io.github.willqi.pizzaserver.api.player.Player;
import io.github.willqi.pizzaserver.server.network.protocol.packets.ContainerClosePacket;
import io.github.willqi.pizzaserver.server.network.protocol.packets.InventoryContentPacket;
import io.github.willqi.pizzaserver.server.network.protocol.packets.InventorySlotPacket;

import java.util.*;

public abstract class BaseInventory implements Inventory {

    public static int ID = 1;

    protected final Entity entity;
    protected final int id;
    protected final int size;

    protected ItemStack[] slots;
    protected final Set<InventorySlotType> slotTypes;

    private final Set<Player> viewers = new HashSet<>();


    public BaseInventory(Entity entity, Set<InventorySlotType> slotTypes, int size) {
        this(entity, slotTypes, size, ID++);
    }

    public BaseInventory(Entity entity, Set<InventorySlotType> slotTypes, int size, int id) {
        this.entity = entity;
        this.size = size;
        this.id = id;
        this.slotTypes = slotTypes;
        this.slots = new ItemStack[this.size];
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public Set<InventorySlotType> getSlotTypes() {
        return this.slotTypes;
    }

    @Override
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public ItemStack[] getSlots() {
        ItemStack[] slots = new ItemStack[this.getSize()];
        for (int i = 0; i < this.getSize(); i++) {
            slots[i] = this.getSlot(i); // These slots are cloned
        }
        return slots;
    }

    @Override
    public void setSlots(ItemStack[] slots) {
        if (slots.length != this.size) {
            throw new IllegalArgumentException("The slots provided must be " + this.size + " in length.");
        }

        for (int i = 0 ; i < this.size; i++) {
            this.slots[i] = slots[i].newNetworkStack();
        }

        for (Player viewer : this.getViewers()) {
            this.sendSlots(viewer);
        }
    }

    @Override
    public ItemStack getSlot(int slot) {
        return this.getSlot(slot, true);
    }

    /**
     * Get a slot in the inventory
     * @param slot the slot
     * @param clone if the ItemStack should be cloned or if it should retrieve the actual object
     * @return ItemStack in that slot
     */
    public ItemStack getSlot(int slot, boolean clone) {
        ItemStack itemStack = Optional.ofNullable(this.slots[slot]).orElse(ItemRegistry.getItem(BlockTypeID.AIR));
        if (clone) {
            return itemStack.clone();
        } else {
            return itemStack;
        }
    }

    @Override
    public void setSlot(int slot, ItemStack itemStack) {
        this.setSlot(null, slot, itemStack, false);
    }

    /**
     * Change a slot in the inventory
     * @param player the player who is changing the slot if any exists
     * @param slot the slot changed
     * @param itemStack the new item stack
     * @param keepNetworkId if the network id of the ItemStack should be kept or if a new one should be generated
     */
    public void setSlot(Player player, int slot, ItemStack itemStack, boolean keepNetworkId) {
        this.slots[slot] = keepNetworkId ? ItemStack.ensureItemStackExists(itemStack) : ItemStack.ensureItemStackExists(itemStack).newNetworkStack();

        for (Player viewer : this.getViewers()) {
            if (!viewer.equals(player)) {
                sendSlot(viewer, this.getSlot(slot), slot, this.getId());
            }
        }
    }

    @Override
    public Optional<ItemStack> addItem(ItemStack item) {
        return Optional.empty();
    }

    @Override
    public Set<ItemStack> addItems(Collection<ItemStack> itemStacks) {
        return this.addItems(itemStacks.toArray(new ItemStack[0]));
    }

    @Override
    public Set<ItemStack> addItems(ItemStack... itemStacks) {
        Set<ItemStack> failedToAddItems = new HashSet<>();
        for (ItemStack itemStack : itemStacks) {
            this.addItem(itemStack).ifPresent(failedToAddItems::add);
        }
        return failedToAddItems;
    }

    @Override
    public void sendSlots(Player player) {
        InventoryContentPacket inventoryContentPacket = new InventoryContentPacket();
        inventoryContentPacket.setInventoryId(this.getId());
        inventoryContentPacket.setContents(this.getSlots());
        player.sendPacket(inventoryContentPacket);
    }

    @Override
    public boolean canBeOpenedBy(Player player) {
        return true;
    }

    /**
     * Tries to open the inventory
     * @param player the player to open this inventory to
     * @return if the inventory was opened
     */
    public boolean openFor(Player player) {
        if (!this.viewers.contains(player)) {
            InventoryOpenEvent inventoryOpenEvent = new InventoryOpenEvent(player, this);
            this.getEntity().getServer().getEventManager().call(inventoryOpenEvent);
            if (inventoryOpenEvent.isCancelled()) {
                return false;
            }

            this.viewers.add(player);
            this.sendContainerOpenPacket(player);
            return true;
        } else {
            return false;
        }
    }

    protected abstract void sendContainerOpenPacket(Player player);

    /**
     * Close this inventory for a player
     * @param player the player to close this inventory for
     * @return if the inventory was closed
     */
    public boolean closeFor(Player player) {
        if (this.viewers.contains(player)) {
            ContainerClosePacket containerClosePacket = new ContainerClosePacket();
            containerClosePacket.setInventoryId(this.getId());
            player.sendPacket(containerClosePacket);

            InventoryCloseEvent inventoryCloseEvent = new InventoryCloseEvent(player, this);
            this.getEntity().getServer().getEventManager().call(inventoryCloseEvent);

            this.viewers.remove(player);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Set<Player> getViewers() {
        return Collections.unmodifiableSet(this.viewers);
    }

    protected static void sendSlot(Player player, ItemStack itemStack, int slot, int inventoryId) {
        InventorySlotPacket inventorySlotPacket = new InventorySlotPacket();
        inventorySlotPacket.setInventoryId(inventoryId);
        inventorySlotPacket.setSlot(slot);
        inventorySlotPacket.setItem(itemStack);
        player.sendPacket(inventorySlotPacket);
    }

}
