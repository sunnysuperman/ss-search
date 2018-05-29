package com.sunnysuperman.fastsearch.doc;

import java.util.Collection;
import java.util.Map;

public interface AbstractDocRepository<T> extends DocRepository {

    Map<T, Map<String, Object>> findDocByIds(Collection<T> docIds);

    T castDocId(Object id);
}
