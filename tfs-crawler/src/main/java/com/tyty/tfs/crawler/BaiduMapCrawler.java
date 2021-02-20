package com.tyty.tfs.crawler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.tyty.tfs.common.util.JsoupUtil;
import com.tyty.tfs.dao.entity.TmHouse;
import com.tyty.tfs.dao.entity.TmHouseExample;
import com.tyty.tfs.dao.mapper.TmHouseMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.tyty.tfs.common.SystemConstants.*;

/**
 * 百度地图
 */
@Component
public class BaiduMapCrawler implements Crawler {

    private static final Logger logger = LoggerFactory.getLogger(BaiduMapCrawler.class);

    /**
     * 通勤时间缓存
     */
    private static final Map<String, Object> cache = new ConcurrentHashMap<>();

    /**
     * dummy
     */
    private static final Object NULL_OBJECT = new Object();

    /**
     * 请求头
     */
    private static final Map<String, String> httpHeaders = ImmutableMap.<String, String>builder()
            .put("Accept", "*/*")
            .put("Accept-Encoding", "gzip, deflate, br")
            .put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .put("Cache-Control", "no-cache")
            .put("Connection", "keep-alive")
            .put("Cookie", "BAIDUID=73348AC65B89D4CDA6E1B90BFDAC0770:FG=1; BIDUPSID=73348AC65B89D4CDA6E1B90BFDAC0770; PSTM=1541469118; routeiconclicked=1; __yjs_duid=1_22dcf0476161c494a7c4bd3a2aefe3c01612409394195; BDORZ=B490B5EBF6F3CD402E515D22BCDA1598; H_PS_PSSID=33425_33440_33344_31253_33337_26350_22157; H_WISE_SIDS=107318_110085_114551_127969_131424_144966_156287_156927_161567_162187_162372_162775_162898_163507_164075_164163_164326_164955_165135_165329_165935_166147_166256_166691_166830_167069_167086_167103_167111_167112_167396_167422_167537_168388_168403_168540_168572_168607_168719_168767_168789_168908_168913_169062_169165_169308_169309_169374_169515; BDSFRCVID=CKAOJexroG3HR4veXFzh2MQMY2KK0gOTDYLErvRaJRvTJgFVNR8vEG0PtEeyRLA-oxqPogKK0gOTH6KF_2uxOjjg8UtVJeC6EG0Ptf8g0M5; H_BDCLCKID_SF=tRk8oIDaJCv5j5r1MtQ_M4F_qxby26nmW269aJ5y-J7nhpn-jbjqWMKhMa5uQxrkte3ion3vQpbZ8h545l5HylDB3-6M0q5wLJbqKl0MLPbpetT-XMLVhnkL0MnMBMPj52OnaIQc3fAKftnOM46JehL3346-35543bRTLnLy5KJYMDcnK4-Xj5b-DaJP; BDSFRCVID_BFESS=CKAOJexroG3HR4veXFzh2MQMY2KK0gOTDYLErvRaJRvTJgFVNR8vEG0PtEeyRLA-oxqPogKK0gOTH6KF_2uxOjjg8UtVJeC6EG0Ptf8g0M5; H_BDCLCKID_SF_BFESS=tRk8oIDaJCv5j5r1MtQ_M4F_qxby26nmW269aJ5y-J7nhpn-jbjqWMKhMa5uQxrkte3ion3vQpbZ8h545l5HylDB3-6M0q5wLJbqKl0MLPbpetT-XMLVhnkL0MnMBMPj52OnaIQc3fAKftnOM46JehL3346-35543bRTLnLy5KJYMDcnK4-Xj5b-DaJP; delPer=0; PSINO=5; BAIDUID_BFESS=73348AC65B89D4CDA6E1B90BFDAC0770:FG=1; MCITY=-%3A; routetype=bus; BA_HECTOR=a40ka5ak80al8085pk1g21j5l0r; ZD_ENTRY=baidu; ab_sr=1.0.0_ZTBmN2VlZTNmNzYzZDIzNjgyNWE0N2YxZTI4NGY0YjhmZTBmNGE2ZDNmNDA0NDg0MWJhMmQxM2U4YTJhNjczOTYxZDAxZThkODNiOTk4MDk1MDEyOTMxNjhkZTA3YWUz")
            .put("Host", "map.baidu.com")
            .put("Pragma", "no-cache")
            .put("Referer", "https://map.baidu.com/dir/%E6%A1%83%E5%9B%AD%E6%96%B0%E5%9F%8E%E4%B8%AD%E5%9F%8E%E8%8B%91/%E9%99%86%E5%AE%B6%E5%98%B4%E4%B8%96%E7%BA%AA%E9%87%91%E8%9E%8D%E5%B9%BF%E5%9C%BA/@13528055.080339506,3630816.9500000007,13.38z?querytype=bt&c=289&sn=1$$fa835ce2c066f7b7e912e485$$13535968.7309,3623061.04827$$%E6%A1%83%E5%9B%AD%E6%96%B0%E5%9F%8E%E4%B8%AD%E5%9F%8E%E8%8B%91$$0$$$$&en=1$$e42dfdca6754f83e9de32179$$13530123.70,3638862.25$$%E9%99%86%E5%AE%B6%E5%98%B4%E4%B8%96%E7%BA%AA%E9%87%91%E8%9E%8D%E5%B9%BF%E5%9C%BA$$0$$$$&sc=289&ec=289&pn=0&rn=5&exptype=dep&exptime=2021-02-08%2013:36&version=5&da_src=shareurl")
            .put("sec-ch-ua", "\"Chromium\";v=\"88\", \"Google Chrome\";v=\"88\", \";Not A Brand\";v=\"99\"")
            .put("sec-ch-ua-mobile", "?0")
            .put("Sec-Fetch-Dest", "empty")
            .put("Sec-Fetch-Mode", "cors")
            .put("Sec-Fetch-Site", "same-origin")
            .put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
            .build();


    @Autowired
    private TmHouseMapper tmHouseMapper;

    /**
     * 执行入口
     */
    public void execute() {
        int batchNo = (int) (System.currentTimeMillis() - 1612769223769L);
        TmHouseExample example = new TmHouseExample();
        example.setLimit(30);
        example.createCriteria().andBatchNoNotEqualTo(batchNo);
        example.or().andBatchNoIsNull();

        while (true) {
            List<TmHouse> tmHouses = tmHouseMapper.selectByExample(example);
            if (CollectionUtils.isEmpty(tmHouses)) {
                break;
            }
            tmHouses.parallelStream().forEach(tmHouse -> {
                tmHouse.setBatchNo(batchNo);
                tmHouse.setLastModifiedDatetime(new Date());
                try {
                    tmHouse.setToLexMinutes(getCommuteMinutes(tmHouse.getXiaoQuName(), LEX_LOCATION, LEX_LOCATION_EN));
                    tmHouse.setToCeciMinutes(getCommuteMinutes(tmHouse.getXiaoQuName(), CECI_LOCATION, CECI_LOCATION_EN));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
                tmHouseMapper.updateByPrimaryKey(tmHouse);
            });
        }
    }

    /**
     * 获取两个位置的通勤分钟数
     */
    @SuppressWarnings("unchecked")
    public Integer getCommuteMinutes(String origin, String destination, String destinationEn) {
        if (StringUtils.isAnyBlank(origin, destination, destinationEn)) {
            return null;
        }

        // cache first
        String cacheKey = origin + destination;
        Object cacheMinutes = cache.get(cacheKey);
        if (cacheMinutes != null) {
            return cacheMinutes == NULL_OBJECT ? null : (Integer) cacheMinutes;
        }

        // get from remote
        String url = "https://map.baidu.com/?newmap=1&reqflag=pcmap&biz=1&from=webmap&da_par=direct&pcevaname=pc4.1" +
                "&qt=bse&c=289&t=0&singleType=0&poiType=0&isSingle=true&ptx=13530123.70&pty=3638862.25&wd=" + origin +
                "&name=" + destination + "" +
                "&en=" + destinationEn +
                "&exptype=dep" +
                "&exptime=2021-02-08 16:32&version=5&da_src=&da_src=pcmappg.searchBox.button&tn=B_NORMAL_MAP&nn=0" +
                "&auth=" + getAuth(origin) +
                "&ie=utf-8&l=12&b=(13520338.310879631,3599125.1998148146;13553927.762268519,3646999.820185185)" +
                "&t=" + System.currentTimeMillis();

        String resp = JsoupUtil.sendRequest(url, httpHeaders, DEFAULT_HTTP_TIME_OUT_MILLIS, DEFAULT_HTTP_RETRY_TIMES);
        JSONObject jsonObject = JSON.parseObject(resp);
        List<JSONObject> content = (List<JSONObject>) jsonObject.get("content");
        if (CollectionUtils.isEmpty(content)) {
            return null;
        }

        // parse
        Integer minSeconds = content.stream()
                .map(item -> (List<JSONObject>) item.get("exts"))
                .filter(CollectionUtils::isNotEmpty)
                .map(exts -> exts.get(0).get("time"))
                .filter(Objects::nonNull)
                .map(value -> Integer.parseInt(value.toString()))
                .min(Integer::compareTo)
                .orElse(null);

        Integer minMinutes = minSeconds != null ? (int) TimeUnit.MINUTES.convert(minSeconds, TimeUnit.SECONDS) : null;
        logger.info("从[{}]到[{}]的时间为：{}分钟", origin, destination, minMinutes);
        cache.put(cacheKey, minMinutes == null ? NULL_OBJECT : minMinutes);
        return minMinutes;
    }

    /**
     * 获取token
     */
    private String getAuth(String origin) {
        String url = "https://map.baidu.com/?newmap=1" +
                "&qt=userOp&useraction=sugg&wd=" + origin +
                "&cid=289&auth=gAVwzU%40eH5yN793PAxOWd3PcQ9XBFz11uxLxzNNzTTNtComRB199A1GgvPUDZYOYIZuVt1cv3uVtGccZcuVtPWv3GuBLt%40jUIgHUvhgMZSguxzBEHLNRTVtcEWe1GD8zv7ucvY1SGpuxVthgW1aDeuVtegvcguxLxzNLzNTLttx77IKTHFkk0H6";
        String content = JsoupUtil.sendRequest(url, httpHeaders, DEFAULT_HTTP_TIME_OUT_MILLIS, DEFAULT_HTTP_RETRY_TIMES);
        logger.info("get auth resp : {}", content);
        //window.AUTH="gAVwzU@eH5yN793PAxOWd3PcQ9XBFz11uxLxzNNHNTBtzljPyBYYx1GgvPUDZYOYIZuVt1cv3uVtGccZcuVtcvY1SGpuHt69AN3zFPy@jUIgHUvhgMZSguxzBEHLNRTVtcEWe1GD8zv7u@ZPuVteuVtegvcguxLxzNLzNTLttx77IKXHGllhIT";
        content = StringUtils.trim(content);
        if (StringUtils.startsWithIgnoreCase(content, "window.AUTH=\"")) {
            content = content.substring(13, content.length() - 3);
        }
        return content;
    }
}
