package com.fund.service;

import com.fund.util.HttpUtil;
import com.fund.vo.FundData;
import com.fund.vo.FundHolding;
import com.fund.vo.FundHistoryTrend;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FundDataService {

    private static final Logger logger = LoggerFactory.getLogger(FundDataService.class);

    private static final int TIMEOUT_SECONDS = 10;

    private static final String TIANTIAN_FUND_URL = "https://fundgz.1234567.com.cn/js/%s.js?rt=%d";
    private static final String EASTMONEY_HOLDINGS_URL = "https://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code=%s&topline=10&year=&month=&rt=%d";
    private static final String TENCENT_STOCK_URL = "https://qt.gtimg.cn/q=%s";
    private static final String EASTMONEY_TREND_URL = "https://fund.eastmoney.com/pingzhongdata/%s.js?v=%d";

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Resource
    private HttpUtil httpUtil;

    public FundData getFundData(String fundCode) {
        FundData fundData = new FundData();
        fundData.setFundCode(fundCode);

        try {
            fetchBasicInfo(fundCode, fundData);
            fetchHoldingsAndEnrich(fundCode, fundData);
            fetchHistoryTrend(fundCode, fundData);
        } catch (Exception e) {
            logger.error("获取基金数据异常: fundCode={}, error={}", fundCode, e.getMessage());
            fundData.getErrorMessages().add("获取基金数据异常: " + e.getMessage());
        }

        return fundData;
    }

    private void fetchBasicInfo(String fundCode, FundData fundData) {
        try {
            String url = String.format(TIANTIAN_FUND_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);

            if (response == null || response.isEmpty()) {
                fundData.setBasicInfoSuccess(false);
                fundData.getErrorMessages().add("天天基金接口返回为空");
                logger.warn("天天基金接口返回为空: fundCode={}", fundCode);
                return;
            }

            String jsonStr = parseJsonP(response);
            if (jsonStr == null) {
                fundData.setBasicInfoSuccess(false);
                fundData.getErrorMessages().add("天天基金JSONP解析失败");
                logger.warn("天天基金JSONP解析失败: fundCode={}", fundCode);
                return;
            }

            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"?([^\",}]*)\"?");
            Matcher matcher = pattern.matcher(jsonStr);

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);

                switch (key) {
                    case "fundcode":
                        fundData.setFundCode(value);
                        break;
                    case "name":
                        fundData.setFundName(value);
                        break;
                    case "dwjz":
                        fundData.setUnitNetValue(value);
                        break;
                    case "gsz":
                        fundData.setEstimatedNetValue(value);
                        break;
                    case "gztime":
                        fundData.setValuationTime(value);
                        break;
                    case "gszzl":
                        try {
                            fundData.setEstimatedChange(value.isEmpty() ? null : Double.parseDouble(value));
                        } catch (NumberFormatException e) {
                            fundData.setEstimatedChange(null);
                        }
                        break;
                }
            }

            fundData.setBasicInfoSuccess(true);
            logger.info("天天基金基本信息获取成功: fundCode={}, fundName={}", fundCode, fundData.getFundName());

            updateTradingDayStatus(fundData);

        } catch (Exception e) {
            fundData.setBasicInfoSuccess(false);
            fundData.getErrorMessages().add("获取基本信息失败: " + e.getMessage());
            logger.error("获取基金基本信息失败: fundCode={}, error={}", fundCode, e.getMessage());
        }
    }

    private void updateTradingDayStatus(FundData fundData) {
        String gztime = fundData.getValuationTime();
        String gsz = fundData.getEstimatedNetValue();

        fundData.setTradingDay(isTradingDay());

        if (gsz != null && !gsz.isEmpty() && !gsz.equals("null")) {
            fundData.setPriced(true);
        } else {
            fundData.setPriced(false);
        }

        logger.info("交易日状态更新: fundCode={}, tradingDay={}, priced={}, gztime={}",
                fundData.getFundCode(), fundData.isTradingDay(), fundData.isPriced(), gztime);
    }

    private boolean isTradingDay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
        if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
            return false;
        }
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);
        int currentTime = hour * 60 + minute;
        int tradingStart = 9 * 60 + 30;
        int tradingEnd = 15 * 60;
        return currentTime >= tradingStart && currentTime <= tradingEnd;
    }

    private void fetchHoldingsAndEnrich(String fundCode, FundData fundData) {
        try {
            String url = String.format(EASTMONEY_HOLDINGS_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);

            if (response == null || response.isEmpty()) {
                fundData.setHoldingsSuccess(false);
                fundData.getErrorMessages().add("东方财富持仓接口返回为空");
                logger.warn("东方财富持仓接口返回为空: fundCode={}", fundCode);
                return;
            }

            String htmlContent = extractApidataContent(response);
            if (htmlContent == null || htmlContent.isEmpty() ||
                htmlContent.contains("暂无") ||
                htmlContent.length() < 100) {
                fundData.setHoldingsSuccess(true);
                logger.info("东方财富持仓数据为空（无持仓或未公布）: fundCode={}", fundCode);
                return;
            }

            List<FundHolding> holdings = parseHoldingsTable(htmlContent);
            fundData.setHoldings(holdings);

            if (!holdings.isEmpty()) {
                enrichStockChange(holdings);
            }

            fundData.setHoldingsSuccess(true);
            logger.info("东方财富持仓数据获取成功: fundCode={}, count={}", fundCode, holdings.size());

        } catch (Exception e) {
            fundData.setHoldingsSuccess(false);
            fundData.getErrorMessages().add("获取持仓数据失败: " + e.getMessage());
            logger.error("获取持仓数据失败: fundCode={}, error={}", fundCode, e.getMessage());
        }
    }

    private List<FundHolding> parseHoldingsTable(String htmlContent) {
        List<FundHolding> holdings = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(htmlContent);

            Elements tables = doc.select("table");
            for (Element table : tables) {
                String classAttr = table.attr("class");
                if (classAttr.contains("tzxq") || classAttr.contains("comm")) {
                    Elements rows = table.select("tbody tr");
                    if (rows.isEmpty()) {
                        rows = table.select("tr");
                    }
                    for (Element row : rows) {
                        if (holdings.size() >= 10) break;

                        Elements tds = row.select("td");
                        if (tds.size() >= 7) {
                            FundHolding holding = new FundHolding();

                            String stockCodeHtml = tds.get(1).html();
                            String stockCode = extractStockCode(stockCodeHtml);
                            holding.setStockCode(stockCode);

                            String stockName = tds.get(2).text();
                            holding.setStockName(stockName);

                            String weight = tds.get(6).text();
                            holding.setWeight(weight);

                            holdings.add(holding);
                        }
                    }
                    break;
                }
            }

        } catch (Exception e) {
            logger.error("解析持仓表格失败: {}", e.getMessage());
        }

        return holdings;
    }

    private void enrichStockChange(List<FundHolding> holdings) {
        if (holdings == null || holdings.isEmpty()) {
            return;
        }

        try {
            List<String> stockCodes = new ArrayList<>();
            for (FundHolding holding : holdings) {
                String code = holding.getStockCode();
                if (code != null && code.length() == 6) {
                    char first = code.charAt(0);
                    String prefix;
                    if (first == '6' || first == '9') {
                        prefix = "sh";
                    } else if (first == '0' || first == '3') {
                        prefix = "sz";
                    } else if (first == '4' || first == '8') {
                        prefix = "bj";
                    } else {
                        prefix = "sz";
                    }
                    stockCodes.add(prefix + code);
                }
            }

            if (stockCodes.isEmpty()) {
                return;
            }

            StringBuilder urlBuilder = new StringBuilder();
            for (int i = 0; i < stockCodes.size(); i++) {
                if (i > 0) urlBuilder.append(",");
                urlBuilder.append(stockCodes.get(i));
            }

            String url = String.format(TENCENT_STOCK_URL, urlBuilder.toString());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);

            if (response == null || response.isEmpty()) {
                logger.warn("腾讯股票行情接口返回为空");
                return;
            }

            Pattern stockPattern = Pattern.compile("v_(sh|sz|bj|hk)(\\d{5,6})=\"([^\"]+)\"");
            Matcher stockMatcher = stockPattern.matcher(response);

            while (stockMatcher.find()) {
                String code = stockMatcher.group(2);
                String data = stockMatcher.group(3);
                String[] fields = data.split("~");

                for (FundHolding holding : holdings) {
                    if (code.equals(holding.getStockCode())) {
                        try {
                            if (fields.length > 5 && fields[5] != null && !fields[5].isEmpty()) {
                                holding.setChange(Double.parseDouble(fields[5]));
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

    private void fetchHistoryTrend(String fundCode, FundData fundData) {
        try {
            String url = String.format(EASTMONEY_TREND_URL, fundCode, System.currentTimeMillis());
            String response = httpUtil.get(url, TIMEOUT_SECONDS);

            if (response == null || response.isEmpty()) {
                fundData.setHistorySuccess(false);
                fundData.getErrorMessages().add("东方财富走势接口返回为空");
                logger.warn("东方财富走势接口返回为空: fundCode={}", fundCode);
                return;
            }

            String varName = "Data_netWorthTrend";
            String startMarker = varName + " = ";
            int startIndex = response.indexOf(startMarker);

            if (startIndex == -1) {
                startIndex = response.indexOf("Data_netWorthTrend=");
                if (startIndex != -1) {
                    startIndex += "Data_netWorthTrend=".length();
                } else {
                    fundData.setHistorySuccess(false);
                    fundData.getErrorMessages().add("东方财富走势数据格式异常");
                    logger.warn("东方财富走势数据格式异常: fundCode={}", fundCode);
                    return;
                }
            } else {
                startIndex += startMarker.length();
            }

            int endIndex = response.indexOf(";", startIndex);
            if (endIndex == -1) {
                endIndex = response.length();
            }

            String arrayStr = response.substring(startIndex, endIndex).trim();

            List<FundHistoryTrend> trends = new ArrayList<>();

            Pattern itemPattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher itemMatcher = itemPattern.matcher(arrayStr);

            List<String> items = new ArrayList<>();
            while (itemMatcher.find()) {
                items.add(itemMatcher.group(1));
            }

            int start = Math.max(0, items.size() - 90);
            for (int i = start; i < items.size(); i++) {
                String item = items.get(i);

                Long x = null;
                Double y = null;
                Double equityReturn = null;

                Pattern xPattern = Pattern.compile("\"x\"\\s*:\\s*(\\d+)");
                Matcher xMatcher = xPattern.matcher(item);
                if (xMatcher.find()) {
                    x = Long.parseLong(xMatcher.group(1));
                }

                Pattern yPattern = Pattern.compile("\"y\"\\s*:\\s*([\\d.]+)");
                Matcher yMatcher = yPattern.matcher(item);
                if (yMatcher.find()) {
                    y = Double.parseDouble(yMatcher.group(1));
                }

                Pattern erPattern = Pattern.compile("\"equityReturn\"\\s*:\\s*([\\d.-]+)");
                Matcher erMatcher = erPattern.matcher(item);
                if (erMatcher.find()) {
                    equityReturn = Double.parseDouble(erMatcher.group(1));
                }

                if (x != null && y != null) {
                    FundHistoryTrend trend = new FundHistoryTrend();
                    trend.setDate(String.valueOf(x));
                    trend.setNetValue(y);
                    trend.setDailyChange(equityReturn);
                    trends.add(trend);
                }
            }

            fundData.setHistoryTrend(trends);

            if (trends.size() >= 2) {
                FundHistoryTrend last = trends.get(trends.size() - 1);
                FundHistoryTrend secondLast = trends.get(trends.size() - 2);
                fundData.setYesterdayNetValue(String.valueOf(secondLast.getNetValue()));

                if (last.getNetValue() != null && secondLast.getNetValue() != null
                    && secondLast.getNetValue() != 0) {
                    double change = (last.getNetValue() - secondLast.getNetValue()) / secondLast.getNetValue() * 100;
                    fundData.setYesterdayChange(change);
                } else if (secondLast.getDailyChange() != null) {
                    fundData.setYesterdayChange(secondLast.getDailyChange());
                }
            }

            fundData.setHistorySuccess(true);
            logger.info("东方财富走势数据获取成功: fundCode={}, count={}", fundCode, trends.size());

        } catch (Exception e) {
            fundData.setHistorySuccess(false);
            fundData.getErrorMessages().add("获取历史走势失败: " + e.getMessage());
            logger.error("获取历史走势失败: fundCode={}, error={}", fundCode, e.getMessage());
        }
    }

    private String parseJsonP(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("jsonpgz\\((.*)\\)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return response;
    }

    private String extractApidataContent(String response) {
        if (response == null || response.isEmpty()) {
            logger.warn("extractApidataContent: response为空");
            return null;
        }
        try {
            int apidataStart = response.indexOf("apidata={ content:");
            if (apidataStart == -1) {
                logger.warn("extractApidataContent: 未找到apidata={ content:, response前200字符={}", response.substring(0, Math.min(200, response.length())));
                return null;
            }

            int contentStart = apidataStart + "apidata={ content:".length();
            if (contentStart >= response.length()) {
                logger.warn("extractApidataContent: content开始位置超出范围");
                return null;
            }

            char firstChar = response.charAt(contentStart);

            int searchFrom = contentStart + 1;
            int contentEnd = -1;

            if (firstChar == '"' || firstChar == '\'') {
                contentEnd = findUnescapedChar(response, firstChar, searchFrom);
            }

            if (contentEnd == -1) {
                logger.warn("extractApidataContent: 未找到结束引号");
                return null;
            }

            String content = response.substring(contentStart + 1, contentEnd);

            return content;

        } catch (Exception e) {
            logger.warn("解析apidata失败: {}", e.getMessage());
        }
        return null;
    }

    private int findUnescapedChar(String str, char target, int start) {
        boolean escaped = false;
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == target) {
                return i;
            }
        }
        return -1;
    }

    private String extractStockCode(String html) {
        if (html == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("<a[^>]*>(\\d{5,6})</a>");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Pattern numPattern = Pattern.compile("\\d{5,6}");
        Matcher numMatcher = numPattern.matcher(html);
        if (numMatcher.find()) {
            return numMatcher.group();
        }
        return "";
    }
}
