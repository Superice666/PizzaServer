package io.github.pizzaserver.api.block.types.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.nukkitx.nbt.NbtMap;
import io.github.pizzaserver.api.item.ToolTypes;
import io.github.pizzaserver.api.item.data.ToolTypeID;
import io.github.pizzaserver.api.block.types.BaseBlockType;
import io.github.pizzaserver.api.item.data.ToolType;
import io.github.pizzaserver.api.block.types.BlockTypeID;

import java.util.*;

public class BlockTypeStone extends BaseBlockType {

    private static final BiMap<NbtMap, Integer> BLOCK_STATES = HashBiMap.create(new HashMap<NbtMap, Integer>() {
        {
            List<String> stoneTypes = Arrays.asList("stone", "granite", "granite_smooth", "diorite", "diorite_smooth", "andesite", "andesite_smooth");
            int stateIndex = 0;
            for (String stoneType : stoneTypes) {
                NbtMap state = NbtMap.builder()
                        .putString("stone_type", stoneType)
                        .build();
                this.put(state, stateIndex++);
            }
        }
    });


    @Override
    public String getBlockId() {
        return BlockTypeID.STONE;
    }

    @Override
    public String getName(int blockStateIndex) {
        return "Stone";
    }

    @Override
    public BiMap<NbtMap, Integer> getBlockStates() {
        return BLOCK_STATES;
    }

    @Override
    public float getBlastResistance(int blockStateIndex) {
        return 6;
    }

    @Override
    public float getToughness(int blockStateIndex) {
        return 1.5f;
    }

    @Override
    public Set<ToolType> getCorrectTools(int blockStateIndex) {
        return Collections.singleton(ToolTypes.getToolType(ToolTypeID.WOOD_PICKAXE));
    }

    @Override
    public Set<ToolType> getBestTools(int blockStateIndex) {
        return Collections.singleton(ToolTypes.getToolType(ToolTypeID.WOOD_PICKAXE));
    }

}
