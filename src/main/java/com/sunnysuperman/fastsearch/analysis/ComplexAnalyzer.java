package com.sunnysuperman.fastsearch.analysis;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Seg;
import com.sunnysuperman.fastsearch.core.ProviderManager;

/**
 * mmseg 的 complex analyzer
 * 
 * @author chenlb 2009-3-16 下午10:08:16
 */
public class ComplexAnalyzer extends MMSegAnalyzer {

    public ComplexAnalyzer() {
        super();
    }

    @Override
    protected Seg newSeg() {
        return new ComplexSeg(ProviderManager.getDictionary());
    }
}
