package com.sunnysuperman.fastsearch.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;

public class ProviderManager {
    public static final AttributeKey<SimilarWordManager> KEY_SIMILAR_WORD_MANAGER = AttributeKey.valueOf("similarword");
    public static final AttributeKey<Dictionary> KEY_DICTIONARY = AttributeKey.valueOf("dictionary");

    private static final ConcurrentMap<AttributeKey<?>, Object> providers = new ConcurrentHashMap<AttributeKey<?>, Object>();
    private static Dictionary dictionary;
    private static ComplexSeg complexSeg = null;

    public static <T> void register(AttributeKey<T> key, T value) {
        providers.put(key, value);
        if (key.equals(KEY_DICTIONARY)) {
            dictionary = (Dictionary) value;
            complexSeg = new ComplexSeg(dictionary);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(AttributeKey<T> key) {
        return (T) providers.get(key);
    }

    public static Dictionary getDictionary() {
        return dictionary;
    }

    public static ComplexSeg getComplexSeg() {
        return complexSeg;
    }
}
