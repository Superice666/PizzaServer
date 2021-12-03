package io.github.pizzaserver.server.entity.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.MobArmorEquipmentPacket;
import com.nukkitx.protocol.bedrock.packet.MobEquipmentPacket;
import io.github.pizzaserver.api.entity.Entity;
import io.github.pizzaserver.api.entity.inventory.EntityInventory;
import io.github.pizzaserver.api.item.ItemRegistry;
import io.github.pizzaserver.api.item.ItemStack;
import io.github.pizzaserver.api.block.types.BlockTypeID;
import io.github.pizzaserver.api.player.Player;
import io.github.pizzaserver.server.item.ItemUtils;

import java.util.Optional;

public class ImplEntityInventory extends BaseInventory implements EntityInventory {

    protected final Entity entity;

    protected ItemStack helmet = null;
    protected ItemStack chestplate = null;
    protected ItemStack leggings = null;
    protected ItemStack boots = null;

    protected ItemStack mainHand = null;
    protected ItemStack offHand = null;


    public ImplEntityInventory(Entity entity, ContainerType containerType, int size) {
        super(containerType, size);
        this.entity = entity;
    }

    public ImplEntityInventory(Entity entity, ContainerType containerType, int size, int id) {
        super(containerType, size, id);
        this.entity = entity;
    }

    @Override
    public void clear() {
        super.clear();
        this.setHeldItem(null);
        this.setOffhandItem(null);
        this.setArmour(null, null, null, null);
    }

    @Override
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public void setArmour(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        this.setArmour(helmet, chestplate, leggings, boots, false);
    }

    public void setArmour(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots, boolean keepNetworkId) {
        this.setHelmet(helmet, keepNetworkId);
        this.setChestplate(chestplate, keepNetworkId);
        this.setLeggings(leggings, keepNetworkId);
        this.setBoots(boots, keepNetworkId);
    }

    @Override
    public ItemStack getHelmet() {
        return this.getHelmet(true);
    }

    public ItemStack getHelmet(boolean clone) {
        ItemStack helmet = Optional.ofNullable(this.helmet).orElse(ItemRegistry.getInstance().getItem(BlockTypeID.AIR));
        if (clone) {
            return helmet.clone();
        } else {
            return helmet;
        }
    }

    @Override
    public void setHelmet(ItemStack helmet) {
        this.setHelmet(helmet, false);
    }

    public void setHelmet(ItemStack helmet, boolean keepNetworkId) {
        this.helmet = keepNetworkId ? helmet : ItemStack.ensureItemStackExists(helmet).newNetworkStack();
        this.broadcastMobArmourEquipmentPacket(); // TODO when entity support is implemented: check if entity supports armor before sending
    }

    @Override
    public ItemStack getChestplate() {
        return this.getChestplate(true);
    }

    public ItemStack getChestplate(boolean clone) {
        ItemStack chestplate = Optional.ofNullable(this.chestplate).orElse(ItemRegistry.getInstance().getItem(BlockTypeID.AIR));
        if (clone) {
            return chestplate.clone();
        } else {
            return chestplate;
        }
    }

    @Override
    public void setChestplate(ItemStack chestplate) {
        this.setChestplate(chestplate, false);
    }

    public void setChestplate(ItemStack chestplate, boolean keepNetworkId) {
        this.chestplate = keepNetworkId ? chestplate : ItemStack.ensureItemStackExists(chestplate).newNetworkStack();
        this.broadcastMobArmourEquipmentPacket(); // TODO when entity support is implemented: check if entity supports armor before sending
    }

    @Override
    public ItemStack getLeggings() {
        return this.getLeggings(true);
    }

    public ItemStack getLeggings(boolean clone) {
        ItemStack leggings = Optional.ofNullable(this.leggings).orElse(ItemRegistry.getInstance().getItem(BlockTypeID.AIR));
        if (clone) {
            return leggings.clone();
        } else {
            return leggings;
        }
    }

    @Override
    public void setLeggings(ItemStack leggings) {
        this.setLeggings(leggings, false);
    }

    public void setLeggings(ItemStack leggings, boolean keepNetworkId) {
        this.leggings = keepNetworkId ? leggings : ItemStack.ensureItemStackExists(leggings).newNetworkStack();
        this.broadcastMobArmourEquipmentPacket(); // TODO when entity support is implemented: check if entity supports armor before sending
    }

    @Override
    public ItemStack getBoots() {
        return this.getBoots(true);
    }

    public ItemStack getBoots(boolean clone) {
        ItemStack boots = Optional.ofNullable(this.boots).orElse(ItemRegistry.getInstance().getItem(BlockTypeID.AIR));
        if (clone) {
            return boots.clone();
        } else {
            return boots;
        }
    }

    @Override
    public void setBoots(ItemStack boots) {
        this.setBoots(boots, false);
    }

    public void setBoots(ItemStack boots, boolean keepNetworkId) {
        this.boots = keepNetworkId ? boots : ItemStack.ensureItemStackExists(boots).newNetworkStack();
        this.broadcastMobArmourEquipmentPacket(); // TODO when entity support is implemented: check if entity supports armor before sending
    }

    protected void broadcastMobArmourEquipmentPacket() {
        for (Player player : this.getEntity().getViewers()) {
            MobArmorEquipmentPacket mobArmourEquipmentPacket = new MobArmorEquipmentPacket();
            mobArmourEquipmentPacket.setRuntimeEntityId(this.getEntity().getId());
            mobArmourEquipmentPacket.setHelmet(ItemUtils.serializeForNetwork(this.getHelmet(), player.getVersion()));
            mobArmourEquipmentPacket.setChestplate(ItemUtils.serializeForNetwork(this.getChestplate(), player.getVersion()));
            mobArmourEquipmentPacket.setLeggings(ItemUtils.serializeForNetwork(this.getLeggings(), player.getVersion()));
            mobArmourEquipmentPacket.setBoots(ItemUtils.serializeForNetwork(this.getBoots(), player.getVersion()));
            player.sendPacket(mobArmourEquipmentPacket);
        }
    }

    @Override
    public ItemStack getHeldItem() {
        return this.getHeldItem(true);
    }

    public ItemStack getHeldItem(boolean clone) {
        ItemStack mainHand = Optional.ofNullable(this.mainHand).orElse(ItemRegistry.getInstance().getItem(BlockTypeID.AIR));
        if (clone) {
            return mainHand.clone();
        } else {
            return mainHand;
        }
    }

    @Override
    public void setHeldItem(ItemStack mainHand) {
        this.setHeldItem(mainHand, false);
    }

    public void setHeldItem(ItemStack mainHand, boolean keepNetworkId) {
        this.mainHand = keepNetworkId ? mainHand : ItemStack.ensureItemStackExists(mainHand).newNetworkStack();
        this.broadcastMobEquipmentPacket(this.getHeldItem(), 0, true); // TODO when entity support is implemented: check if entity supports armor before sending
    }

    @Override
    public ItemStack getOffhandItem() {
        return this.getOffhandItem(true);
    }

    public ItemStack getOffhandItem(boolean clone) {
        ItemStack offhand = Optional.ofNullable(this.offHand).orElse(ItemRegistry.getInstance().getItem(BlockTypeID.AIR));
        if (clone) {
            return offhand.clone();
        } else {
            return offhand;
        }
    }

    @Override
    public void setOffhandItem(ItemStack offHand) {
        this.setOffhandItem(offHand, false);
    }

    public void setOffhandItem(ItemStack offHand, boolean keepNetworkId) {
        this.offHand = keepNetworkId ? offHand : ItemStack.ensureItemStackExists(offHand).newNetworkStack();
        this.broadcastMobEquipmentPacket(this.getHeldItem(), 1, false); // TODO when entity support is implemented: check if entity supports armor before sending
    }

    /**
     * Broadcasts mob equipment packet to all viewers of this entity.
     * @param itemStack the item stack being sent
     * @param slot the slot to send it as
     * @param mainHand if the item is in the main hand
     */
    protected void broadcastMobEquipmentPacket(ItemStack itemStack, int slot, boolean mainHand) {
        for (Player player : this.getEntity().getViewers()) {
            MobEquipmentPacket mobEquipmentPacket = new MobEquipmentPacket();
            mobEquipmentPacket.setRuntimeEntityId(this.getEntity().getId());
            mobEquipmentPacket.setContainerId(mainHand ? ContainerId.INVENTORY : ContainerId.OFFHAND);
            mobEquipmentPacket.setInventorySlot(slot);
            mobEquipmentPacket.setHotbarSlot(slot);
            mobEquipmentPacket.setItem(ItemUtils.serializeForNetwork(ItemStack.ensureItemStackExists(itemStack), player.getVersion()));
            player.sendPacket(mobEquipmentPacket);
        }
    }

    @Override
    public boolean canBeOpenedBy(Player player) {
        return this.getEntity().getViewers().contains(player) && super.canBeOpenedBy(player);
    }

    @Override
    public boolean shouldBeClosedFor(Player player) {
        return !player.canReach(this.getEntity()) && super.shouldBeClosedFor(player);
    }

    @Override
    protected void sendContainerOpenPacket(Player player) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setId((byte) this.getId());
        containerOpenPacket.setType(this.getContainerType());
        containerOpenPacket.setUniqueEntityId(this.getEntity().getId());
        player.sendPacket(containerOpenPacket);

        this.sendSlots(player);
    }

}
