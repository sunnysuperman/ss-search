package com.sunnysuperman.fastsearch.test;

import java.util.LinkedHashSet;

import com.chenlb.mmseg4j.Dictionary;
import com.sunnysuperman.commons.util.StringUtil;
import com.sunnysuperman.fastsearch.core.ProviderManager;
import com.sunnysuperman.fastsearch.core.SegType;
import com.sunnysuperman.fastsearch.core.SegUtil;

import junit.framework.TestCase;

public class SegTest extends TestCase {

    static {
        ProviderManager.register(ProviderManager.KEY_DICTIONARY, Dictionary.getInstance());
    }

    public void test1() {
        String s = "大宝3456789北京大学";
        {
            LinkedHashSet<String> tokens = new LinkedHashSet<String>();
            SegUtil.seg(s, tokens, SegType.COMPLEX);
            System.out.println(StringUtil.join(tokens));
        }
        {
            LinkedHashSet<String> tokens = new LinkedHashSet<String>();
            SegUtil.seg(s, tokens, SegType.MAXWORD);
            System.out.println(StringUtil.join(tokens));
        }
        {
            LinkedHashSet<String> tokens = new LinkedHashSet<String>();
            SegUtil.seg(s, tokens, SegType.COMPLEX_MAXWORD);
            System.out.println(StringUtil.join(tokens));
        }
    }

}
