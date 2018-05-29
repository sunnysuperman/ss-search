package com.sunnysuperman.fastsearch.core;

import com.sunnysuperman.commons.util.StringUtil;

public class SearchField {
    private String name;
    private int type;
    private boolean store;
    private SearchIndexOptions index;
    private boolean generalSearch;
    private byte segType;

    public static byte parseSegType(String s, byte defaultType) {
        if (StringUtil.isEmpty(s)) {
            return defaultType;
        }
        if (s.equals("complex")) {
            return SegType.COMPLEX;
        }
        if (s.equals("maxword")) {
            return SegType.MAXWORD;
        }
        if (s.equals("complex_maxword")) {
            return SegType.COMPLEX_MAXWORD;
        }
        throw new RuntimeException("Unknown seg type: " + s);
    }

    public SearchField() {
        super();
    }

    public SearchField(int type) {
        super();
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isStore() {
        return store;
    }

    public void setStore(boolean store) {
        this.store = store;
    }

    public SearchIndexOptions getIndex() {
        return index;
    }

    public void setIndex(SearchIndexOptions index) {
        this.index = index;
    }

    public boolean isGeneralSearch() {
        return generalSearch;
    }

    public void setGeneralSearch(boolean generalSearch) {
        this.generalSearch = generalSearch;
    }

    public byte getSegType() {
        return segType;
    }

    public void setSegType(byte segType) {
        this.segType = segType;
    }

}
