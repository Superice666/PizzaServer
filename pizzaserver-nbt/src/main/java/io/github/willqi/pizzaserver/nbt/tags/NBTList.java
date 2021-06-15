package io.github.willqi.pizzaserver.nbt.tags;

import io.github.willqi.pizzaserver.nbt.exceptions.NBTLimitException;

public class NBTList<T extends NBTTag> extends NBTTag implements NBTContainer {

    public static final int ID = 9;

    private T[] list = (T[])new NBTTag[0];
    private final int childrenTypeId;
    private int depth;


    public NBTList(int childrenTypeId) {
        this.childrenTypeId = childrenTypeId;
    }

    public NBTList(String name, int childrenTypeId) {
        super(name);
        this.childrenTypeId = childrenTypeId;
    }

    public void setContents(T[] list) {
        this.list = list;
        for (T tag : list) {
            if (tag instanceof NBTContainer) {
                ((NBTContainer)tag).setDepth(this.getDepth() + 1);
            }
        }
    }

    public T[] getContents() {
        return this.list;
    }

    @Override
    public int getId() {
        return ID;
    }

    public int getChildrenTypeId() {
        return this.childrenTypeId;
    }

    @Override
    public int getDepth() {
        return this.depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
        if (this.depth > 512) {
            throw new NBTLimitException("Reached maximum depth of 512.");
        }
        for (T childTag : this.list) {
            if (childTag instanceof NBTContainer) {
                ((NBTContainer)childTag).setDepth(this.getDepth() + 1);
            }
        }
    }
}
