package com.fund.controller;

import com.fund.service.MarketIndexService;
import com.fund.vo.ApiResponse;
import com.fund.vo.MarketIndexData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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
}