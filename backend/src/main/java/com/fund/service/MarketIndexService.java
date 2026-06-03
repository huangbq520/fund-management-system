package com.fund.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fund.entity.MarketIndexConfig;
import com.fund.exception.IndexDataParseException;
import com.fund.exception.KlineDataException;
import com.fund.exception.UnsupportedIndexCodeException;
import com.fund.util.HttpUtil;
import com.fund.vo.KlineDataItem;
import com.fund.vo.MarketIndexData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarketIndexService {

    private static final Logger logger = LoggerFactory.getLogger(MarketIndexService.class);

    private static final String TENCENT_INDEX_URL = "https://qt.gtimg.cn/q=%s&_t=%d";
    /** 东方财富K线接口 */
    private static final String EASTMONEY_KLINE_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=%s&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&klt=%s&fqt=1&beg=%s&end=%s";
    private static final int TIMEOUT_SECONDS = 30;
    // opt: 定义BigDecimal精度和舍入模式
    private static final int DECIMAL_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private static final List<MarketIndexConfig> INDEX_CONFIGS = Arrays.asList(
        new MarketIndexConfig("sh000001", "v_sh000001", "上证指数"),
        new MarketIndexConfig("sh000016", "v_sh000016", "上证50"),
        new MarketIndexConfig("sz399001", "v_sz399001", "深证成指"),
        new MarketIndexConfig("sz399006", "v_sz399006", "创业板指"),
        new MarketIndexConfig("sh000300", "v_sh000300", "沪深300")
    );

    @Resource
    private HttpUtil httpUtil;

    public List<MarketIndexData> getMarketIndices() {
        List<MarketIndexData> result = new ArrayList<>();

        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < INDEX_CONFIGS.size(); i++) {
            if (i > 0) codeBuilder.append(",");
            codeBuilder.append(INDEX_CONFIGS.get(i).getCode());
        }

        String url = String.format(TENCENT_INDEX_URL, codeBuilder.toString(), System.currentTimeMillis());
        logger.info("请求腾讯指数接口: {}", url);
        
        String response = httpUtil.get(url, TIMEOUT_SECONDS);

        if (response == null || response.isEmpty()) {
            logger.warn("腾讯指数接口返回为空，返回空结果");
            return result;
        }
        
        // opt: 只打印一次原始数据日志
        logger.info("腾讯接口返回原始数据: {}", response);

        for (MarketIndexConfig config : INDEX_CONFIGS) {
            try {
                logger.info("正在解析指数: code={}, varKey={}", config.getCode(), config.getVarKey());
                MarketIndexData data = parseIndexData(response, config);
                if (data != null) {
                    result.add(data);
                    logger.info("成功解析指数: name={}, price={}", data.getName(), data.getPrice());
                }
            } catch (Exception e) {
                // fix: 打印完整的异常堆栈
                logger.error("解析指数数据失败: code={}, error={}", config.getCode(), e.getMessage(), e);
            }
        }

        logger.info("大盘指数获取完成: count={}", result.size());
        return result;
    }

    public Map<String, Object> getIndexKline(String code, String startDate, String endDate, String klt) {
        Map<String, Object> result = new LinkedHashMap<>();

        String secid = toSecid(code);
        if (secid == null) {
            throw new UnsupportedIndexCodeException("不支持的指数代码: " + code);
        }

        String url = String.format(EASTMONEY_KLINE_URL, secid, klt, startDate, endDate);
        logger.info("请求东方财富K线: {}", url);


        String response = httpUtil.get(url, TIMEOUT_SECONDS);
        if (response == null || response.isEmpty()) {
            throw new KlineDataException("获取K线数据失败，接口返回为空");
        }

        JSONObject json = JSONObject.parseObject(response);
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            throw new KlineDataException("无data字段");
        }

        String name = data.getString("name");
        JSONArray klines = data.getJSONArray("klines");
        List<KlineDataItem> items = new ArrayList<>();

        if (klines != null) {
            for (int i = 0; i < klines.size(); i++) {
                KlineDataItem item = parseKlineLine(klines.getString(i));
                if (item != null && item.isValid()) {
                    items.add(item);
                }
            }
        }

        logger.info("K线解析完成: code={}, count={}", code, items.size());
        result.put("code", code);
        result.put("name", name);
        result.put("klines", items);
        return result;
    }

    private String toSecid(String code) {
        if (code == null) return null;
        switch (code) {
            case "sh000001": return "1.000001";
            case "sh000016": return "1.000016";
            case "sh000300": return "1.000300";
            case "sz399001": return "0.399001";
            case "sz399006": return "0.399006";
            default: return null;
        }
    }

    public MarketIndexData getIndexRealtime(String code) {
        MarketIndexConfig config = null;
        for (MarketIndexConfig c : INDEX_CONFIGS) {
            if (c.getCode().equals(code)) {
                config = c;
                break;
            }
        }
        if (config == null) {
            // fix: 使用自定义异常UnsupportedIndexCodeException
            throw new UnsupportedIndexCodeException("不支持的指数代码: " + code);
        }

        String url = String.format(TENCENT_INDEX_URL, config.getVarKey(), System.currentTimeMillis());
        String response = httpUtil.get(url, TIMEOUT_SECONDS);

        if (response == null || response.isEmpty()) {
            // fix: 使用自定义异常IndexDataParseException
            throw new IndexDataParseException("获取实时行情失败");
        }

        MarketIndexData data = parseIndexData(response, config);
        if (data == null) {
            throw new IndexDataParseException("解析实时行情失败");
        }
        return data;
    }

    /** 解析东方财富K线单行: date,open,close,high,low,volume,amount,amplitude,changePct,change,turnover */
    private KlineDataItem parseKlineLine(String line) {
        KlineDataItem item = new KlineDataItem();
        if (line == null || line.isEmpty()) { item.setValid(false); return item; }
        String[] f = line.split(",");
        if (f.length < 11) { item.setValid(false); return item; }
        try {
            item.setDate(f[0]);
            item.setOpen(new BigDecimal(f[1]));
            item.setClose(new BigDecimal(f[2]));
            item.setHigh(new BigDecimal(f[3]));
            item.setLow(new BigDecimal(f[4]));
            item.setVolume(Long.parseLong(f[5]));
            item.setAmount(new BigDecimal(f[6]));
            item.setAmplitude(new BigDecimal(f[7]));
            item.setChangePercent(new BigDecimal(f[8]));
            item.setChange(new BigDecimal(f[9]));
            item.setTurnoverRate(new BigDecimal(f[10]));
        } catch (Exception e) {
            item.setValid(false);
        }
        return item;
    }

    /**
     * 从腾讯接口响应中解析指数数据
     * @param response 接口响应字符串
     * @param config 指数配置
     * @return MarketIndexData对象，解析失败返回null
     */
    private MarketIndexData parseIndexData(String response, MarketIndexConfig config) {
        String searchPattern = config.getVarKey() + "=\"([^\"]+)\"";
        logger.info("搜索模式: {}", searchPattern);
        
        Pattern pattern = Pattern.compile(searchPattern);
        Matcher matcher = pattern.matcher(response);

        if (!matcher.find()) {
            logger.warn("未找到指数数据: code={}, varKey={}", config.getCode(), config.getVarKey());
            return null;
        }
        
        // fix: 删除了冗余的matchedData变量
        String data = matcher.group(1);
        logger.info("找到匹配数据: {}", data);
        String[] fields = data.split("~");

        // fix: 增加fields数组越界判断
        if (fields.length < 33) {
            logger.warn("指数数据字段不足: code={}, fields.length={}", config.getCode(), fields.length);
            return null;
        }

        MarketIndexData indexData = new MarketIndexData();
        indexData.setCode(config.getCode());
        // fix: 安全获取fields[1]
        if (fields.length > 1 && fields[1] != null) {
            indexData.setName(fields[1]);
        }

        // fix: 增强BigDecimal转换失败的日志
        try {
            if (fields.length > 3 && fields[3] != null) {
                indexData.setPrice(new BigDecimal(fields[3]).setScale(DECIMAL_SCALE, ROUNDING_MODE));
            } else {
                indexData.setPrice(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            logger.warn("转换price字段失败: fieldValue={}, error={}", fields.length > 3 ? fields[3] : "null", e.getMessage());
            indexData.setPrice(BigDecimal.ZERO);
        }

        try {
            if (fields.length > 31 && fields[31] != null) {
                indexData.setChange(new BigDecimal(fields[31]).setScale(DECIMAL_SCALE, ROUNDING_MODE));
            } else {
                indexData.setChange(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            logger.warn("转换change字段失败: fieldValue={}, error={}", fields.length > 31 ? fields[31] : "null", e.getMessage());
            indexData.setChange(BigDecimal.ZERO);
        }

        try {
            if (fields.length > 32 && fields[32] != null) {
                indexData.setChangePercent(new BigDecimal(fields[32]).setScale(DECIMAL_SCALE, ROUNDING_MODE));
            } else {
                indexData.setChangePercent(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            logger.warn("转换changePercent字段失败: fieldValue={}, error={}", fields.length > 32 ? fields[32] : "null", e.getMessage());
            indexData.setChangePercent(BigDecimal.ZERO);
        }

        // opt: 使用StringUtils.isEmpty优化空值判断
        if (fields.length > 30 && !StringUtils.isEmpty(fields[30])) {
            indexData.setUpdateTime(fields[30]);
        } else {
            indexData.setUpdateTime("");
        }

        return indexData;
    }
}
