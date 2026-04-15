package com.fund.mapper;

import com.fund.entity.Fund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * Fund Mapper - 基金数据操作
 */
@Mapper
public interface FundMapper {
    
    /**
     * 查询所有基金
     */
    List<Fund> selectAll();
    
    /**
     * 根据基金代码查询
     */
    Fund selectByCode(@Param("fundCode") String fundCode);
    
    /**
     * 插入基金
     */
    int insert(Fund fund);
    
    /**
     * 删除基金
     */
    int deleteByCode(@Param("fundCode") String fundCode);
    
    /**
     * 检查基金是否存在
     */
    int countByCode(@Param("fundCode") String fundCode);
}