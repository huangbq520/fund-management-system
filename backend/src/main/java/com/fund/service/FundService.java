package com.fund.service;

import com.fund.entity.Fund;
import com.fund.mapper.FundMapper;
import com.fund.vo.FundDataVO;
import com.fund.vo.FundDetailVO;
import com.fund.vo.FundEstimateVO;
import com.fund.vo.FundHoldingVO;
import com.fund.vo.FundTrendVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Fund Service - 数据处理服务
 * 负责数据清洗、格式统一、字段映射、数据聚合
 */
@Service
public class FundService {
    
    private static final Logger logger = LoggerFactory.getLogger(FundService.class);
    
    @Resource
    private FundMapper fundMapper;
    
    @Resource
    private FundApiService fundApiService;
    
    /**
     * Search fund by code - 获取基金实时信息
     * 优先使用天天基金数据，腾讯财经数据作为兜底
     */
    public FundEstimateVO searchFund(String fundCode) {
        logger.info("搜索基金: fundCode={}", fundCode);
        
        // 优先调用天天基金接口
        FundEstimateVO tianTianVO = fundApiService.getFundFromTianTian(fundCode);
        
        if (tianTianVO != null && tianTianVO.getFundCode() != null) {
            // 天天基金数据有效，尝试用腾讯财经数据补充/覆盖
            FundEstimateVO tencentVO = fundApiService.getFundFromTencent(fundCode);
            if (tencentVO != null && tencentVO.getJzrq() != null) {
                // 如果腾讯财经的净值日期更晚，使用腾讯财经的数据
                if (tianTianVO.getJzrq() == null || 
                    tencentVO.getJzrq().compareTo(tianTianVO.getJzrq()) > 0) {
                    tianTianVO.setDwjz(tencentVO.getDwjz());
                    tianTianVO.setGszzl(tencentVO.getGszzl());
                    tianTianVO.setJzrq(tencentVO.getJzrq());
                    logger.info("使用腾讯财经数据覆盖: fundCode={}", fundCode);
                }
            }
            return tianTianVO;
        }
        
        // 天天基金失败，尝试腾讯财经
        FundEstimateVO tencentVO = fundApiService.getFundFromTencent(fundCode);
        if (tencentVO != null && tencentVO.getFundCode() != null) {
            logger.info("使用腾讯财经数据: fundCode={}", fundCode);
            return tencentVO;
        }
        
        // 两个接口都失败
        logger.warn("基金数据获取失败: fundCode={}", fundCode);
        return null;
    }
    
    /**
     * Get all funds with real-time data - 获取用户已添加的所有基金实时数据
     */
    public List<FundEstimateVO> getFundList() {
        logger.info("获取基金列表");
        
        // 从数据库获取用户添加的基金
        List<Fund> funds = fundMapper.selectAll();
        List<FundEstimateVO> result = new ArrayList<>();
        
        for (Fund fund : funds) {
            try {
                FundEstimateVO estimateVO = searchFund(fund.getFundCode());
                if (estimateVO != null) {
                    // 确保基金名称与数据库一致
                    if (estimateVO.getFundName() == null || estimateVO.getFundName().isEmpty()) {
                        estimateVO.setFundName(fund.getFundName());
                    }
                    result.add(estimateVO);
                } else {
                    // 接口失败时返回基础信息
                    FundEstimateVO vo = new FundEstimateVO();
                    vo.setFundCode(fund.getFundCode());
                    vo.setFundName(fund.getFundName());
                    result.add(vo);
                }
            } catch (Exception e) {
                logger.error("获取基金实时数据失败: fundCode={}, error={}", fund.getFundCode(), e.getMessage());
                // 仍然返回基础信息
                FundEstimateVO vo = new FundEstimateVO();
                vo.setFundCode(fund.getFundCode());
                vo.setFundName(fund.getFundName());
                result.add(vo);
            }
        }
        
        return result;
    }
    
    /**
     * Get fund detail - 获取基金详情（基础信息+持仓+走势）
     */
    public FundDetailVO getFundDetail(String fundCode) {
        logger.info("获取基金详情: fundCode={}", fundCode);
        
        FundDetailVO detailVO = new FundDetailVO();
        
        // 1. 获取基础信息
        FundEstimateVO estimateVO = searchFund(fundCode);
        if (estimateVO != null) {
            detailVO.setFundCode(estimateVO.getFundCode());
            detailVO.setFundName(estimateVO.getFundName());
            detailVO.setDwjz(estimateVO.getDwjz());
            detailVO.setGsz(estimateVO.getGsz());
            detailVO.setGszzl(estimateVO.getGszzl());
            detailVO.setGztime(estimateVO.getGztime());
            detailVO.setJzrq(estimateVO.getJzrq());
        }
        
        // 2. 获取持仓数据（失败不影响其他数据）
        try {
            List<FundHoldingVO> holdings = fundApiService.getFundHoldings(fundCode);
            detailVO.setHoldings(holdings);
        } catch (Exception e) {
            logger.error("获取持仓数据失败: fundCode={}, error={}", fundCode, e.getMessage());
            detailVO.setHoldings(new ArrayList<>());
        }
        
        // 3. 获取走势数据（失败不影响其他数据）
        try {
            List<FundTrendVO> trends = fundApiService.getFundTrend(fundCode);
            detailVO.setHistoryTrend(trends);
        } catch (Exception e) {
            logger.error("获取走势数据失败: fundCode={}, error={}", fundCode, e.getMessage());
            detailVO.setHistoryTrend(new ArrayList<>());
        }
        
        return detailVO;
    }
    
    /**
     * Add fund - 添加基金到列表
     */
    public boolean addFund(String fundCode, String fundName) {
        logger.info("添加基金: fundCode={}, fundName={}", fundCode, fundName);
        
        // 校验基金代码唯一性
        int count = fundMapper.countByCode(fundCode);
        if (count > 0) {
            logger.warn("基金已存在: fundCode={}", fundCode);
            return false;
        }
        
        // 获取基金实时信息以验证基金是否存在
        FundEstimateVO estimateVO = searchFund(fundCode);
        if (estimateVO != null && estimateVO.getFundName() != null) {
            // 使用接口返回的基金名称
            fundName = estimateVO.getFundName();
        }
        
        // 保存到数据库
        Fund fund = new Fund();
        fund.setFundCode(fundCode);
        fund.setFundName(fundName);
        
        int result = fundMapper.insert(fund);
        return result > 0;
    }
    
    /**
     * Delete fund - 从列表中删除基金
     */
    public boolean deleteFund(String fundCode) {
        logger.info("删除基金: fundCode={}", fundCode);
        
        int result = fundMapper.deleteByCode(fundCode);
        return result > 0;
    }
    
    /**
     * 获取基金完整数据（统一接口）
     * 聚合天天基金、腾讯财经、东方财富持仓、东方财富走势4个接口数据
     * 
     * @param fundCode 基金代码
     * @return FundDataVO 统一基金数据对象
     */
    public FundDataVO getFundData(String fundCode) {
        logger.info("获取基金完整数据: fundCode={}", fundCode);
        
        return fundApiService.getFundData(fundCode);
    }
}