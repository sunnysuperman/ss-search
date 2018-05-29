package com.sunnysuperman.fastsearch.doc;

import com.sunnysuperman.commons.util.FormatUtil;

public abstract class LongIdDocRepository implements AbstractDocRepository<Long> {

    @Override
    public Long castDocId(Object id) {
        return FormatUtil.parseLong(id);
    }

}
