package com.fund.mapper;

import com.fund.entity.Fund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FundMapper {

    List<Fund> selectAll(@Param("userId") Long userId);

    Fund selectByCode(@Param("fundCode") String fundCode, @Param("userId") Long userId);

    int insert(Fund fund);

    int deleteByCode(@Param("fundCode") String fundCode, @Param("userId") Long userId);

    int countByCode(@Param("fundCode") String fundCode, @Param("userId") Long userId);
}
