package com.sunnysuperman.fastsearch.doc;

import com.sunnysuperman.commons.util.FormatUtil;

public abstract class StringIdDocRepository implements AbstractDocRepository<String> {

    @Override
    public String castDocId(Object id) {
        return FormatUtil.parseString(id);
    }

}
