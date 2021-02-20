package com.tyty.tfs.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CommonUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void printHeader(String headers) {
        String[] arr = headers.split("\n");
        System.out.println("Map<String, String> headers = Maps.newHashMap();");
        for (String header : arr) {
            String[] pair = header.split(": ", 2);
            System.out.println("headers.put(\"" + pair[0].trim() + "\", \"" + pair[1].trim().replaceAll("\"", "\\\\\"") + "\");");
        }
    }
}
