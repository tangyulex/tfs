package com.tyty.tfs.common.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JsoupUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsoupUtil.class);

    /**
     * 发送该请求，获取返回报文
     */
    public static String sendRequest(String url, Map<String, String> headers, int timeoutMillis, int maxRetryTimes) {
        Connection connection = Jsoup.connect(url);
        connection.headers(headers);
        int retryTimes = 0;
        do {
            Connection.Response sendMsgResp = null;
            try {
                sendMsgResp = connection
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .timeout(timeoutMillis)
                        .execute();
                return sendMsgResp.body();
            } catch (Exception e) {
                if (sendMsgResp != null) {
                    logger.warn("statusCode[{}], statusMessage[{}]", sendMsgResp.statusCode(), sendMsgResp.statusMessage());
                }
                // 超时重试
                if (StringUtils.containsIgnoreCase(e.getMessage(), "time")
                        && StringUtils.containsIgnoreCase(e.getMessage(), "out")) {
                    if (++retryTimes <= maxRetryTimes) {
                        continue;
                    }
                }
                throw new RuntimeException(e);
            }

        } while (true);
    }

    /**
     * 获取元素文本信息
     */
    public static String getText(Element element) {
        if (element != null) {
            return element.text();
        }
        return null;
    }

    /**
     * 获取元素属性
     */
    public static String getAttr(Element element, String attr) {
        if (element != null) {
            String attrVal = element.attr(attr);
            return StringUtils.isBlank(attrVal) ? null : attrVal;
        }
        return null;
    }
}
