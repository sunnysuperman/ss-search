package com.sunnysuperman.fastsearch.analysis;

import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.sunnysuperman.fastsearch.core.ProviderManager;

/**
 * 最多分词方式.
 * 
 * @author chenlb 2009-4-6 下午08:43:46
 */
public class MaxWordAnalyzer extends MMSegAnalyzer {

    public MaxWordAnalyzer() {
        super();
    }

    @Override
    protected Seg newSeg() {
        return new MaxWordSeg(ProviderManager.getDictionary());
    }
}
