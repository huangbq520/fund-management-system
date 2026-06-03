package com.fund.controller;

import com.fund.service.MarketIndexService;
import com.fund.vo.ApiResponse;
import com.fund.vo.MarketIndexData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketIndexController {

    private static final Logger logger = LoggerFactory.getLogger(MarketIndexController.class);

    @Resource
    private MarketIndexService marketIndexService;

    @GetMapping("/indices")
    public ApiResponse<List<MarketIndexData>> getMarketIndices() {
        logger.info("API: 获取大盘指数数据");
        try {
            List<MarketIndexData> indices = marketIndexService.getMarketIndices();
            return ApiResponse.success(indices);
        } catch (Exception e) {
            logger.error("获取大盘指数失败: {}", e.getMessage());
            return ApiResponse.error("获取大盘指数失败");
        }
    }

    @GetMapping("/kline")
    public ApiResponse<Map<String, Object>> getIndexKline(
            @RequestParam String code,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "101") String klt) {
        logger.info("API: 获取指数K线数据 code={}, startDate={}, endDate={}, klt={}", code, startDate, endDate, klt);
        try {
            Map<String, Object> result = marketIndexService.getIndexKline(code, startDate, endDate, klt);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            logger.warn("参数校验失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("获取指数K线数据失败: {}", e.getMessage());
            return ApiResponse.error("获取指数K线数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/realtime")
    public ApiResponse<MarketIndexData> getIndexRealtime(@RequestParam String code) {
        logger.info("API: 获取指数实时行情 code={}", code);
        try {
            MarketIndexData data = marketIndexService.getIndexRealtime(code);
            return ApiResponse.success(data);
        } catch (IllegalArgumentException e) {
            logger.warn("参数校验失败: {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("获取指数实时行情失败: {}", e.getMessage());
            return ApiResponse.error("获取指数实时行情失败");
        }
    }
}