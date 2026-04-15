package com.fund.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.fund.util.HttpUtil;
import com.fund.vo.FundEstimateVO;
import com.fund.vo.FundHoldingVO;
import com.fund.vo.FundTrendVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Third-party API Adapter Service
 * 第三方接口适配服务
 */
@Service
public class FundApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(FundApiService.class);
    
    // 天天基金接口地址
    private static final String TIAN_TIAN_FUND_URL = "https://fundgz.1234567.com.cn/js/%s.js?rt=%d";
    // 腾讯财经接口地址
    private static final String TENCENT_FUND_URL = "https://qt.gtimg.cn/q=jj%s";
    // 东方财富持仓接口地址
    private static final String EAST_MONEY_HOLDINGS_URL = "https://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code=%s&topline=10&_=%d";
    // 东方财富走势接口地址
    private static final String EAST_MONEY_TREND_URL = "https://fund.eastmoney.com/pingzhongdata/%s.js?v=%d";
    
    @Resource
    private HttpUtil httpUtil;
    
    /**
     * Get fund estimate data from 天天基金
     * 天天基金接口数据处理
     */
    public FundEstimateVO getFundFromTianTian(String fundCode) {
        try {
            String url = String.format(TIAN_TIAN_FUND_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url);
            
            if (response == null || response.isEmpty()) {
                logger.warn("天天基金接口返回为空: fundCode={}", fundCode);
                return null;
            }
            
            // 解析JSONP格式: jsonpgz({...})
            String jsonStr = parseJsonP(response);
            if (jsonStr == null) {
                logger.warn("天天基金JSONP解析失败: fundCode={}", fundCode);
                return null;
            }
            
            // 使用FastJSON2解析
            FundEstimateVO vo = JSON.parseObject(jsonStr, FundEstimateVO.class);
            if (vo != null) {
                // 转换字段名 (fastjson2自动映射)
                vo.setFundCode(vo.getFundCode());
                // 转换涨跌幅为Double
                if (vo.getGszzl() == null && jsonStr.contains("gszzl")) {
                    try {
                        Pattern pattern = Pattern.compile("\"gszzl\"\\s*:\\s*\"?([^\",}]+)\"?");
                        Matcher matcher = pattern.matcher(jsonStr);
                        if (matcher.find()) {
                            String zzl = matcher.group(1).replace("\"", "");
                            vo.setGszzl(Double.parseDouble(zzl));
                        }
                    } catch (Exception e) {
                        logger.warn("解析涨跌幅失败: {}", e.getMessage());
                    }
                }
                logger.info("天天基金数据获取成功: fundCode={}, fundName={}", fundCode, vo.getFundName());
                return vo;
            }
        } catch (Exception e) {
            logger.error("天天基金接口调用失败: fundCode={}, error={}", fundCode, e.getMessage());
        }
        return null;
    }
    
    /**
     * Get fund data from 腾讯财经
     * 腾讯财经接口数据处理
     * 返回格式: v_jj000001="1~华夏成长混合~0.20~...~1.0000~0.20~2025-04-15~..."
     */
    public FundEstimateVO getFundFromTencent(String fundCode) {
        try {
            String url = String.format(TENCENT_FUND_URL, fundCode);
            String response = httpUtil.get(url);
            
            if (response == null || response.isEmpty()) {
                logger.warn("腾讯财经接口返回为空: fundCode={}", fundCode);
                return null;
            }
            
            // 提取v_jj{fundCode}="..."的内容
            String key = "v_jj" + fundCode + "=\"";
            int startIndex = response.indexOf(key);
            if (startIndex == -1) {
                // 尝试另一种格式
                key = "v_jj" + fundCode + "=\"";
                startIndex = response.indexOf(key);
            }
            
            if (startIndex == -1) {
                logger.warn("腾讯财经数据格式异常: fundCode={}", fundCode);
                return null;
            }
            
            startIndex += key.length();
            int endIndex = response.indexOf("\"", startIndex);
            if (endIndex == -1) {
                endIndex = response.length();
            }
            
            String dataStr = response.substring(startIndex, endIndex);
            String[] fields = dataStr.split("~");
            
            if (fields.length < 9) {
                logger.warn("腾讯财经数据字段不足: fundCode={}, fields={}", fundCode, fields.length);
                return null;
            }
            
            FundEstimateVO vo = new FundEstimateVO();
            vo.setFundCode(fundCode);
            vo.setFundName(fields[1]); // 基金名称
            
            // 索引5: 单位净值
            String dwjz = fields[5];
            vo.setDwjz(dwjz.isEmpty() ? null : dwjz);
            
            // 索引7: 涨跌幅
            String zzl = fields[7];
            try {
                vo.setGszzl(zzl.isEmpty() ? null : Double.parseDouble(zzl));
            } catch (NumberFormatException e) {
                vo.setGszzl(null);
            }
            
            // 索引8: 净值日期 (截取前10位)
            String jzrq = fields[8];
            if (jzrq != null && jzrq.length() >= 10) {
                vo.setJzrq(jzrq.substring(0, 10));
            } else {
                vo.setJzrq(jzrq);
            }
            
            logger.info("腾讯财经数据获取成功: fundCode={}, fundName={}", fundCode, vo.getFundName());
            return vo;
        } catch (Exception e) {
            logger.error("腾讯财经接口调用失败: fundCode={}, error={}", fundCode, e.getMessage());
        }
        return null;
    }
    
    /**
     * Get fund holdings from 东方财富
     * 东方财富持仓接口数据处理
     */
    public List<FundHoldingVO> getFundHoldings(String fundCode) {
        List<FundHoldingVO> holdings = new ArrayList<>();
        
        try {
            String url = String.format(EAST_MONEY_HOLDINGS_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url);
            
            if (response == null || response.isEmpty()) {
                logger.warn("东方财富持仓接口返回为空: fundCode={}", fundCode);
                return holdings;
            }
            
            // 解析HTML中的表格数据
            // 东方财富返回的是HTML片段，包含多个季度数据，取最新季度
            Pattern tablePattern = Pattern.compile("<tbody[^>]*>(.*?)</tbody>", Pattern.DOTALL);
            Matcher tableMatcher = tablePattern.matcher(response);
            
            if (tableMatcher.find()) {
                String tbodyContent = tableMatcher.group(1);
                
                // 匹配tr标签
                Pattern trPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL);
                Matcher trMatcher = trPattern.matcher(tbodyContent);
                
                int count = 0;
                while (trMatcher.find() && count < 10) {
                    String trContent = trMatcher.group(1);
                    
                    // 提取td内容
                    Pattern tdPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL);
                    Matcher tdMatcher = tdPattern.matcher(trContent);
                    
                    List<String> tdContents = new ArrayList<>();
                    while (tdMatcher.find()) {
                        tdContents.add(tdMatcher.group(1));
                    }
                    
                    if (tdContents.size() >= 3) {
                        FundHoldingVO holding = new FundHoldingVO();
                        
                        // 股票代码: 提取6位数字
                        String stockCode = extractNumbers(tdContents.get(0));
                        if (stockCode.length() == 6) {
                            holding.setCode(stockCode);
                        } else {
                            holding.setCode(tdContents.get(0).trim());
                        }
                        
                        // 股票名称
                        holding.setName(tdContents.get(1).trim());
                        
                        // 占净值比例
                        holding.setWeight(extractPercentage(tdContents.get(2)));
                        
                        holdings.add(holding);
                        count++;
                    }
                }
            }
            
            logger.info("东方财富持仓数据获取成功: fundCode={}, count={}", fundCode, holdings.size());
        } catch (Exception e) {
            logger.error("东方财富持仓接口调用失败: fundCode={}, error={}", fundCode, e.getMessage());
        }
        
        return holdings;
    }
    
    /**
     * Get fund trend data from 东方财富
     * 东方财富走势接口数据处理
     */
    public List<FundTrendVO> getFundTrend(String fundCode) {
        List<FundTrendVO> trends = new ArrayList<>();
        
        try {
            String url = String.format(EAST_MONEY_TREND_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url);
            
            if (response == null || response.isEmpty()) {
                logger.warn("东方财富走势接口返回为空: fundCode={}", fundCode);
                return trends;
            }
            
            // 提取Data_netWorthTrend = [...]数据
            String varName = "Data_netWorthTrend";
            String startMarker = varName + " = ";
            int startIndex = response.indexOf(startMarker);
            
            if (startIndex == -1) {
                // 尝试其他格式
                startIndex = response.indexOf("Data_netWorthTrend=");
                if (startIndex != -1) {
                    startIndex += "Data_netWorthTrend=".length();
                } else {
                    logger.warn("东方财富走势数据格式异常: fundCode={}", fundCode);
                    return trends;
                }
            } else {
                startIndex += startMarker.length();
            }
            
            // 找到数组结束位置
            int endIndex = response.indexOf(";", startIndex);
            if (endIndex == -1) {
                endIndex = response.length();
            }
            
            String arrayStr = response.substring(startIndex, endIndex).trim();
            
            // 解析JSON数组
            JSONArray jsonArray = JSON.parseArray(arrayStr);
            if (jsonArray != null && !jsonArray.isEmpty()) {
                // 取最近90条数据
                int start = Math.max(0, jsonArray.size() - 90);
                for (int i = start; i < jsonArray.size(); i++) {
                    Object obj = jsonArray.get(i);
                    if (obj instanceof com.alibaba.fastjson2.JSONObject) {
                        com.alibaba.fastjson2.JSONObject jsonObj = (com.alibaba.fastjson2.JSONObject) obj;
                        FundTrendVO trend = new FundTrendVO();
                        trend.setX(jsonObj.getLong("x"));
                        trend.setY(jsonObj.getDouble("y"));
                        trend.setEquityReturn(jsonObj.getDouble("equityReturn"));
                        trends.add(trend);
                    }
                }
            }
            
            logger.info("东方财富走势数据获取成功: fundCode={}, count={}", fundCode, trends.size());
        } catch (Exception e) {
            logger.error("东方财富走势接口调用失败: fundCode={}, error={}", fundCode, e.getMessage());
        }
        
        return trends;
    }
    
    /**
     * Parse JSONP format
     * 解析JSONP格式，提取JSON部分
     */
    private String parseJsonP(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        // 匹配 jsonpgz({...})
        Pattern pattern = Pattern.compile("jsonpgz\\((.*)\\)");
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果不是JSONP格式，直接返回
        return response;
    }
    
    /**
     * Extract numbers from string
     */
    private String extractNumbers(String str) {
        if (str == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }
    
    /**
     * Extract percentage from string
     */
    private String extractPercentage(String str) {
        if (str == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("[\\d.]+%?");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            String result = matcher.group();
            if (!result.endsWith("%")) {
                result += "%";
            }
            return result;
        }
        return "";
    }
}