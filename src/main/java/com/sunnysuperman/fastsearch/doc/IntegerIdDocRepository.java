package com.sunnysuperman.fastsearch.doc;

import com.sunnysuperman.commons.util.FormatUtil;

public abstract class IntegerIdDocRepository implements AbstractDocRepository<Integer> {

    @Override
    public Integer castDocId(Object id) {
        return FormatUtil.parseInteger(id);
    }

}
