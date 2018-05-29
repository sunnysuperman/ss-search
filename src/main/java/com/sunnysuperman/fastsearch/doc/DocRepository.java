package com.sunnysuperman.fastsearch.doc;

public interface DocRepository {

    DocRoster findDocs(Object lastId, int limit);

}
