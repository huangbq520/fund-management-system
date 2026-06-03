package com.fund.controller;

import com.fund.entity.User;
import com.fund.mapper.UserMapper;
import com.fund.service.DailyProfitService;
import com.fund.vo.ApiResponse;
import com.fund.vo.DailyProfitVO;
import com.fund.vo.OverallDailyProfitVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/fund")
public class DailyProfitController {

    private static final Logger logger = LoggerFactory.getLogger(DailyProfitController.class);

    @Resource
    private DailyProfitService dailyProfitService;
    
    @Resource
    private UserMapper userMapper;

    @PostMapping("/daily-profit/calculate")
    public ApiResponse<String> triggerCalculation() {
        logger.info("API: 手动触发每日收益统计");
        try {
            dailyProfitService.calculateDailyProfit();
            return ApiResponse.success("每日收益统计完成");
        } catch (Exception e) {
            logger.error("手动触发每日收益统计失败: {}", e.getMessage(), e);
            return ApiResponse.error("统计失败: " + e.getMessage());
        }
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            logger.warn("未从JWT找到用户ID，用户未登录！");
            return null;
        }
        logger.info("从JWT获取到用户ID: {}", userId);
        return userId;
    }

    @GetMapping("/daily-profit/overall")
    public ApiResponse<OverallDailyProfitVO> getOverallDailyProfit(
            HttpServletRequest request,
            @RequestParam(value = "period", defaultValue = "6month") String period) {
        Long userId = getUserId(request);
        logger.info("API: 查询用户整体每日收益, userId={}, period={}", userId, period);

        if (userId == null) {
            return ApiResponse.error(401, "请先登录后再查看收益数据");
        }

        if (!isValidPeriod(period)) {
            return ApiResponse.error("无效的周期参数，支持: 1month, 3month, 6month, 1year, 3year, all");
        }

        OverallDailyProfitVO vo = dailyProfitService.getOverallDailyProfit(userId, period);
        return ApiResponse.success(vo);
    }

    @GetMapping("/daily-profit/{fundCode}")
    public ApiResponse<DailyProfitVO> getFundDailyProfit(
            HttpServletRequest request,
            @PathVariable("fundCode") String fundCode,
            @RequestParam(value = "period", defaultValue = "6month") String period) {
        Long userId = getUserId(request);
        logger.info("API: 查询单基金每日收益, userId={}, fundCode={}, period={}", userId, fundCode, period);

        if (userId == null) {
            return ApiResponse.error(401, "请先登录后再查看收益数据");
        }

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        if (!isValidPeriod(period)) {
            return ApiResponse.error("无效的周期参数，支持: 1month, 3month, 6month, 1year, 3year, all");
        }

        DailyProfitVO vo = dailyProfitService.getFundDailyProfit(userId, fundCode.trim(), period);
        return ApiResponse.success(vo);
    }

    private boolean isValidPeriod(String period) {
        return "1month".equals(period) || "3month".equals(period) ||
               "6month".equals(period) || "1year".equals(period) ||
               "3year".equals(period) || "all".equals(period);
    }
}
