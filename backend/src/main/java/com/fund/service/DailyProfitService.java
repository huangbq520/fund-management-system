package com.fund.service;

import com.fund.entity.FundDailyProfit;
import com.fund.entity.UserFund;
import com.fund.mapper.FundDailyProfitMapper;
import com.fund.mapper.UserFundMapper;
import com.fund.vo.DailyProfitVO;
import com.fund.vo.FundData;
import com.fund.vo.FundHistoryTrend;
import com.fund.vo.OverallDailyProfitVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DailyProfitService {

    private static final Logger logger = LoggerFactory.getLogger(DailyProfitService.class);
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private UserFundMapper userFundMapper;

    @Resource
    private FundDailyProfitMapper fundDailyProfitMapper;

    @Resource
    private FundDataService fundDataService;

    public void calculateDailyProfit() {
        List<UserFund> allHoldings = userFundMapper.findAll();
        logger.info("开始每日收益统计，持仓记录数: {}", allHoldings.size());

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (UserFund uf : allHoldings) {
            try {
                if (uf.getHoldShare() == null || uf.getHoldShare().compareTo(BigDecimal.ZERO) <= 0) {
                    skipCount++;
                    continue;
                }

                FundData fundData = fundDataService.getFundData(uf.getFundCode());
                List<FundHistoryTrend> trends = fundData.getHistoryTrend();
                if (trends == null || trends.size() < 2) {
                    logger.debug("历史净值数据不足，跳过: fundCode={}", uf.getFundCode());
                    skipCount++;
                    continue;
                }

                trends.sort(Comparator.comparing(t -> {
                    try { return Long.parseLong(t.getDate()); } catch (Exception e) { return 0L; }
                }));

                FundHistoryTrend todayTrend = trends.get(trends.size() - 1);
                FundHistoryTrend yesterdayTrend = trends.get(trends.size() - 2);

                if (todayTrend.getNetValue() == null || yesterdayTrend.getNetValue() == null
                    || yesterdayTrend.getNetValue() == 0) {
                    skipCount++;
                    continue;
                }

                String recordDate = LocalDate.now(BEIJING_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE);

                FundDailyProfit existing = fundDailyProfitMapper.selectByUserFundDate(
                        uf.getUserId(), uf.getFundCode(), recordDate);
                if (existing != null) {
                    skipCount++;
                    continue;
                }

                BigDecimal todayNAV = BigDecimal.valueOf(todayTrend.getNetValue());
                BigDecimal yesterdayNAV = BigDecimal.valueOf(yesterdayTrend.getNetValue());
                BigDecimal holdShare = uf.getHoldShare();

                BigDecimal dailyReturnRate = todayNAV.subtract(yesterdayNAV)
                        .divide(yesterdayNAV, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                BigDecimal dailyProfit = holdShare.multiply(todayNAV.subtract(yesterdayNAV))
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal holdAmount = holdShare.multiply(todayNAV).setScale(2, RoundingMode.HALF_UP);

                FundDailyProfit record = new FundDailyProfit();
                record.setUserId(uf.getUserId());
                record.setFundCode(uf.getFundCode());
                record.setFundName(uf.getFundName());
                record.setRecordDate(java.sql.Date.valueOf(recordDate));
                record.setDailyProfit(dailyProfit);
                record.setDailyReturnRate(dailyReturnRate.setScale(4, RoundingMode.HALF_UP));
                record.setNetValue(todayNAV.setScale(4, RoundingMode.HALF_UP));
                record.setHoldShare(holdShare);
                record.setHoldAmount(holdAmount);

                fundDailyProfitMapper.insert(record);
                successCount++;

            } catch (Exception e) {
                logger.error("统计收益失败: fundCode={}, userId={}, error={}",
                        uf.getFundCode(), uf.getUserId(), e.getMessage());
                failCount++;
            }
        }

        logger.info("每日收益统计完成: 成功={}, 跳过={}, 失败={}", successCount, skipCount, failCount);
    }

    public DailyProfitVO getFundDailyProfit(Long userId, String fundCode, String period) {
        DailyProfitVO vo = new DailyProfitVO();
        vo.setFundCode(fundCode);

        UserFund userFund = userFundMapper.findByUserIdAndFundCode(userId, fundCode);
        if (userFund != null) {
            vo.setFundName(userFund.getFundName());
            vo.setHoldShare(userFund.getHoldShare());
            vo.setCostPrice(userFund.getCostPrice());
        }

        String startDate = computeStartDate(period);
        List<FundDailyProfit> records;
        if (startDate != null) {
            records = fundDailyProfitMapper.selectByUserAndFundSince(userId, fundCode, startDate);
        } else {
            records = fundDailyProfitMapper.selectByUserAndFund(userId, fundCode);
        }

        List<DailyProfitVO.CurvePoint> curve = new ArrayList<>();
        List<DailyProfitVO.DetailItem> details = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;

        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal maxProfit = null;
        BigDecimal maxLoss = null;
        String maxProfitDate = null;
        String maxLossDate = null;

        for (FundDailyProfit r : records) {
            cumulative = cumulative.add(r.getDailyProfit());

            DailyProfitVO.CurvePoint point = new DailyProfitVO.CurvePoint();
            point.setRecordDate(formatDate(r.getRecordDate()));
            point.setDailyProfit(r.getDailyProfit());
            point.setCumulativeProfit(cumulative.setScale(2, RoundingMode.HALF_UP));
            point.setDailyReturnRate(r.getDailyReturnRate());
            point.setNetValue(r.getNetValue());
            curve.add(point);

            DailyProfitVO.DetailItem detail = new DailyProfitVO.DetailItem();
            detail.setRecordDate(formatDate(r.getRecordDate()));
            detail.setDailyProfit(r.getDailyProfit());
            detail.setDailyReturnRate(r.getDailyReturnRate());
            detail.setNetValue(r.getNetValue());
            detail.setHoldShare(r.getHoldShare());
            detail.setHoldAmount(r.getHoldAmount());
            details.add(0, detail);

            totalProfit = totalProfit.add(r.getDailyProfit());

            if (maxProfit == null || r.getDailyProfit().compareTo(maxProfit) > 0) {
                maxProfit = r.getDailyProfit();
                maxProfitDate = formatDate(r.getRecordDate());
            }
            if (maxLoss == null || r.getDailyProfit().compareTo(maxLoss) < 0) {
                maxLoss = r.getDailyProfit();
                maxLossDate = formatDate(r.getRecordDate());
            }
        }

        DailyProfitVO.Summary summary = new DailyProfitVO.Summary();
        summary.setTotalProfit(totalProfit.setScale(2, RoundingMode.HALF_UP));
        summary.setMaxDailyProfit(maxProfit != null ? maxProfit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        summary.setMaxDailyLoss(maxLoss != null ? maxLoss.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        summary.setMaxProfitDate(maxProfitDate);
        summary.setMaxLossDate(maxLossDate);
        summary.setTradingDays(records.size());

        if (!records.isEmpty()) {
            summary.setAvgDailyProfit(totalProfit.divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP));
        } else {
            summary.setAvgDailyProfit(BigDecimal.ZERO);
        }

        if (userFund != null && userFund.getCostPrice() != null
                && userFund.getHoldShare() != null
                && userFund.getCostPrice().compareTo(BigDecimal.ZERO) > 0
                && userFund.getHoldShare().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal cost = userFund.getHoldShare().multiply(userFund.getCostPrice());
            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                summary.setTotalReturnRate(totalProfit.divide(cost, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            }
        }

        vo.setSummary(summary);
        vo.setProfitCurve(curve);
        vo.setDetailList(details);
        return vo;
    }

    public OverallDailyProfitVO getOverallDailyProfit(Long userId, String period) {
        List<FundDailyProfit> records = fundDailyProfitMapper.selectOverallByUserId(userId);

        Map<String, BigDecimal> dailyMap = new LinkedHashMap<>();
        Map<String, BigDecimal> amountMap = new LinkedHashMap<>();
        for (FundDailyProfit r : records) {
            String date = formatDate(r.getRecordDate());
            BigDecimal existing = dailyMap.get(date);
            if (existing == null) {
                dailyMap.put(date, r.getDailyProfit());
            } else {
                dailyMap.put(date, existing.add(r.getDailyProfit()));
            }
            amountMap.merge(date, r.getHoldAmount(), BigDecimal::add);
        }

        String startDate = computeStartDate(period);

        List<OverallDailyProfitVO.CurvePoint> dailyList = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal maxProfit = null;
        BigDecimal maxLoss = null;
        String maxProfitDate = null;
        String maxLossDate = null;
        int count = 0;

        for (Map.Entry<String, BigDecimal> entry : dailyMap.entrySet()) {
            String date = entry.getKey();
            if (startDate != null && date.compareTo(startDate) < 0) continue;

            BigDecimal dailyProfit = entry.getValue();
            cumulative = cumulative.add(dailyProfit);
            totalProfit = totalProfit.add(dailyProfit);
            count++;

            OverallDailyProfitVO.CurvePoint point = new OverallDailyProfitVO.CurvePoint();
            point.setRecordDate(date);
            point.setDailyProfit(dailyProfit.setScale(2, RoundingMode.HALF_UP));
            point.setCumulativeProfit(cumulative.setScale(2, RoundingMode.HALF_UP));
            point.setHoldAmount(amountMap.getOrDefault(date, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
            dailyList.add(point);

            if (maxProfit == null || dailyProfit.compareTo(maxProfit) > 0) {
                maxProfit = dailyProfit;
                maxProfitDate = date;
            }
            if (maxLoss == null || dailyProfit.compareTo(maxLoss) < 0) {
                maxLoss = dailyProfit;
                maxLossDate = date;
            }
        }

        OverallDailyProfitVO.Summary summary = new OverallDailyProfitVO.Summary();
        summary.setTotalProfit(totalProfit.setScale(2, RoundingMode.HALF_UP));
        summary.setMaxDailyProfit(maxProfit != null ? maxProfit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        summary.setMaxDailyLoss(maxLoss != null ? maxLoss.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        summary.setMaxProfitDate(maxProfitDate);
        summary.setMaxLossDate(maxLossDate);
        summary.setAvgDailyProfit(count > 0
                ? totalProfit.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        OverallDailyProfitVO vo = new OverallDailyProfitVO();
        vo.setSummary(summary);
        vo.setDailyList(dailyList);
        return vo;
    }

    private String computeStartDate(String period) {
        if (period == null || "all".equals(period)) return null;
        LocalDate today = LocalDate.now(BEIJING_ZONE);
        LocalDate start;
        switch (period) {
            case "1month":  start = today.minusMonths(1); break;
            case "3month":  start = today.minusMonths(3); break;
            case "6month":  start = today.minusMonths(6); break;
            case "1year":   start = today.minusYears(1); break;
            case "3year":   start = today.minusYears(3); break;
            default:        return null;
        }
        return start.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatTrendDate(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(new Date(ts));
        } catch (Exception e) {
            return timestamp;
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }
}
