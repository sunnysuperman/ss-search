package com.sunnysuperman.fastsearch.doc;

import java.util.List;
import java.util.Map;

public class DocRoster {
    public List<Map<String, Object>> items;
    public Object lastId;

    public DocRoster() {
        super();
    }

    public DocRoster(List<Map<String, Object>> items, Object lastId) {
        super();
        this.items = items;
        this.lastId = lastId;
    }

    public List<Map<String, Object>> getItems() {
        return items;
    }

    public void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    public Object getLastId() {
        return lastId;
    }

    public void setLastId(Object lastId) {
        this.lastId = lastId;
    }

}
