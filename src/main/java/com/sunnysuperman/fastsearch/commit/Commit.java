package com.sunnysuperman.fastsearch.commit;

public class Commit {
    // public static final byte TYPE_CREATE = 1;
    // public static final byte TYPE_UPDATE = 2;
    public static final byte TYPE_SAVE = 3;
    public static final byte TYPE_REMOVE = 4;

    private long id;
    private byte type;
    private Object payload;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

}
