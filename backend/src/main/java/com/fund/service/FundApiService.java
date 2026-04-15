package com.fund.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.fund.util.HttpUtil;
import com.fund.vo.FundDataVO;
import com.fund.vo.FundEstimateVO;
import com.fund.vo.FundHoldingVO;
import com.fund.vo.FundTrendVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 第三方接口适配模块
 * 统一调用天天基金、腾讯财经、东方财富持仓、东方财富走势4个接口
 * 返回完整基金结构化数据
 * 
 * 执行流程：
 * 1. 先请求天天基金
 * 2. 并行请求腾讯、持仓、走势
 * 3. 按日期覆盖净值数据
 * 4. 组合返回统一对象
 * 5. 任何子接口异常不中断主流程
 */
@Service
public class FundApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(FundApiService.class);
    
    // 超时配置：5秒
    private static final int TIMEOUT_SECONDS = 5;
    
    // 天天基金接口地址
    private static final String TIAN_TIAN_FUND_URL = "https://fundgz.1234567.com.cn/js/%s.js?rt=%d";
    // 腾讯财经接口地址
    private static final String TENCENT_FUND_URL = "https://qt.gtimg.cn/q=jj%s";
    // 东方财富持仓接口地址
    private static final String EAST_MONEY_HOLDINGS_URL = "https://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code=%s&topline=10&_=%d";
    // 东方财富走势接口地址
    private static final String EAST_MONEY_TREND_URL = "https://fund.eastmoney.com/pingzhongdata/%s.js?v=%d";
    // 腾讯股票实时行情接口
    private static final String TENCENT_STOCK_URL = "https://qt.gtimg.cn/q=%s";
    
    // 线程池用于并行请求
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Resource
    private HttpUtil httpUtil;
    
    /**
     * 获取基金完整数据（统一入口）
     * 按指定流程执行：
     * 1. 先请求天天基金
     * 2. 并行请求腾讯、持仓、走势
     * 3. 按日期覆盖净值数据
     * 4. 组合返回统一对象
     * 
     * 异常处理：单个接口失败不影响整体，返回空/兜底
     * 
     * @param fundCode 基金代码
     * @return FundDataVO 统一基金数据对象
     */
    public FundDataVO getFundData(String fundCode) {
        FundDataVO result = new FundDataVO();
        result.setCode(fundCode); // 无论成功失败，都设置基金代码
        result.setHoldings(new ArrayList<>());
        result.setHistoryTrend(new ArrayList<>());
        
        try {
            // 1. 先请求天天基金（基础数据）
            FundEstimateVO tianTianData = getFundFromTianTian(fundCode);
            
            if (tianTianData != null) {
                result.setCode(tianTianData.getFundCode());
                result.setName(tianTianData.getFundName());
                result.setDwjz(tianTianData.getDwjz());
                result.setGsz(tianTianData.getGsz());
                result.setGztime(tianTianData.getGztime());
                result.setJzrq(tianTianData.getJzrq());
                result.setGszzl(tianTianData.getGszzl());
            } else {
                // 天天基金失败，尝试腾讯财经作为兜底
                logger.warn("天天基金接口失败，尝试腾讯财经: fundCode={}", fundCode);
                FundEstimateVO tencentData = getFundFromTencent(fundCode);
                if (tencentData != null) {
                    result.setCode(tencentData.getFundCode());
                    result.setName(tencentData.getFundName());
                    result.setDwjz(tencentData.getDwjz());
                    result.setJzrq(tencentData.getJzrq());
                    result.setZzl(tencentData.getGszzl());
                }
            }
            
            // 2. 并行请求腾讯、持仓、走势
            CompletableFuture<FundEstimateVO> tencentFuture = CompletableFuture.supplyAsync(
                    () -> getFundFromTencent(fundCode), executorService);
            
            CompletableFuture<List<FundHoldingVO>> holdingsFuture = CompletableFuture.supplyAsync(
                    () -> getFundHoldings(fundCode), executorService);
            
            CompletableFuture<List<FundTrendVO>> trendFuture = CompletableFuture.supplyAsync(
                    () -> getFundTrend(fundCode), executorService);
            
            // 等待所有并行请求完成
            try {
                FundEstimateVO tencentData = tencentFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                List<FundHoldingVO> holdings = holdingsFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                List<FundTrendVO> trends = trendFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                
                // 3. 按日期覆盖净值数据
                if (tencentData != null && tencentData.getJzrq() != null) {
                    // 如果腾讯日期更新，则覆盖dwjz、jzrq、zzl
                    String currentJzrq = result.getJzrq();
                    if (currentJzrq == null || tencentData.getJzrq().compareTo(currentJzrq) > 0) {
                        result.setDwjz(tencentData.getDwjz());
                        result.setJzrq(tencentData.getJzrq());
                        result.setZzl(tencentData.getGszzl());
                    }
                }
                
                // 4. 批量获取持仓股票实时行情并补充change
                if (holdings != null && !holdings.isEmpty()) {
                    enrichStockData(holdings);
                    result.setHoldings(holdings);
                }
                
                // 设置历史走势
                if (trends != null && !trends.isEmpty()) {
                    result.setHistoryTrend(trends);
                    
                    // 5. 取倒数第二条equityReturn作为yesterdayChange
                    if (trends.size() >= 2) {
                        FundTrendVO secondLast = trends.get(trends.size() - 2);
                        result.setYesterdayChange(secondLast.getEquityReturn());
                    }
                }
                
            } catch (Exception e) {
                logger.warn("并行请求部分失败: fundCode={}, error={}", fundCode, e.getMessage());
                // 继续返回已有数据，不中断
            }
            
            logger.info("基金数据获取完成: fundCode={}", fundCode);
            
        } catch (Exception e) {
            logger.error("获取基金数据异常: fundCode={}, error={}", fundCode, e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 一、天天基金（JSONP解析，基础数据）
     * 地址：https://fundgz.1234567.com.cn/js/{code}.js?rt=时间戳
     * 提取：code、name、dwjz、gsz、gszzl、gztime、jzrq
     * 处理：截取()内JSON，解析为对象，gszzl转为数字
     */
    public FundEstimateVO getFundFromTianTian(String fundCode) {
        try {
            String url = String.format(TIAN_TIAN_FUND_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);
            
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
     * 二、腾讯财经（~分割，覆盖单位净值）
     * 地址：https://qt.gtimg.cn/q=jj{code}
     * 提取：dwjz=p[5]、zzl=p[7]、jzrq=p[8]
     * 规则：如果腾讯日期更新，则覆盖dwjz、jzrq、zzl
     */
    public FundEstimateVO getFundFromTencent(String fundCode) {
        try {
            String url = String.format(TENCENT_FUND_URL, fundCode);
            String response = httpUtil.get(url, TIMEOUT_SECONDS);
            
            if (response == null || response.isEmpty()) {
                logger.warn("腾讯财经接口返回为空: fundCode={}", fundCode);
                return null;
            }
            
            // 提取v_jj{fundCode}="..."的内容
            String key = "v_jj" + fundCode + "=\"";
            int startIndex = response.indexOf(key);
            if (startIndex == -1) {
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
            
            // 索引5: 单位净值 dwjz
            String dwjz = fields[5];
            vo.setDwjz(dwjz.isEmpty() ? null : dwjz);
            
            // 索引7: 涨跌幅 zzl
            String zzl = fields[7];
            try {
                vo.setGszzl(zzl.isEmpty() ? null : Double.parseDouble(zzl));
            } catch (NumberFormatException e) {
                vo.setGszzl(null);
            }
            
            // 索引8: 净值日期 jzrq
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
     * 三、东方财富持仓（HTML解析 + 股票实时行情）
     * 地址：https://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code={code}&topline=10
     * 解析：var apidata={ content:"<div>...</div>", arryear:[], curyear: }
     * 自动识别股票代码、名称、占净值比例
     * 返回前10条
     * 关键：必须批量请求腾讯股票行情，给每条持仓补充change（涨跌幅）
     * 股票前缀规则：6开头=sh，0/3开头=sz，4/8开头=bj，5位=hk
     */
    public List<FundHoldingVO> getFundHoldings(String fundCode) {
        List<FundHoldingVO> holdings = new ArrayList<>();
        
        try {
            String url = String.format(EAST_MONEY_HOLDINGS_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);
            
            if (response == null || response.isEmpty()) {
                logger.warn("东方财富持仓接口返回为空: fundCode={}", fundCode);
                return holdings;
            }
            
            // 解析 var apidata={ content:"...", arryear:[], curyear: } 格式
            String htmlContent = extractApidataContent(response);
            
            if (htmlContent == null || htmlContent.isEmpty()) {
                logger.warn("东方财富持仓数据为空: fundCode={}", fundCode);
                return holdings;
            }
            
            // 从HTML内容中提取表格数据
            Pattern tablePattern = Pattern.compile("<table[^>]*class=\"[^\"]*tzxq[^\"]*\"[^>]*>(.*?)</table>", Pattern.DOTALL);
            Matcher tableMatcher = tablePattern.matcher(htmlContent);
            
            if (tableMatcher.find()) {
                String tableContent = tableMatcher.group(1);
                
                // 提取tbody中的tr数据
                Pattern tbodyPattern = Pattern.compile("<tbody[^>]*>(.*?)</tbody>", Pattern.DOTALL);
                Matcher tbodyMatcher = tbodyPattern.matcher(tableContent);
                
                if (tbodyMatcher.find()) {
                    String tbodyContent = tbodyMatcher.group(1);
                    
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
                        
                        // 格式：序号、股票代码、股票名称、最新价、涨跌幅、相关资讯、占净值比例、持股数、持仓市值
                        if (tdContents.size() >= 7) {
                            FundHoldingVO holding = new FundHoldingVO();
                            
                            // 股票代码：从 <a href='//quote.eastmoney.com/unify/r/1.600118'>600118</a> 提取
                            String stockCode = extractStockCode(tdContents.get(1));
                            holding.setCode(stockCode);
                            
                            // 股票名称：从 <a>...</a> 中提取纯文本
                            String stockName = extractTextFromHtml(tdContents.get(2));
                            holding.setName(stockName);
                            
                            // 占净值比例：提取百分比
                            String weight = extractPercentage(tdContents.get(6));
                            holding.setWeight(weight);
                            
                            holdings.add(holding);
                            count++;
                        }
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
     * 从 var apidata={...} 格式中提取 content 字段的HTML内容
     */
    private String extractApidataContent(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        try {
            // 匹配 var apidata={ content:"...", ... }
            Pattern pattern = Pattern.compile("var\\s+apidata\\s*=\\s*\\{[^}]*content\\s*:\\s*\"([^\"]*)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                String content = matcher.group(1);
                // 处理转义字符
                content = content.replace("\\\"", "\"")
                                .replace("\\n", "")
                                .replace("\\t", "");
                return content;
            }
        } catch (Exception e) {
            logger.warn("解析apidata失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 从股票代码的HTML中提取纯代码
     * 格式：<a href='//quote.eastmoney.com/unify/r/1.600118'>600118</a>
     */
    private String extractStockCode(String html) {
        if (html == null) {
            return "";
        }
        
        // 从 <a>...</a> 标签中提取文本内容
        Pattern pattern = Pattern.compile("<a[^>]*>(\\d{5,6})</a>");
        Matcher matcher = pattern.matcher(html);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果没有匹配到a标签，直接提取数字
        return extractNumbers(html);
    }
    
    /**
     * 从HTML中提取纯文本内容
     */
    private String extractTextFromHtml(String html) {
        if (html == null) {
            return "";
        }
        
        // 移除所有HTML标签
        String text = html.replaceAll("<[^>]+>", "");
        // 移除多余空白
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }
    
    /**
     * 批量获取股票实时行情，给每条持仓补充change（涨跌幅）
     * 股票前缀规则：
     * - 6开头 = sh (上海)
     * - 0/3开头 = sz (深圳)
     * - 4/8开头 = bj (北京)
     * - 5位 = hk (港股)
     * 
     * 腾讯股票行情返回格式: v_sh600000="1~贵州茅台~...~0.50~..."
     * 字段: 0=名称, 1=代码, ..., 3=现价, 4=涨跌, 5=涨幅
     */
    private void enrichStockData(List<FundHoldingVO> holdings) {
        if (holdings == null || holdings.isEmpty()) {
            return;
        }
        
        try {
            // 构建股票代码前缀
            List<String> stockCodes = new ArrayList<>();
            for (FundHoldingVO holding : holdings) {
                String code = holding.getCode();
                if (code != null && code.length() == 6) {
                    String prefix;
                    char first = code.charAt(0);
                    if (first == '6') {
                        prefix = "sh";
                    } else if (first == '0' || first == '3') {
                        prefix = "sz";
                    } else if (first == '4' || first == '8') {
                        prefix = "bj";
                    } else {
                        prefix = "hk";
                    }
                    stockCodes.add(prefix + code);
                } else if (code != null && code.length() == 5) {
                    // 港股5位代码
                    stockCodes.add("hk" + code);
                }
            }
            
            if (stockCodes.isEmpty()) {
                return;
            }
            
            // 批量请求腾讯股票行情
            StringBuilder urlBuilder = new StringBuilder();
            for (int i = 0; i < stockCodes.size(); i++) {
                if (i > 0) {
                    urlBuilder.append(",");
                }
                urlBuilder.append(stockCodes.get(i));
            }
            
            String url = String.format(TENCENT_STOCK_URL, urlBuilder.toString());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);
            
            if (response == null || response.isEmpty()) {
                logger.warn("股票行情接口返回为空");
                return;
            }
            
            // 解析股票行情数据
            // 格式: v_sh600000="1~贵州茅台~600118~现价~涨跌~涨幅~..."
            // 字段: 0=市场, 1=名称, 2=代码, 3=现价, 4=涨跌, 5=涨幅(百分比)
            Pattern stockPattern = Pattern.compile("v_(sh|sz|bj|hk)(\\d{5,6})=\"([^\"]+)\"");
            Matcher stockMatcher = stockPattern.matcher(response);
            
            while (stockMatcher.find()) {
                String prefix = stockMatcher.group(1);
                String code = stockMatcher.group(2);
                String data = stockMatcher.group(3);
                String[] fields = data.split("~");
                
                // 查找对应的持仓记录
                for (FundHoldingVO holding : holdings) {
                    String holdingCode = holding.getCode();
                    if (holdingCode != null && holdingCode.equals(code)) {
                        // fields[5]是涨幅（百分比形式，如0.50表示0.50%）
                        try {
                            if (fields.length > 5 && fields[5] != null && !fields[5].isEmpty()) {
                                holding.setChange(fields[5]);
                            }
                        } catch (Exception e) {
                            logger.debug("解析股票涨跌幅失败: code={}", code);
                        }
                        break;
                    }
                }
            }
            
            logger.info("股票行情批量获取完成: count={}", stockCodes.size());
            
        } catch (Exception e) {
            logger.error("批量获取股票行情失败: error={}", e.getMessage());
        }
    }
    
    /**
     * 四、东方财富走势（提取Data_netWorthTrend）
     * 地址：https://fund.eastmoney.com/pingzhongdata/{code}.js?v=时间戳
     * 截取最近90条数据
     * 取倒数第二条equityReturn作为yesterdayChange
     */
    public List<FundTrendVO> getFundTrend(String fundCode) {
        List<FundTrendVO> trends = new ArrayList<>();
        
        try {
            String url = String.format(EAST_MONEY_TREND_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);
            
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
     * 解析JSONP格式
     * 提取JSON部分
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
     * 从字符串中提取数字
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
     * 从字符串中提取百分比
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