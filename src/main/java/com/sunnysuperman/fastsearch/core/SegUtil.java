package com.sunnysuperman.fastsearch.core;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashSet;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.Word;

public class SegUtil {

    public static void seg(String s, LinkedHashSet<String> tokens, byte segType) {
        try {
            Seg[] segs = null;
            if (segType == SegType.COMPLEX) {
                segs = new Seg[] { new ComplexSeg(ProviderManager.getDictionary()) };
            } else if (segType == SegType.MAXWORD) {
                segs = new Seg[] { new MaxWordSeg(ProviderManager.getDictionary()) };
            } else if (segType == SegType.COMPLEX_MAXWORD) {
                segs = new Seg[] { new ComplexSeg(ProviderManager.getDictionary()),
                        new MaxWordSeg(ProviderManager.getDictionary()) };
            } else {
                throw new RuntimeException("Unknown seg type: " + segType);
            }
            for (Seg seg : segs) {
                MMSeg mmSeg = new MMSeg(new StringReader(s), seg);
                Word word = null;
                while ((word = mmSeg.next()) != null) {
                    String smallWord = word.getString();
                    tokens.add(smallWord);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
