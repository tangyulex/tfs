package com.tyty.tfs.crawler;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tyty.tfs.common.util.JsoupUtil;
import com.tyty.tfs.dao.entity.TmStock;
import com.tyty.tfs.dao.entity.TmStockHistory;
import com.tyty.tfs.dao.mapper.TmStockHistoryMapper;
import com.tyty.tfs.dao.mapper.TmStockMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tyty.tfs.common.SystemConstants.DEFAULT_HTTP_RETRY_TIMES;
import static com.tyty.tfs.common.SystemConstants.DEFAULT_HTTP_TIME_OUT_MILLIS;

/**
 * 东方财富网
 */
@Component
public class EastMoneyStockCrawler implements Crawler {

    public static final Logger logger = LoggerFactory.getLogger(EastMoneyStockCrawler.class);

    @Autowired
    private TmStockMapper tmStockMapper;

    @Autowired
    private TmStockHistoryMapper tmStockHistoryMapper;

    @Override
    public void execute() {
        List<TmStock> stockList = getStockList();
        stockList.parallelStream().forEach(tmStock -> tmStockMapper.insert(tmStock));
        stockList.parallelStream().forEach(tmStock -> {
            List<TmStockHistory> stockHistory = getStockHistory(tmStock.getStockCode(), tmStock.getF14());
            stockHistory.parallelStream().forEach(his -> tmStockHistoryMapper.insert(his));
        });
    }

    /**
     * 获取股票历史数据
     */
    private List<TmStockHistory> getStockHistory(String stockCode, String stockName) {
        logger.info("正在获取[{}]历史信息", stockCode);

        final String cb = "jQuery112408254874490232971_1613017371133";
        final String url = "http://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                "cb=" + cb +
                "&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6" +
                "&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61" +
                "&ut=7eea3edcaed734bea9cbfc24409ed989" +
                "&klt=101" +
                "&fqt=1" +
                "&secid=0." + stockCode +
                "&beg=0" +
                "&end=20500000" +
                "&_=" + System.currentTimeMillis();

        String s = JsoupUtil.sendRequest(url, getHeaders(), DEFAULT_HTTP_TIME_OUT_MILLIS, DEFAULT_HTTP_RETRY_TIMES);
        s = extractJson(s, cb);

        StockHistoryResp stockHistoryResp = StringUtils.isNotBlank(s) ? JSON.parseObject(s, StockHistoryResp.class) : null;
        return Optional.ofNullable(stockHistoryResp)
                .map(StockHistoryResp::getData)
                .map(StockHistoryResp.DataBean::getKlines)
                .map(klines -> klines.stream()
                        .map(info -> {
                            TmStockHistory his = new TmStockHistory();
                            his.setStockCode(stockCode);
                            his.setName(stockName);
                            his.setCreatedDatetime(new Date());
                            his.setLastModifiedDatetime(new Date());
                            // 日期,开盘,收盘,最高,最低,成交量,成交额,振幅%,涨跌幅%,涨跌额,换手率%
                            String[] arr = StringUtils.split(info, ",");
                            his.setStockDate(ArrayUtils.get(arr, 0));
                            his.setKaiPan(ArrayUtils.get(arr, 1));
                            his.setShouPan(ArrayUtils.get(arr, 2));
                            his.setZuiGao(ArrayUtils.get(arr, 3));
                            his.setZuiDi(ArrayUtils.get(arr, 4));
                            his.setChengJiaoLiang(ArrayUtils.get(arr, 5));
                            his.setChengJiaoE(ArrayUtils.get(arr, 6));
                            his.setZhenFu(ArrayUtils.get(arr, 7));
                            his.setZhangDieFu(ArrayUtils.get(arr, 8));
                            his.setZhangDieE(ArrayUtils.get(arr, 9));
                            his.setHuanShouLv(ArrayUtils.get(arr, 10));
                            return his;
                        }).collect(Collectors.toList()))
                .orElse(Lists.newArrayListWithCapacity(0));
    }

    /**
     * 获取股票列表
     */
    private List<TmStock> getStockList() {
        final String cb = "jQuery112408183587842078144_1613015912592";
        final String url = "http://15.push2.eastmoney.com/api/qt/clist/get?" +
                "cb=" + cb +
                "&pn=1" +
                //"&pz=20" +
                "&pz=10000" +
                "&po=0" +
                "&np=1" +
                "&ut=bd1d9ddb04089700cf9c27f6f7426281" +
                "&fltt=2" +
                "&invt=2" +
                "&fid=f12" +
                "&fs=m:0+t:6,m:0+t:13,m:0+t:80,m:1+t:2,m:1+t:23" +
                "&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f22,f11,f62,f128,f136,f115,f152" +
                "&_=" + System.currentTimeMillis();

        String s = JsoupUtil.sendRequest(url, getHeaders(), DEFAULT_HTTP_TIME_OUT_MILLIS, DEFAULT_HTTP_RETRY_TIMES);
        s = extractJson(s, cb);

        StockListResp stockListResp = StringUtils.isNotBlank(s) ? JSON.parseObject(s, StockListResp.class) : null;
        return Optional.ofNullable(stockListResp)
                .map(StockListResp::getData)
                .map(StockListResp.DataBean::getDiff)
                .map(diff -> diff.stream()
                        .map(stock -> {
                            TmStock tmStock = new TmStock();
                            tmStock.setStockCode(stock.getF12());
                            tmStock.setCreatedDatetime(new Date());
                            tmStock.setLastModifiedDatetime(new Date());
                            BeanUtils.copyProperties(stock, tmStock);
                            return tmStock;
                        }).collect(Collectors.toList()))
                .orElse(Lists.newArrayListWithCapacity(0));
    }

    /**
     * 获取请求头
     */
    private Map<String, String> getHeaders() {
        Map<String, String> headers = Maps.newHashMap();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("Cookie", "em_hq_fls=js; em-quote-version=topspeed; qgqp_b_id=c63189d9858c879ea50c474eac3d8580; st_si=40799144541616; st_pvi=45961737154564; st_sp=2020-02-11%2014%3A30%3A29; st_inirUrl=https%3A%2F%2Fwww.baidu.com%2Fs; st_sn=8; st_psi=20210211122251247-113200301202-3084249018; st_asi=delete; HAList=a-sz-000012-%u5357%20%20%u73BB%uFF21%2Ca-sz-000001-%u5E73%u5B89%u94F6%u884C%2Ca-sh-603301-%u632F%u5FB7%u533B%u7597%2Cf-0-000001-%u4E0A%u8BC1%u6307%u6570");
        headers.put("Host", "push2his.eastmoney.com");
        headers.put("Pragma", "no-cache");
        headers.put("Referer", "http://quote.eastmoney.com/");
        headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36");
        return headers;
    }

    /**
     * 去掉response中jsonp的函数
     */
    private String extractJson(String s, String cb) {
        return s.trim().replaceFirst(cb + "\\((.*)\\);", "$1");
    }

    @NoArgsConstructor
    @Data
    static class StockListResp {

        private String rc;
        private String rt;
        private String svr;
        private String lt;
        private String full;
        private DataBean data;

        @NoArgsConstructor
        @Data
        public static class DataBean {

            private int total;
            private List<DiffBean> diff;

            @NoArgsConstructor
            @Data
            public static class DiffBean {
                private String f1;
                private String f2;
                private String f3;
                private String f4;
                private String f5;
                private String f6;
                private String f7;
                private String f8;
                private String f9;
                private String f10;
                private String f11;
                private String f12;
                private String f13;
                private String f14;
                private String f15;
                private String f16;
                private String f17;
                private String f18;
                private String f20;
                private String f21;
                private String f22;
                private String f23;
                private String f24;
                private String f25;
                private String f62;
                private String f115;
                private String f128;
                private String f140;
                private String f141;
                private String f136;
                private String f152;
            }
        }
    }

    @NoArgsConstructor
    @Data
    static class StockHistoryResp {

        private String rc;
        private String rt;
        private String svr;
        private String lt;
        private String full;
        private DataBean data;

        @NoArgsConstructor
        @Data
        public static class DataBean {
            /**
             * code : 000012
             * market : 0
             * name : 南  玻Ａ
             * decimal : 2
             * dktotal : 6941
             * preKPrice : -1.68
             * klines : ["2020-11-26,7.11,6.97,7.19,6.92,494986,346959584.00,3.80,-1.97,-0.14,2.53","2020-11-27,7.00,6.95,7.10,6.83,402619,279691968.00,3.87,-0.29,-0.02,2.06"]
             */

            private String code;
            private String market;
            private String name;
            private String decimal;
            private String dktotal;
            private String preKPrice;
            // 日期,开盘,收盘,最高,最低,成交量,成交额,振幅%,涨跌幅%,涨跌额,换手率%
            private List<String> klines;
        }
    }
}
