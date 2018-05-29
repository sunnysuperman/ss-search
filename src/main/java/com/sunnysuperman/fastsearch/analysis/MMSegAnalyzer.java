package com.sunnysuperman.fastsearch.analysis;

import org.apache.lucene.analysis.Analyzer;

import com.chenlb.mmseg4j.Seg;

/**
 * 默认使用 max-word
 *
 * @see {@link SimpleAnalyzer}, {@link ComplexAnalyzer}, {@link MaxWordAnalyzer}
 *
 * @author chenlb
 */
public abstract class MMSegAnalyzer extends Analyzer {

    protected abstract Seg newSeg();

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new TokenStreamComponents(new MMSegTokenizer(newSeg()));
    }
}
