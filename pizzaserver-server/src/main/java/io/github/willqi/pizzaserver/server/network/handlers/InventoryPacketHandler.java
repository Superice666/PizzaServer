package io.github.willqi.pizzaserver.server.network.handlers;

import io.github.willqi.pizzaserver.server.network.BaseBedrockPacketHandler;
import io.github.willqi.pizzaserver.server.network.handlers.inventory.InventoryActionTakeHandler;
import io.github.willqi.pizzaserver.server.network.protocol.data.inventory.actions.*;
import io.github.willqi.pizzaserver.server.network.protocol.packets.ContainerClosePacket;
import io.github.willqi.pizzaserver.server.network.protocol.packets.InteractPacket;
import io.github.willqi.pizzaserver.server.network.protocol.packets.ItemStackRequestPacket;
import io.github.willqi.pizzaserver.server.network.protocol.packets.ItemStackResponsePacket;
import io.github.willqi.pizzaserver.server.player.ImplPlayer;

import java.util.HashSet;
import java.util.Set;

public class InventoryPacketHandler extends BaseBedrockPacketHandler {
    private final ImplPlayer player;


    public InventoryPacketHandler(ImplPlayer player) {
        this.player = player;
    }

    @Override
    public void onPacket(InteractPacket packet) {
        if (packet.getAction() == InteractPacket.Type.OPEN_INVENTORY && !this.player.getOpenInventory().isPresent()) {
            this.player.openInventory(this.player.getInventory());
        }
    }

    @Override
    public void onPacket(ContainerClosePacket packet) {
        this.player.closeOpenInventory();
    }

    @Override
    public void onPacket(ItemStackRequestPacket packet) {
        ItemStackResponsePacket itemStackResponsePacket = new ItemStackResponsePacket();
        Set<ItemStackResponsePacket.Response> responses = new HashSet<>();

        for (ItemStackRequestPacket.Request request : packet.getRequests()) {
            int requestId = request.getId();
            ItemStackResponsePacket.Response response = new ItemStackResponsePacket.Response(requestId);

            boolean continueActions = true;
            for (InventoryAction action : request.getActions()) {
                if (!continueActions) {  // e.g. if the inventory action is incorrect
                    break;
                }

                switch (action.getType()) {
                    case TAKE:
                        continueActions = InventoryActionTakeHandler.INSTANCE.isValid(this.player, (InventoryActionTake)action) &&
                                InventoryActionTakeHandler.INSTANCE.handle(response, this.player, (InventoryActionTake)action);
                        break;
                    case PLACE:
                        break;
                    case SWAP:
                        break;
                    case DROP:
                        break;
                    case DESTROY:
                        break;
                    case CONSUME:
                        break;
                    case LAB_TABLE_COMBINE:
                        break;
                    case BEACON_PAYMENT:
                        break;
                    case MINE_BLOCK:
                        break;
                    case CRAFT_RECIPE:
                        break;
                    case AUTO_CRAFT_RECIPE:
                        break;
                    case CRAFT_CREATIVE:
                        break;
                    case CRAFT_RECIPE_OPTIONAL:
                        break;
                    case CRAFT_NOT_IMPLEMENTED:
                        break;
                    case CRAFT_RESULTS_DEPRECATED:
                        break;
                    default:
                        this.player.getServer().getLogger().warn("Unhandled inventory item stack request type: " + action.getType());
                        break;
                }
                responses.add(response);
            }
        }

        itemStackResponsePacket.setResponses(responses);
        this.player.sendPacket(itemStackResponsePacket);
    }

}
