package com.fund.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TradingDayChecker {

    private static final Logger logger = LoggerFactory.getLogger(TradingDayChecker.class);
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String SHANGHAI_INDEX_URL = "https://qt.gtimg.cn/q=sh000001&_t=%d";
    private static final Pattern DATA_PATTERN = Pattern.compile("v_sh000001=\"([^\"]+)\"");

    private static final LocalTime MORNING_START = LocalTime.of(9, 30);
    private static final LocalTime MORNING_END = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(15, 0);

    @Resource
    private HttpUtil httpUtil;

    public boolean isTradingDay() {
        LocalDate today = LocalDate.now(BEIJING_ZONE);
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            logger.debug("Today is weekend, not a trading day");
            return false;
        }

        String indexDateStr = fetchShanghaiIndexDate();
        if (indexDateStr == null) {
            logger.warn("Failed to fetch Shanghai index date, using weekend check");
            return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        }

        LocalDate indexDate;
        try {
            indexDate = LocalDate.parse(indexDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (DateTimeParseException e) {
            logger.error("Failed to parse index date: {}", indexDateStr, e);
            return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
        }

        if (indexDate.equals(today)) {
            return true;
        }

        LocalTime now = LocalTime.now(BEIJING_ZONE);
        if (now.isBefore(MORNING_START)) {
            logger.debug("Before market open (9:30), treating as trading day");
            return true;
        }

        logger.debug("Index date {} does not match today {}, and time is after 9:30", indexDate, today);
        return false;
    }

    public boolean isMarketOpen() {
        LocalTime now = LocalTime.now(BEIJING_ZONE);

        boolean inMorningSession = !now.isBefore(MORNING_START) && !now.isAfter(MORNING_END);
        boolean inAfternoonSession = !now.isBefore(AFTERNOON_START) && !now.isAfter(AFTERNOON_END);

        return inMorningSession || inAfternoonSession;
    }

    private String fetchShanghaiIndexDate() {
        long timestamp = System.currentTimeMillis();
        String url = String.format(SHANGHAI_INDEX_URL, timestamp);

        logger.debug("Fetching Shanghai index data from: {}", url);

        String response = httpUtil.get(url, 10);
        if (response == null) {
            logger.error("Failed to fetch Shanghai index data from: {}", url);
            return null;
        }

        Matcher matcher = DATA_PATTERN.matcher(response);
        if (!matcher.find()) {
            logger.error("Failed to parse Shanghai index data, response: {}", response);
            return null;
        }

        String data = matcher.group(1);
        String[] fields = data.split("~");

        if (fields.length < 31) {
            logger.error("Invalid Shanghai index data, field count: {}, expected at least 31", fields.length);
            return null;
        }

        String tradeTimeField = fields[30];
        if (tradeTimeField.length() < 8) {
            logger.error("Invalid trade time field: {}", tradeTimeField);
            return null;
        }

        String dateStr = tradeTimeField.substring(0, 8);
        logger.debug("Extracted trading date: {}", dateStr);
        return dateStr;
    }
}
