package com.sunnysuperman.fastsearch.util;

import java.util.LinkedList;
import java.util.List;

import com.sunnysuperman.commons.util.StringUtil;

public class ChineseUtil {

    public static boolean isChineseChar(char c) {
        Character.UnicodeScript sc = Character.UnicodeScript.of(c);
        if (sc == Character.UnicodeScript.HAN) {
            return true;
        }
        return false;
    }

    public static List<String> tokenizeChinese(String s) {
        if (StringUtil.isEmpty(s)) {
            return null;
        }
        List<String> list = new LinkedList<String>();
        int i = 0;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            StringBuilder buf = new StringBuilder();
            buf.append(c);
            i++;
            if (isChineseChar(c)) {
                for (; i < len; i++) {
                    c = s.charAt(i);
                    if (isChineseChar(c)) {
                        buf.append(c);
                    } else {
                        break;
                    }
                }
            } else {
                for (; i < len; i++) {
                    c = s.charAt(i);
                    if (!isChineseChar(c)) {
                        buf.append(c);
                    } else {
                        break;
                    }
                }
            }
            list.add(buf.toString());
        }
        return list;
    }
}
