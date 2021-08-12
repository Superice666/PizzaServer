package io.github.willqi.pizzaserver.server.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigGroup {

    protected Map<String, Object> properties = new LinkedHashMap<>();


    public ConfigGroup() {}

    public ConfigGroup(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setString(String name, String value) {
        this.properties.put(name, value);
    }

    public String getString(String name) {
        return String.valueOf(this.properties.get(name));
    }

    public void setStringList(String name, List<String> list) {
        this.properties.put(name, list);
    }

    public List<String> getStringList(String name) {
        return ((List<String>)this.properties.get(name))
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public void setBoolean(String name, boolean value) {
        this.properties.put(name, value);
    }

    public boolean getBoolean(String name) {
        return Boolean.parseBoolean(this.properties.get(name).toString());
    }

    public void setBooleanList(String name, List<Boolean> list) {
        this.properties.put(name, list);
    }

    public List<Boolean> getBooleanList(String name) {
        return ((List<Boolean>)this.properties.get(name))
                .stream()
                .map(obj -> Boolean.valueOf(obj.toString()))
                .collect(Collectors.toList());
    }

    public void setInteger(String name, int value) {
        this.properties.put(name, value);
    }

    public int getInteger(String name) {
        return Integer.parseInt(this.properties.get(name).toString());
    }

    public void setIntegerList(String name, List<Integer> list) {
        this.properties.put(name, list);
    }

    public List<Integer> getIntegerList(String name) {
        return ((List<Integer>)this.properties.get(name))
                .stream()
                .mapToInt(obj -> Integer.parseInt(obj.toString()))
                .boxed()
                .collect(Collectors.toList());
    }

    public void setDouble(String name, double value) {
        this.properties.put(name, value);
    }

    public double getDouble(String name) {
        return Double.parseDouble(this.properties.get(name).toString());
    }

    public void setDoubleList(String name, List<Double> list) {
        this.properties.put(name, list);
    }

    public List<Double> getDoubleList(String name) {
        return ((List<Double>)this.properties.get(name))
                .stream()
                .mapToDouble(obj -> Double.parseDouble(obj.toString()))
                .boxed()
                .collect(Collectors.toList());
    }

    public void setLong(String name, long value) {
        this.properties.put(name, value);
    }
    public long getLong(String name) {
        return Long.parseLong(this.properties.get(name).toString());
    }

    public void setLongList(String name, List<Long> list) {
        this.properties.put(name, list);
    }

    public List<Long> getLongList(String name) {
        return ((List<Long>)this.properties.get(name))
                .stream()
                .mapToLong(obj -> Long.parseLong(obj.toString()))
                .boxed()
                .collect(Collectors.toList());
    }

    public void setGroup(String name, ConfigGroup group) {
        this.properties.put(name, group);
    }

    public ConfigGroup getGroup(String name) {
        return new ConfigGroup((Map<String, Object>)this.properties.get(name));
    }

    public void remove(String name) {
        this.properties.remove(name);
    }

    public boolean has(String name) {
        return this.properties.containsKey(name);
    }

    protected Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(this.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConfigGroup) {
            return ((ConfigGroup)obj).getProperties().equals(this.getProperties());
        } else {
            return false;
        }
    }
}
