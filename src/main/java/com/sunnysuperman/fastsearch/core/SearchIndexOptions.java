package com.sunnysuperman.fastsearch.core;

import java.util.List;

import org.apache.lucene.index.IndexOptions;

import com.sunnysuperman.commons.util.StringUtil;

public class SearchIndexOptions {
    private IndexOptions index;
    private boolean tokenized;

    public static SearchIndexOptions parse(String s) {
        if (StringUtil.isEmpty(s)) {
            return null;
        }
        SearchIndexOptions options = new SearchIndexOptions();
        List<String> types = StringUtil.split(s, "|");
        for (String type : types) {
            type = type.trim();
            if (type.isEmpty()) {
                continue;
            }
            if (type.equals("score")) {
                options.setIndex(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            } else if (type.equals("doc")) {
                options.setIndex(IndexOptions.DOCS);
            } else if (type.equals("tokenized")) {
                options.setTokenized(true);
            } else {
                throw new IllegalArgumentException("Bad index options: " + s);
            }
        }
        if (options.getIndex() == null) {
            options.setIndex(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        }
        return options;
    }

    public SearchIndexOptions() {
        super();
    }

    public SearchIndexOptions(IndexOptions index, boolean tokenized) {
        super();
        this.index = index;
        this.tokenized = tokenized;
    }

    public IndexOptions getIndex() {
        return index;
    }

    public void setIndex(IndexOptions index) {
        this.index = index;
    }

    public boolean isTokenized() {
        return tokenized;
    }

    public void setTokenized(boolean tokenized) {
        this.tokenized = tokenized;
    }

}
