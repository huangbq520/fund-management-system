package com.fund.controller;

import com.fund.entity.Fund;
import com.fund.entity.User;
import com.fund.entity.UserFund;
import com.fund.mapper.FundMapper;
import com.fund.mapper.UserMapper;
import com.fund.mapper.UserFundMapper;
import com.fund.service.FundDataService;
import com.fund.service.FundHoldingService;
import com.fund.service.FundSearchService;
import com.fund.vo.ApiResponse;
import com.fund.vo.FundData;
import com.fund.vo.FundHoldingVO;
import com.fund.vo.FundSearchResult;
import com.fund.vo.PerformanceData;
import com.fund.vo.PortfolioSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fund")
@CrossOrigin(origins = "*")
public class FundController {

    private static final Logger logger = LoggerFactory.getLogger(FundController.class);

    @Resource
    private FundDataService fundDataService;

    @Resource
    private FundSearchService fundSearchService;

    @Resource
    private FundMapper fundMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserFundMapper userFundMapper;

    @Resource
    private FundHoldingService fundHoldingService;

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            logger.warn("未从JWT找到用户ID，用户未登录！");
            return null;
        }
        return userId;
    }

    @GetMapping("/nav-at")
    public ApiResponse<Map<String, Object>> getNavAt(
            @RequestParam("code") String code,
            @RequestParam("date") String date) {
        logger.info("API: 查询历史净值, code={}, date={}", code, date);
        BigDecimal nav = fundDataService.getNavByDate(code, date);
        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("nav", nav);
        return ApiResponse.success(result);
    }

    @GetMapping("/data")
    public ApiResponse<FundData> getFundData(@RequestParam("code") String code) {
        logger.info("API: 获取基金数据, code={}", code);

        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        FundData fundData = fundDataService.getFundData(code.trim());

        if (fundData.getFundCode() != null) {
            return ApiResponse.success(fundData);
        } else {
            return ApiResponse.error("基金数据加载失败");
        }
    }

    @GetMapping("/performance")
    public ApiResponse<PerformanceData> getPerformanceData(
            @RequestParam("code") String code,
            @RequestParam(value = "period", defaultValue = "6month") String period) {
        logger.info("API: 获取基金业绩走势, code={}, period={}", code, period);

        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        if (!isValidPeriod(period)) {
            return ApiResponse.error("无效的周期参数，支持: 1month, 3month, 6month, 1year, 3year, all");
        }

        PerformanceData performanceData = fundDataService.getPerformanceData(code.trim(), period);

        if (performanceData.getFundCode() != null) {
            return ApiResponse.success(performanceData);
        } else {
            return ApiResponse.error("业绩数据加载失败");
        }
    }

    private boolean isValidPeriod(String period) {
        return "1month".equals(period) || "3month".equals(period) ||
               "6month".equals(period) || "1year".equals(period) ||
               "3year".equals(period) || "all".equals(period);
    }

    @GetMapping("/search")
    public ApiResponse<List<FundSearchResult>> searchFunds(@RequestParam("keyword") String keyword) {
        logger.info("API: 搜索基金, keyword={}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResponse.error("搜索关键词不能为空");
        }

        List<FundSearchResult> searchResults = fundSearchService.searchFunds(keyword.trim());

        return ApiResponse.success(searchResults);
    }

    @GetMapping("/list")
    public ApiResponse<List<Fund>> listFunds(HttpServletRequest request) {
        Long userId = getUserId(request);
        logger.info("API: 获取基金列表, userId={}", userId);
        try {
            List<Fund> funds = fundMapper.selectAll(userId);
            return ApiResponse.success(funds);
        } catch (Exception e) {
            logger.error("获取基金列表失败: {}", e.getMessage());
            return ApiResponse.error("获取基金列表失败");
        }
    }

    @GetMapping("/holding/list")
    public ApiResponse<List<FundHoldingVO>> listHoldings(HttpServletRequest request) {
        Long userId = getUserId(request);
        logger.info("API: 获取持仓列表, userId={}", userId);
        try {
            List<FundHoldingVO> holdings = fundHoldingService.getHoldingList(userId);
            return ApiResponse.success(holdings);
        } catch (Exception e) {
            logger.error("获取持仓列表失败: {}", e.getMessage());
            return ApiResponse.error("获取持仓列表失败");
        }
    }

    @GetMapping("/portfolio/summary")
    public ApiResponse<PortfolioSummary> getPortfolioSummary(HttpServletRequest request) {
        Long userId = getUserId(request);
        logger.info("API: 获取组合汇总, userId={}", userId);
        try {
            PortfolioSummary summary = fundHoldingService.getPortfolioSummary(userId);
            return ApiResponse.success(summary);
        } catch (Exception e) {
            logger.error("获取组合汇总失败: {}", e.getMessage());
            return ApiResponse.error("获取组合汇总失败");
        }
    }

    @PostMapping("/add")
    public ApiResponse<Map<String, Object>> addFund(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        String fundCode = request.get("fundCode");
        String fundName = request.get("fundName");
        logger.info("API: 添加基金, fundCode={}, fundName={}, userId={}", fundCode, fundName, userId);

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        try {
            Fund existingFund = fundMapper.selectByCode(fundCode, userId);
            if (existingFund != null) {
                return ApiResponse.error("该基金已存在");
            }

            Fund fund = new Fund();
            fund.setUserId(userId);
            fund.setFundCode(fundCode);
            fund.setFundName(fundName);
            fundMapper.insert(fund);

            UserFund userFund = new UserFund();
            userFund.setUserId(userId);
            userFund.setFundCode(fundCode);
            userFund.setFundName(fundName);
            userFund.setHoldShare(BigDecimal.ZERO);
            userFund.setHoldAmount(BigDecimal.ZERO);
            userFund.setCostPrice(BigDecimal.ZERO);
            userFundMapper.insert(userFund);

            Map<String, Object> result = new HashMap<>();
            result.put("fundCode", fundCode);
            result.put("fundName", fundName);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("添加基金失败: fundCode={}, error={}", fundCode, e.getMessage());
            return ApiResponse.error("添加基金失败: " + e.getMessage());
        }
    }

    @PostMapping("/holding/update")
    public ApiResponse<FundHoldingVO> updateHolding(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        String fundCode = (String) request.get("fundCode");
        String mode = request.containsKey("mode") ? (String) request.get("mode") : "SHARES";
        logger.info("API: 更新持仓, fundCode={}, userId={}, mode={}", fundCode, userId, mode);

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        try {
            UserFund userFund = userFundMapper.findByUserIdAndFundCode(userId, fundCode);
            if (userFund == null) {
                return ApiResponse.error("持仓记录不存在");
            }

            if ("AMOUNT".equals(mode)) {
                fundHoldingService.applyModeTwo(userFund, request);
            } else {
                if (request.containsKey("holdShare")) {
                    userFund.setHoldShare(new BigDecimal(request.get("holdShare").toString()));
                }
                if (request.containsKey("costPrice")) {
                    userFund.setCostPrice(new BigDecimal(request.get("costPrice").toString()));
                }
                BigDecimal holdShare = userFund.getHoldShare();
                BigDecimal costPrice = userFund.getCostPrice();
                if (holdShare != null && costPrice != null
                        && holdShare.compareTo(BigDecimal.ZERO) > 0
                        && costPrice.compareTo(BigDecimal.ZERO) > 0) {
                    userFund.setHoldAmount(holdShare.multiply(costPrice));
                }
            }

            if (request.containsKey("buyDate")) {
                String buyDateStr = (String) request.get("buyDate");
                if (buyDateStr != null && !buyDateStr.isEmpty()) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    userFund.setBuyDate(sdf.parse(buyDateStr));
                }
            }

            userFundMapper.update(userFund);
            FundHoldingVO vo = fundHoldingService.getSingleHolding(userId, fundCode);
            return ApiResponse.success(vo);
        } catch (Exception e) {
            logger.error("更新持仓失败: fundCode={}, error={}", fundCode, e.getMessage());
            return ApiResponse.error("更新持仓失败: " + e.getMessage());
        }
    }

    @PostMapping("/holding/adjust")
    public ApiResponse<FundHoldingVO> adjustHolding(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        String fundCode = (String) request.get("fundCode");
        String type = (String) request.get("type");
        String adjustDateStr = (String) request.get("adjustDate");
        Boolean before3pm = request.containsKey("before3pm") && Boolean.TRUE.equals(request.get("before3pm"));
        logger.info("API: 调整持仓, fundCode={}, type={}, date={}, before3pm={}, userId={}",
                fundCode, type, adjustDateStr, before3pm, userId);

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        try {
            UserFund userFund = userFundMapper.findByUserIdAndFundCode(userId, fundCode);
            if (userFund == null) {
                return ApiResponse.error("持仓记录不存在");
            }

            BigDecimal adjustShare = new BigDecimal(request.get("adjustShare").toString());
            BigDecimal effectiveNAV = null;

            if (adjustDateStr != null && !adjustDateStr.isEmpty()) {
                String navDate = adjustDateStr;
                if (!before3pm) {
                    java.time.LocalDate d = java.time.LocalDate.parse(adjustDateStr);
                    navDate = d.plusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                }
                effectiveNAV = fundDataService.getNavByDate(fundCode, navDate);
                logger.info("历史净值查询: fundCode={}, date={}, navDate={}, nav={}", fundCode, adjustDateStr, navDate, effectiveNAV);
            }

            if ("BUY".equals(type)) {
                if (effectiveNAV == null) {
                    effectiveNAV = new BigDecimal(
                        fundDataService.getFundData(fundCode).getEstimatedNetValue() != null
                            ? fundDataService.getFundData(fundCode).getEstimatedNetValue() : "1");
                }
                fundHoldingService.applyBuy(userFund, adjustShare, effectiveNAV);
            } else if ("SELL".equals(type)) {
                fundHoldingService.applySell(userFund, adjustShare);
            } else {
                return ApiResponse.error("类型错误，只支持 BUY 或 SELL");
            }

            userFundMapper.update(userFund);
            FundHoldingVO vo = fundHoldingService.getSingleHolding(userId, fundCode);
            return ApiResponse.success(vo);
        } catch (Exception e) {
            logger.error("调整持仓失败: fundCode={}, error={}", fundCode, e.getMessage());
            return ApiResponse.error("调整持仓失败: " + e.getMessage());
        }
    }

    @PostMapping("/holding/clear")
    public ApiResponse<Void> clearHolding(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        String fundCode = request.get("fundCode");
        logger.info("API: 清仓, fundCode={}, userId={}", fundCode, userId);

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        try {
            UserFund userFund = userFundMapper.findByUserIdAndFundCode(userId, fundCode);
            if (userFund == null) {
                return ApiResponse.error("持仓记录不存在");
            }

            userFund.setHoldShare(BigDecimal.ZERO);
            userFund.setCostPrice(BigDecimal.ZERO);
            userFund.setHoldAmount(BigDecimal.ZERO);
            userFundMapper.update(userFund);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("清仓失败: fundCode={}, error={}", fundCode, e.getMessage());
            return ApiResponse.error("清仓失败");
        }
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteFund(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        String fundCode = request.get("fundCode");
        logger.info("API: 删除基金, fundCode={}, userId={}", fundCode, userId);

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        try {
            userFundMapper.deleteByUserIdAndFundCode(userId, fundCode);
            fundMapper.deleteByCode(fundCode, userId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("删除基金失败: fundCode={}, error={}", fundCode, e.getMessage());
            return ApiResponse.error("删除基金失败");
        }
    }
}
