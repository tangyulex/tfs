package com.tyty.tfs.crawler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.tyty.tfs.common.util.JsoupUtil;
import com.tyty.tfs.dao.entity.TmHouse;
import com.tyty.tfs.dao.mapper.TmHouseMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tyty.tfs.common.SystemConstants.*;

/**
 * 链家二手房
 */
@Component
public class LianJiaEsfCrawler implements Crawler {

    private static final Logger logger = LoggerFactory.getLogger(LianJiaEsfCrawler.class);

    /**
     * 链家二手房url
     */
    private static final String ESF_URL = "https://sh.lianjia.com/ershoufang";

    /**
     * 区域
     */
    private static final String[] regions = {"pudong", "minhang", "baoshan", "xuhui", "putuo",
            "yangpu", "changning", "songjiang", "jiading", "huangpu", "jingan",
            "hongkou", "qingpu", "fengxian", "jinshan", "chongming", "shanghaizhoubian"};

    /**
     * 总价档次
     */
    private static final String[] priceLevels = {"p1", "p2", "p3", "p4", "p5", "p6", "p7"};

    /**
     * 房屋详细信息映射到tmHouse的字段，反射用
     */
    private static final ImmutableMap<String, String> fieldMap = ImmutableMap.<String, String>builder()
            .put("房屋户型", TmHouse.__houseType)
            .put("所在楼层", TmHouse.__storey)
            .put("建筑面积", TmHouse.__coveredArea)
            .put("户型结构", TmHouse.__houseTypeStructure)
            .put("套内面积", TmHouse.__insideFloorArea)
            .put("建筑类型", TmHouse.__buildingType)
            .put("房屋朝向", TmHouse.__orientation)
            .put("建筑结构", TmHouse.__buildingStructure)
            .put("装修情况", TmHouse.__decoration)
            .put("梯户比例", TmHouse.__tihuRatio)
            .put("配备电梯", TmHouse.__hasElevator)
            .put("挂牌时间", TmHouse.__hangOutTime)
            .put("交易权属", TmHouse.__transactionOwnership)
            .put("上次交易", TmHouse.__lastTransactionTime)
            .put("房屋用途", TmHouse.__houseUsage)
            .put("房屋年限", TmHouse.__houseYear)
            .put("产权所属", TmHouse.__equityOwnership)
            .put("抵押信息", TmHouse.__pledge)
            .put("房本备件", TmHouse.__premisesPermitSpare)
            .build();

    /**
     * 请求头
     */
    private static final Map<String, String> httpHeaders = ImmutableMap.<String, String>builder()
            .put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .put("Accept-Encoding", "gzip, deflate, br")
            .put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .put("Cache-Control", "no-cache")
            .put("Connection", "keep-alive")
            .put("Cookie", "lianjia_uuid=18b5c38f-ce8e-4379-8a1a-b11a03ebd09d; _smt_uid=5e4e5886.f8ab068; _ga=GA1.2.1808908711.1582192776; select_city=310000; digv_extends=%7B%22utmTrackId%22%3A%2221583074%22%7D; lianjia_ssid=e359278b-ce7b-483b-b2fb-d419e9c17eff; _jzqa=1.37159305408411550.1582192775.1582192775.1612694982.2; _jzqc=1; _jzqy=1.1612694982.1612694982.1.jzqsr=baidu|jzqct=%E9%93%BE%E5%AE%B6.-; _jzqckmp=1; _qzjc=1; UM_distinctid=1777c1c8c9f4a4-0f0dd48da3ddf3-1d1e4459-1aeaa0-1777c1c8ca19b7; CNZZDATA1253492439=1065405395-1612693105-https%253A%252F%252Fwww.baidu.com%252F%7C1612693105; CNZZDATA1254525948=1780828630-1612692895-https%253A%252F%252Fwww.baidu.com%252F%7C1612692895; CNZZDATA1255633284=1726783559-1612693014-https%253A%252F%252Fwww.baidu.com%252F%7C1612693014; CNZZDATA1255604082=936091422-1612694128-https%253A%252F%252Fwww.baidu.com%252F%7C1612694128; sensorsdata2015jssdkcross=%7B%22distinct_id%22%3A%221706209c5a2838-04958a3af12f76-396f7007-1764000-1706209c5a3ac1%22%2C%22%24device_id%22%3A%221706209c5a2838-04958a3af12f76-396f7007-1764000-1706209c5a3ac1%22%2C%22props%22%3A%7B%22%24latest_traffic_source_type%22%3A%22%E4%BB%98%E8%B4%B9%E5%B9%BF%E5%91%8A%E6%B5%81%E9%87%8F%22%2C%22%24latest_referrer%22%3A%22https%3A%2F%2Fwww.baidu.com%2Fother.php%22%2C%22%24latest_referrer_host%22%3A%22www.baidu.com%22%2C%22%24latest_search_keyword%22%3A%22%E9%93%BE%E5%AE%B6%22%2C%22%24latest_utm_source%22%3A%22baidu%22%2C%22%24latest_utm_medium%22%3A%22pinzhuan%22%2C%22%24latest_utm_campaign%22%3A%22wyshanghai%22%2C%22%24latest_utm_content%22%3A%22biaotimiaoshu%22%2C%22%24latest_utm_term%22%3A%22biaoti%22%7D%7D; _gid=GA1.2.1013089856.1612694984; Hm_lvt_9152f8221cb6243a53c83b956842be8a=1612694989; Hm_lpvt_9152f8221cb6243a53c83b956842be8a=1612695650; _qzja=1.2050134370.1582192774808.1582192774808.1612694981781.1612695475806.1612695650344.0.0.0.11.2; _qzjb=1.1612694981781.7.0.0.0; _qzjto=7.1.0; _jzqb=1.7.10.1612694982.1; srcid=eyJ0Ijoie1wiZGF0YVwiOlwiOGJkYzE4ZWUwYTlhMTE0MzY3NmQyYTAyNTVkYjU3YWJjMTQ5NTQwOTkxYjExM2FjYzE1NTE2OTQ0MzQ3YzhjOTI2YTE0MDdiN2QxNDJkYmEyMTIzNzU0NzM2NjgwMTliZGEyYjk5NWQzOTljNGQ5N2ZlZmNjNGI2ZjQ1OWI1NmRhYTE3NzI1MmFhNDAwZjQ3ODg1ZDMwZTU2MmNmNzhiODdkNDFmYTA3MGUyMjJiN2RjZGIzZDVlYjY0YTI3ZjQyMjMxNzMxNDAxYWJhMmY0NDZmMmM4M2FiMmJhMDRjZmRmY2QzM2M1ZThiOTZmNTZhOGIwNDI1NTkyYjY0NTk5NjUxMTRkMmZlYTk4NWQ5ZjRlMmY1MzZkOGQ4MTAyODFlNDdjNjE1ODFkYmY0ODFlNDBkZjZhZGQ5ZmFiNmJmNDBiNWFhYWU2NzM0MGVjNTdlODJhZmY0OTY2NWQxNTRmNVwiLFwia2V5X2lkXCI6XCIxXCIsXCJzaWduXCI6XCI4YzhkYTJlNFwifSIsInIiOiJodHRwczovL3NoLmxpYW5qaWEuY29tL2Vyc2hvdWZhbmcvcGc0LyIsIm9zIjoid2ViIiwidiI6IjAuMSJ9; _gat=1; _gat_past=1; _gat_global=1; _gat_new_global=1; _gat_dianpu_agent=1")
            .put("Host", "sh.lianjia.com")
            .put("Pragma", "no-cache")
            .put("Referer", "http://sh.lianjia.com/")
            .put("sec-ch-ua", "\"Chromium\";v=\"88\", \"Google Chrome\";v=\"88\", \";Not A Brand\";v=\"99\"")
            .put("sec-ch-ua-mobile", "?0")
            .put("Sec-Fetch-Dest", "document")
            .put("Sec-Fetch-Mode", "navigate")
            .put("Sec-Fetch-Site", "same-origin")
            .put("Upgrade-Insecure-Requests", "1")
            .put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36")
            .build();

    /**
     * 百度地图
     */
    @Autowired
    private BaiduMapCrawler baiduMapCrawler;

    @Autowired
    private TmHouseMapper tmHouseMapper;

    private ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 4,
            Runtime.getRuntime().availableProcessors() * 4,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    /**
     * 执行入口，按照区域和总价档次逐页解析并落库
     */
    public void execute() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("爬取链家二手房信息");
        Arrays.stream(regions).parallel().forEach(region ->
                Arrays.stream(priceLevels).parallel().forEach(priceLevel -> {
                    boolean hasNextPage = true;
                    for (int i = 1; hasNextPage && i < 1000; i++) {
                        String pageUrl = ESF_URL + "/" + region + "/pg" + i + priceLevel + "/";
                        try {
                            OnePageHouseResult page = parseOnePage(pageUrl, region, priceLevel);
                            hasNextPage = page.isHasNextPage();
                            page.getTmHouses().forEach(tmHouseMapper::insert);
                            asyncEnrichInfo(page.getTmHouses());
                        } catch (Exception e) {
                            logger.error("pageUrl[{}], Exception", pageUrl, e);
                        }
                    }
                }));

        // 等待所有任务执行结束
        waitTaskComplete();
        stopWatch.stop();
        logger.info(stopWatch.prettyPrint());
    }

    /**
     * 异步并行补充房屋详细信息和通勤时间
     */
    private void asyncEnrichInfo(List<TmHouse> houseInfos) {
        threadPool.submit(() ->
                houseInfos.parallelStream().forEach(h -> {
                    enrichHouseDetail(h);
                    h.setToLexMinutes(baiduMapCrawler.getCommuteMinutes(h.getXiaoQuName(), LEX_LOCATION, LEX_LOCATION_EN));
                    h.setToCeciMinutes(baiduMapCrawler.getCommuteMinutes(h.getXiaoQuName(), CECI_LOCATION, CECI_LOCATION_EN));
                    h.setLastModifiedDatetime(new Date());
                    tmHouseMapper.updateByPrimaryKeySelective(h);
                }));
    }

    /**
     * 解析一页房屋信息
     */
    private OnePageHouseResult parseOnePage(String pageUrl, String region, String priceLevel) {
        logger.info("正在爬取的二手房URL[{}]", pageUrl);

        OnePageHouseResult res = new OnePageHouseResult(Lists.newArrayListWithCapacity(0), false);
        String html = JsoupUtil.sendRequest(pageUrl, httpHeaders,
                DEFAULT_HTTP_TIME_OUT_MILLIS,
                DEFAULT_HTTP_RETRY_TIMES);
        Document doc = Jsoup.parse(html);

        // 总记录数
        String total = doc.select(".content .total span").text();
        if (StringUtils.equals("0", StringUtils.trim(total))) {
            return res;
        }

        // 解析
        List<TmHouse> houseInfos = parseOnePage(doc, region, priceLevel);
        res.setTmHouses(houseInfos);

        // 判断是否有下一页
        Element pageBox = doc.selectFirst(".house-lst-page-box");
        String pageData = JsoupUtil.getAttr(pageBox, "page-data");
        if (StringUtils.isNotBlank(pageData)) {
            JSONObject json = JSON.parseObject(pageData);
            res.setHasNextPage((int) json.get("totalPage") > (int) json.get("curPage"));
        }
        return res;
    }

    /**
     * 解析当前页所有房屋信息
     */
    private List<TmHouse> parseOnePage(Document doc, String region, String priceLevel) {
        Elements houseInfoEles = doc.selectFirst(".sellListContent").select("li.LOGVIEWDATA");

        return houseInfoEles.parallelStream()
                .map(houseEle -> {
                    Optional<TmHouse> tmHouse = parseOne(houseEle);
                    tmHouse.ifPresent(h -> {
                        h.setRegion(region);
                        h.setPriceLevel(priceLevel);
                        h.setCreatedDatetime(new Date());
                        h.setLastModifiedDatetime(new Date());
                    });
                    return tmHouse;
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * 解析一个房屋信息
     */
    private Optional<TmHouse> parseOne(Element houseEle) {
        Element info = houseEle.select(".info.clear").first();
        if (info == null) {
            logger.warn("info is null, element:[{}]", houseEle);
            return Optional.empty();
        }

        TmHouse h = new TmHouse();
        Element a = info.select(".title a").first();
        h.setDetailUrl(JsoupUtil.getAttr(a, "href"));
        h.setTitle(JsoupUtil.getText(a));
        Element tag = info.select(".title .goodhouse_tag").first();
        h.setGoodHouseTag(JsoupUtil.getText(tag));
        Elements positionA = info.select(".flood .positionInfo a");
        Element xiaoqu = positionA.first();
        h.setXiaoQuUrl(JsoupUtil.getAttr(xiaoqu, "href"));
        h.setXiaoQuName(JsoupUtil.getText(xiaoqu));
        if (positionA.size() > 1) {
            Element smallRegion = positionA.get(1);
            h.setSmallRegion(JsoupUtil.getText(smallRegion));
        }
        Element houseInfo = info.select(".address .houseInfo").first();
        h.setHouseInfo(JsoupUtil.getText(houseInfo));
        Element followInfo = info.select(".followInfo").first();
        h.setFollowInfo(JsoupUtil.getText(followInfo));
        Element taxfree = info.select(".tag .taxfree").first();
        h.setTaxFree(JsoupUtil.getText(taxfree));
        Element subway = info.select(".tag .subway").first();
        h.setSubway(JsoupUtil.getText(subway));
        Element totalPrice = info.select(".priceInfo .totalPrice").first();
        h.setTotalPrice(JsoupUtil.getText(totalPrice));
        Element unitPrice = info.select(".priceInfo .unitPrice").first();
        h.setUnitPrice(JsoupUtil.getAttr(unitPrice, "data-price"));
        return Optional.of(h);
    }

    /**
     * 增加房屋详细信息
     */
    private void enrichHouseDetail(TmHouse h) {
        if (h == null || StringUtils.isBlank(h.getDetailUrl())) {
            logger.warn("无房屋详细信息url. {}", h);
            return;
        }

        String html = JsoupUtil.sendRequest(h.getDetailUrl(), httpHeaders,
                DEFAULT_HTTP_TIME_OUT_MILLIS,
                DEFAULT_HTTP_RETRY_TIMES);
        Document doc = Jsoup.parse(html);

        // 基本信息
        enrichBaseInfo(h, doc);
        // 交易信息
        enrichTransInfo(h, doc);
        // 图片
        enrichImage(h, doc);
    }

    /**
     * 图片信息
     */
    private void enrichImage(TmHouse h, Document doc) {
        Elements liEles = doc.select("#topImg .thumbnail .smallpic li");
        if (CollectionUtils.isNotEmpty(liEles)) {
            String image = liEles.eachAttr("data-src")
                    .stream()
                    .filter(s -> !s.endsWith("1024x"))
                    .collect(Collectors.joining("|"));
            h.setImage(image);
        }
    }

    /**
     * 详细信息-基本信息
     */
    private void enrichBaseInfo(TmHouse h, Document doc) {
        Elements baseInfoEle = doc.select(".introContent .base .content li");
        baseInfoEle.forEach(element -> {
            String field = element.selectFirst("span").text();
            if (!fieldMap.containsKey(field)) {
                logger.warn("field not found [{}]", field);
                return;
            }
            List<TextNode> textNodes = element.textNodes();
            if (CollectionUtils.isNotEmpty(textNodes)) {
                try {
                    BeanUtils.setProperty(h, fieldMap.get(field), textNodes.get(0));
                } catch (Exception e) {
                    logger.error("{}, house[{}], htmlField[{}], field[{}], value[{}]",
                            e.getMessage(), h, field, fieldMap.get(field), textNodes.get(0), e);
                }
            }
        });
    }

    /**
     * 详细信息-交易信息
     */
    private void enrichTransInfo(TmHouse h, Document doc) {
        Elements transInfoEle = doc.select(".introContent .transaction .content li");
        transInfoEle.forEach(element -> {
            Elements spans = element.select("span");
            if (spans.size() > 1) {
                String field = StringUtils.trim(spans.get(0).text());
                String value = StringUtils.trim(spans.get(1).text());
                if (!StringUtils.isAnyBlank(field, value)) {
                    if (!fieldMap.containsKey(field)) {
                        logger.warn("field not found [{}]", field);
                        return;
                    }
                    try {
                        BeanUtils.setProperty(h, fieldMap.get(field), value);
                    } catch (Exception e) {
                        logger.error("{}, house[{}], htmlField[{}], field[{}], value[{}]",
                                e.getMessage(), h, field, fieldMap.get(field), value, e);
                    }
                }
            }
        });
    }

    /**
     * 等待线程池中所有任务执行结束
     */
    private void waitTaskComplete() {
        try {
            while (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                logger.info("当前线程池状态" + threadPool.toString());
                if (threadPool.getActiveCount() == 0) {
                    threadPool.shutdown();
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    static class OnePageHouseResult {
        private List<TmHouse> tmHouses;
        private boolean hasNextPage;
    }
}
