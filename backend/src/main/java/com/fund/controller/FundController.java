package com.fund.controller;

import com.fund.service.FundService;
import com.fund.vo.ApiResponse;
import com.fund.vo.FundDetailVO;
import com.fund.vo.FundEstimateVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Fund Controller - REST API接口
 * 提供5个标准化RESTful接口
 */
@RestController
@RequestMapping("/api/fund")
@CrossOrigin(origins = "*")
public class FundController {
    
    private static final Logger logger = LoggerFactory.getLogger(FundController.class);
    
    @Resource
    private FundService fundService;
    
    /**
     * GET /api/fund/search
     * 搜索单个基金，获取实时基础信息
     * 
     * @param code 基金代码（必填）
     * @return 基金实时基础信息
     */
    @GetMapping("/search")
    public ApiResponse<FundEstimateVO> search(@RequestParam("code") String code) {
        logger.info("API: 搜索基金, code={}", code);
        
        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }
        
        FundEstimateVO fund = fundService.searchFund(code.trim());
        
        if (fund != null && fund.getFundCode() != null) {
            return ApiResponse.success(fund);
        } else {
            return ApiResponse.error("基金代码不存在或接口调用失败");
        }
    }
    
    /**
     * GET /api/fund/list
     * 获取用户已添加的所有基金实时数据
     * 
     * @return 基金列表
     */
    @GetMapping("/list")
    public ApiResponse<List<FundEstimateVO>> list() {
        logger.info("API: 获取基金列表");
        
        List<FundEstimateVO> funds = fundService.getFundList();
        return ApiResponse.success(funds);
    }
    
    /**
     * GET /api/fund/detail
     * 获取基金详情（基础信息+持仓+走势）
     * 
     * @param code 基金代码（必填）
     * @return 基金详情
     */
    @GetMapping("/detail")
    public ApiResponse<FundDetailVO> detail(@RequestParam("code") String code) {
        logger.info("API: 获取基金详情, code={}", code);
        
        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }
        
        FundDetailVO detail = fundService.getFundDetail(code.trim());
        
        if (detail.getFundCode() != null) {
            return ApiResponse.success(detail);
        } else {
            return ApiResponse.error("基金数据加载失败");
        }
    }
    
    /**
     * POST /api/fund/add
     * 添加基金到列表，校验唯一性
     * 
     * @param request 请求体 {fundCode, fundName}
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<String> add(@RequestBody Map<String, String> request) {
        String fundCode = request.get("fundCode");
        String fundName = request.get("fundName");
        
        logger.info("API: 添加基金, fundCode={}, fundName={}", fundCode, fundName);
        
        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }
        
        if (fundName == null || fundName.trim().isEmpty()) {
            return ApiResponse.error("基金名称不能为空");
        }
        
        boolean success = fundService.addFund(fundCode.trim(), fundName.trim());
        
        if (success) {
            return ApiResponse.success("添加成功", null);
        } else {
            return ApiResponse.error("基金已存在或添加失败");
        }
    }
    
    /**
     * POST /api/fund/delete
     * 从列表中删除基金
     * 
     * @param request 请求体 {fundCode}
     * @return 操作结果
     */
    @PostMapping("/delete")
    public ApiResponse<String> delete(@RequestBody Map<String, String> request) {
        String fundCode = request.get("fundCode");
        
        logger.info("API: 删除基金, fundCode={}", fundCode);
        
        if (fundCode == null || fundCode.trim().isEmpty()) {
            return ApiResponse.error("基金代码不能为空");
        }
        
        boolean success = fundService.deleteFund(fundCode.trim());
        
        if (success) {
            return ApiResponse.success("删除成功", null);
        } else {
            return ApiResponse.error("删除失败");
        }
    }
}