package com.fund.service;

import com.fund.util.HttpUtil;
import com.fund.vo.FundSearchResult;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class FundSearchService {

    private static final Logger logger = LoggerFactory.getLogger(FundSearchService.class);

    private static final String FUND_SEARCH_URL = "https://fundsuggest.eastmoney.com/FundSearch/api/FundSearchAPI.ashx";
    private static final String PUBLIC_FUND_CATEGORY = "700";
    private static final String PUBLIC_FUND_DESC = "基金";

    @Resource
    private HttpUtil httpUtil;

    public List<FundSearchResult> searchFunds(String keyword) {
        List<FundSearchResult> result = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            logger.info("搜索关键词为空，返回空列表");
            return result;
        }

        String trimmedKeyword = keyword.trim();

        try {
            String url = buildSearchUrl(trimmedKeyword);
            logger.info("开始搜索基金, keyword={}, url={}", trimmedKeyword, url);

            String response = httpUtil.get(url);

            if (response == null || response.isEmpty()) {
                logger.warn("基金搜索请求返回为空, keyword={}", trimmedKeyword);
                return result;
            }

            result = parseAndFilterResponse(response);

            logger.info("基金搜索完成, keyword={}, resultCount={}", trimmedKeyword, result.size());

        } catch (Exception e) {
            logger.error("基金搜索异常, keyword={}, error={}", trimmedKeyword, e.getMessage());
        }

        return result;
    }

    private String buildSearchUrl(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
            long timestamp = System.currentTimeMillis();
            return FUND_SEARCH_URL + "?m=1&key=" + encodedKeyword + "&_=" + timestamp;
        } catch (Exception e) {
            logger.error("构建搜索URL失败, keyword={}, error={}", keyword, e.getMessage());
            return FUND_SEARCH_URL + "?m=1&key=" + keyword + "&_=" + System.currentTimeMillis();
        }
    }

    private List<FundSearchResult> parseAndFilterResponse(String response) {
        List<FundSearchResult> result = new ArrayList<>();

        try {
            String jsonContent = extractJsonFromJsonp(response);

            com.alibaba.fastjson2.JSONObject jsonObject = com.alibaba.fastjson2.JSONObject.parseObject(jsonContent);

            if (jsonObject == null) {
                logger.warn("解析响应JSON失败，返回空列表");
                return result;
            }

            String datasStr = jsonObject.getString("Datas");

            if (datasStr == null || datasStr.isEmpty() || "[]".equals(datasStr)) {
                logger.info("搜索结果为空");
                return result;
            }

            List<com.alibaba.fastjson2.JSONObject> datas = JSON.parseArray(datasStr, com.alibaba.fastjson2.JSONObject.class);

            for (com.alibaba.fastjson2.JSONObject item : datas) {
                try {
                    FundSearchResult fundSearchResult = convertToFundSearchResult(item);
                    if (isPublicFund(fundSearchResult)) {
                        result.add(fundSearchResult);
                    }
                } catch (Exception e) {
                    logger.warn("转换基金数据异常, item={}, error={}", item.toJSONString(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("解析基金搜索响应异常, error={}", e.getMessage());
        }

        return result;
    }

    private String extractJsonFromJsonp(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }

        int startIndex = response.indexOf("(");
        int endIndex = response.lastIndexOf(")");

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return response.substring(startIndex + 1, endIndex);
        }

        return response;
    }

    private FundSearchResult convertToFundSearchResult(com.alibaba.fastjson2.JSONObject item) {
        FundSearchResult result = new FundSearchResult();
        result.setFundCode(item.getString("CODE"));
        result.setFundName(item.getString("NAME"));
        result.setCategory(String.valueOf(item.getInteger("CATEGORY")));
        result.setCategoryDesc(item.getString("CATEGORYDESC"));
        result.setSpell(item.getString("JP"));
        result.setPinYin(item.getString("JP"));
        return result;
    }

    private boolean isPublicFund(FundSearchResult fundSearchResult) {
        if (fundSearchResult == null) {
            return false;
        }

        String category = fundSearchResult.getCategory();
        String categoryDesc = fundSearchResult.getCategoryDesc();

        boolean isPublicByCategory = PUBLIC_FUND_CATEGORY.equals(category) || PUBLIC_FUND_CATEGORY.equals(String.valueOf(category));

        boolean isPublicByDesc = PUBLIC_FUND_DESC.equals(categoryDesc);

        return isPublicByCategory || isPublicByDesc;
    }
}