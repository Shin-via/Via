package com.via.shinvia.loananalysis.mapper;

import com.via.shinvia.loananalysis.dto.FinancialCapacityDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// ???? ?? Mapper
@Mapper
public interface FinancialCapacityMapper {

    // ??? ?? ???? ??
    FinancialCapacityDTO findFinancialCapacityByUserId(
            @Param("userId") Long userId
    );
}