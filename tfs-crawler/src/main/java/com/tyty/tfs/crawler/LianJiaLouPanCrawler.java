package com.tyty.tfs.crawler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.tyty.tfs.common.util.JsoupUtil;
import com.tyty.tfs.dao.entity.TmHouseLouPan;
import com.tyty.tfs.dao.mapper.TmHouseLouPanMapper;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tyty.tfs.common.SystemConstants.DEFAULT_HTTP_RETRY_TIMES;
import static com.tyty.tfs.common.SystemConstants.DEFAULT_HTTP_TIME_OUT_MILLIS;

/**
 * 链家新房
 */
@Component
public class LianJiaLouPanCrawler implements Crawler {

    public static final Logger logger = LoggerFactory.getLogger(LianJiaLouPanCrawler.class);

    public static final String LOU_PAN_URL = "https://sh.fang.lianjia.com/loupan";

    public static final Map<String, String> httpHeaders = ImmutableMap.<String, String>builder()
            .put("Accept", "application/json, text/javascript, */*; q=0.01")
            .put("Accept-Encoding", "gzip, deflate, br")
            .put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .put("Cache-Control", "no-cache")
            .put("Connection", "keep-alive")
            .put("Cookie", "lianjia_uuid=18b5c38f-ce8e-4379-8a1a-b11a03ebd09d; _smt_uid=5e4e5886.f8ab068; _ga=GA1.2.1808908711.1582192776; _jzqc=1; _jzqy=1.1612694982.1612694982.1.jzqsr=baidu|jzqct=%E9%93%BE%E5%AE%B6.-; UM_distinctid=1777c1c8c9f4a4-0f0dd48da3ddf3-1d1e4459-1aeaa0-1777c1c8ca19b7; _qzjc=1; _jzqc=1; Hm_lvt_678d9c31c57be1c528ad7f62e5123d56=1612711441; Hm_lpvt_678d9c31c57be1c528ad7f62e5123d56=1612711441; search_position=default_list; _jzqx=1.1612701497.1612882010.4.jzqsr=sh%2Elianjia%2Ecom|jzqct=/ershoufang/pg3/.jzqsr=localhost:8080|jzqct=/; Hm_lvt_9152f8221cb6243a53c83b956842be8a=1612882856,1612883222,1612883592,1612883668; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%221706209c5a2838-04958a3af12f76-396f7007-1764000-1706209c5a3ac1%22%2C%22%24device_id%22%3A%221706209c5a2838-04958a3af12f76-396f7007-1764000-1706209c5a3ac1%22%2C%22props%22%3A%7B%22%24latest_traffic_source_type%22%3A%22%E7%9B%B4%E6%8E%A5%E6%B5%81%E9%87%8F%22%2C%22%24latest_referrer%22%3A%22%22%2C%22%24latest_referrer_host%22%3A%22%22%2C%22%24latest_search_keyword%22%3A%22%E6%9C%AA%E5%8F%96%E5%88%B0%E5%80%BC_%E7%9B%B4%E6%8E%A5%E6%89%93%E5%BC%80%22%2C%22%24latest_utm_source%22%3A%22baidu%22%2C%22%24latest_utm_medium%22%3A%22pinzhuan%22%2C%22%24latest_utm_campaign%22%3A%22wyshanghai%22%2C%22%24latest_utm_content%22%3A%22biaotimiaoshu%22%2C%22%24latest_utm_term%22%3A%22biaoti%22%7D%7D; Hm_lpvt_9152f8221cb6243a53c83b956842be8a=1612966971; select_city=310000; _jzqa=1.37159305408411550.1582192775.1612966972.1613200322.20; _jzqckmp=1; _gid=GA1.2.464090081.1613200325; CNZZDATA1256144506=937501740-1612706012-https%253A%252F%252Fsh.lianjia.com%252F%7C1613205177; CNZZDATA1254525948=74430404-1612709096-https%253A%252F%252Fsh.lianjia.com%252F%7C1613205945; CNZZDATA1255633284=1654412722-1612709218-https%253A%252F%252Fsh.lianjia.com%252F%7C1613206415; CNZZDATA1255604082=851265570-1612710328-https%253A%252F%252Fsh.lianjia.com%252F%7C1613207423; _qzja=1.1275649042.1582192884997.1613200324895.1613209094040.1613209094040.1613209120100.0.0.0.57.6; _qzjto=23.2.0; _jzqa=1.37159305408411550.1582192775.1612966972.1613200322.20; srcid=eyJ0IjoiXCJ7XFxcImRhdGFcXFwiOlxcXCJhNjhjYzExODJiMGVjODVkMWUyODRkY2Y2MzZhNTVmYmI5YmVlZTI0ZTM3MmE1NmI3OGRhYmQzYTA4YTAxOWEyNTkzYzUwMjBlYzhhMTE5NDExNTU5YmE5NmFkZjQ3M2I5ZWRiZGM1MmRmMDY2ZmE5MzQ3YzI3MzYwZjdmOGVkMzUzMWY0YjNmNTkyODhiZTUxODJmZjU2YWJjMGU4YTExMzIyNDhkYjVjYmMxMmRkNjQ1NmRlYTUxMWQ3NDIwMTlkYTcxNDI5MDc3ZTRhZjY0YTYzNWE3YzE3NzIxYjc5ZWQ0YWFlYjdjMGY1ODNmMDNhOGM1NWRmMDA4YjRkOTI3Mjk0MGFiMGY4MzY0MWU0MWZhNDcwZTJmZTA5OWQwNWY3ZTgzMzg3MTk0ZDllOTc0ZjIyYzc1OWQwMTAxODY0YWQwYWFkN2Q4NTdkOGY5ZDc1MTRmNzJjNWE5ZGFjNDI3XFxcIixcXFwia2V5X2lkXFxcIjpcXFwiMVxcXCIsXFxcInNpZ25cXFwiOlxcXCI2YTA0ZjJhM1xcXCJ9XCIiLCJyIjoiaHR0cHM6Ly9zaC5mYW5nLmxpYW5qaWEuY29tL2xvdXBhbi9wZzEvIiwib3MiOiJ3ZWIiLCJ2IjoiMC4xIn0=; lianjia_ssid=0016ae92-d0bf-4ea7-8241-f3650e754690; lj_newh_session=eyJpdiI6IlQwRGwrMUxIYTlCaFBMd0htalU2ZHc9PSIsInZhbHVlIjoiWTlQcUpNZmV2UDlnbDI4RlNZWjJKRkthQVZjYWtQNUt0dHBlbExyTW9UTzNFQ1ZEVXZRRnZJU0hIZFdTbWpKcHNEN1FNSWZHMk9ZbkUwZ1Y2QTZOYWc9PSIsIm1hYyI6IjFjMzZiMmRiNDI4NDFjYmY2ZjMzMGRhYWYxZDczNjBhZjA1ZWFkZWY4OTNmYWQ4ZDZmNzMzYjk3YmFkZTE5MjgifQ%3D%3D")
            .put("Host", "sh.fang.lianjia.com")
            .put("Pragma", "no-cache")
            .put("Referer", "https://sh.fang.lianjia.com")
            .put("sec-ch-ua", "\"Chromium\";v=\"88\", \"Google Chrome\";v=\"88\", \";Not A Brand\";v=\"99\"")
            .put("sec-ch-ua-mobile", "?0")
            .put("Sec-Fetch-Dest", "empty")
            .put("Sec-Fetch-Mode", "cors")
            .put("Sec-Fetch-Site", "same-origin")
            .put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
            .put("X-Requested-With", "XMLHttpRequest")
            .build();

    @Autowired
    private TmHouseLouPanMapper tmHouseLouPanMapper;

    /**
     * 执行入口，分页解析所有楼盘并落库
     */
    @Override
    public void execute() {
        int pageCount = getPageCount();
        // 由于链家接口限制，不能并发请求
        for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
            String url = LOU_PAN_URL + "/pg" + currentPage + "/?_t=1";
            List<TmHouseLouPan> louPans = parseOnePage(url);
            louPans.forEach(louPan -> {
                try {
                    // 可能出现重复情况，忽略掉
                    tmHouseLouPanMapper.insert(louPan);
                } catch (DuplicateKeyException e) {
                    logger.warn(e.getMessage());
                }
            });
        }
    }

    /**
     * 解析出一页
     */
    private List<TmHouseLouPan> parseOnePage(String url) {
        String json = JsoupUtil.sendRequest(url, httpHeaders, DEFAULT_HTTP_TIME_OUT_MILLIS, DEFAULT_HTTP_RETRY_TIMES);
        JSONObject jsonObject = JSON.parseObject(json);
        JSONObject data = (JSONObject) jsonObject.get("data");
        JSONArray list = data.getJSONArray("list");
        return list.stream()
                .map(o -> {
                    JSONObject item = (JSONObject) o;
                    TmHouseLouPan louPan = new TmHouseLouPan();
                    louPan.setCreatedDatetime(new Date());
                    louPan.setLastModifiedDatetime(new Date());
                    item.forEach((key, value) -> {
                        try {
                            BeanUtils.setProperty(louPan, CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key), value);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    });
                    louPan.setTags(StringUtils.join(item.getJSONArray("tags"), "|"));
                    louPan.setDeveloperCompany(StringUtils.join(item.getJSONArray("developer_company"), "|"));
                    louPan.setPropertyCompany(StringUtils.join(item.getJSONArray("property_company"), "|"));
                    return louPan;
                }).collect(Collectors.toList());
    }

    /**
     * 获取总页数
     */
    private int getPageCount() {
        String url = LOU_PAN_URL + "/pg1/";
        String html = JsoupUtil.sendRequest(url, httpHeaders, DEFAULT_HTTP_TIME_OUT_MILLIS, DEFAULT_HTTP_RETRY_TIMES);
        Document doc = Jsoup.parse(html);
        Element element = doc.selectFirst(".resblock-have-find .value");
        String value = JsoupUtil.getText(element);
        if (StringUtils.isBlank(value)) {
            return 0;
        }

        int totalRecord = Integer.parseInt(value.trim());
        if (totalRecord == 0) {
            return 0;
        }

        // 每页大小固定为10
        int pageCount = totalRecord / 10;
        if (totalRecord % 10 != 0) {
            pageCount++;
        }

        return pageCount;
    }
}
