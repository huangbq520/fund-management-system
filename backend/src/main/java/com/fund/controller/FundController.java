package com.fund.controller;

import com.fund.entity.Fund;
import com.fund.mapper.FundMapper;
import com.fund.service.FundDataService;
import com.fund.service.FundSearchService;
import com.fund.vo.ApiResponse;
import com.fund.vo.FundData;
import com.fund.vo.FundSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
    public ApiResponse<List<Fund>> listFunds() {
        logger.info("API: 获取基金列表");
        try {
            List<Fund> funds = fundMapper.selectAll();
            return ApiResponse.success(funds);
        } catch (Exception e) {
            logger.error("获取基金列表失败: {}", e.getMessage());
            return ApiResponse.error("获取基金列表失败");
        }
    }

    @PostMapping("/add")
    public ApiResponse<Map<String, Object>> addFund(@RequestBody Map<String, String> request) {
        String fundCode = request.get("fundCode");
        String fundName = request.get("fundName");
        logger.info("API: 添加基金, fundCode={}, fundName={}", fundCode, fundName);

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        try {
            Fund existingFund = fundMapper.selectByCode(fundCode);
            if (existingFund != null) {
                return ApiResponse.error("该基金已存在");
            }

            Fund fund = new Fund();
            fund.setFundCode(fundCode);
            fund.setFundName(fundName);
            fundMapper.insert(fund);

            Map<String, Object> result = new HashMap<>();
            result.put("fundCode", fundCode);
            result.put("fundName", fundName);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("添加基金失败: fundCode={}, error={}", fundCode, e.getMessage());
            return ApiResponse.error("添加基金失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResponse<Void> deleteFund(@RequestBody Map<String, String> request) {
        String fundCode = request.get("fundCode");
        logger.info("API: 删除基金, fundCode={}", fundCode);

        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }

        try {
            fundMapper.deleteByCode(fundCode);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("删除基金失败: fundCode={}, error={}", fundCode, e.getMessage());
            return ApiResponse.error("删除基金失败");
        }
    }
}
