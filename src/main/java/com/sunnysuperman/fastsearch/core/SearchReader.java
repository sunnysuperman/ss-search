package com.sunnysuperman.fastsearch.core;

import org.apache.lucene.index.DirectoryReader;

public class SearchReader {
    private DirectoryReader reader;
    private long queueAt;

    public SearchReader(DirectoryReader reader) {
        super();
        this.reader = reader;
        queueAt = System.currentTimeMillis();
    }

    public DirectoryReader getReader() {
        return reader;
    }

    public long getQueueAt() {
        return queueAt;
    }

}
