package com.sunnysuperman.fastsearch.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AttributeKey<T> {
    @SuppressWarnings("rawtypes")
    private static final ConcurrentMap<String, AttributeKey> names = new ConcurrentHashMap<String, AttributeKey>();
    private String name;

    public AttributeKey(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> valueOf(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        AttributeKey<T> option = names.get(name);
        if (option == null) {
            option = new AttributeKey<T>(name);
            AttributeKey<T> old = names.putIfAbsent(name, option);
            if (old != null) {
                option = old;
            }
        }
        return option;
    }
}
