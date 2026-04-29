package com.fund.service;

import com.fund.entity.MarketIndexConfig;
import com.fund.util.HttpUtil;
import com.fund.vo.MarketIndexData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarketIndexService {

    private static final Logger logger = LoggerFactory.getLogger(MarketIndexService.class);

    private static final String TENCENT_INDEX_URL = "https://qt.gtimg.cn/q=%s&_t=%d";
    private static final int TIMEOUT_SECONDS = 10;

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
        String response = httpUtil.get(url, TIMEOUT_SECONDS);

        if (response == null || response.isEmpty()) {
            logger.warn("腾讯指数接口返回为空");
            return result;
        }

        for (MarketIndexConfig config : INDEX_CONFIGS) {
            try {
                MarketIndexData data = parseIndexData(response, config);
                if (data != null) {
                    result.add(data);
                }
            } catch (Exception e) {
                logger.error("解析指数数据失败: code={}, error={}", config.getCode(), e.getMessage());
            }
        }

        logger.info("大盘指数获取完成: count={}", result.size());
        return result;
    }

    private MarketIndexData parseIndexData(String response, MarketIndexConfig config) {
        Pattern pattern = Pattern.compile(config.getVarKey() + "=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response);

        if (!matcher.find()) {
            logger.warn("未找到指数数据: code={}, varKey={}", config.getCode(), config.getVarKey());
            return null;
        }

        String data = matcher.group(1);
        String[] fields = data.split("~");

        if (fields.length < 33) {
            logger.warn("指数数据字段不足: code={}, fields.length={}", config.getCode(), fields.length);
            return null;
        }

        MarketIndexData indexData = new MarketIndexData();
        indexData.setCode(config.getCode());
        indexData.setName(fields[1]);

        try {
            indexData.setPrice(new BigDecimal(fields[3]));
        } catch (Exception e) {
            indexData.setPrice(BigDecimal.ZERO);
        }

        try {
            indexData.setChange(new BigDecimal(fields[31]));
        } catch (Exception e) {
            indexData.setChange(BigDecimal.ZERO);
        }

        try {
            indexData.setChangePercent(new BigDecimal(fields[32]));
        } catch (Exception e) {
            indexData.setChangePercent(BigDecimal.ZERO);
        }

        if (fields[30] != null && !fields[30].isEmpty()) {
            indexData.setUpdateTime(fields[30]);
        } else {
            indexData.setUpdateTime("");
        }

        return indexData;
    }
}