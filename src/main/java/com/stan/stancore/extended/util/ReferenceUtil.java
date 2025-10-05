package com.stan.stancore.extended.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;

public class  ReferenceUtil {
    public static String generateInternalReference(){
        return System.currentTimeMillis() + StringUtils.EMPTY + RandomStringUtils.randomNumeric(3);
    }
}
