package com.via.shinvia.loananalysis.mapper;

import com.via.shinvia.loananalysis.dto.LoanAccountAnalysisDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// ???? ?? Mapper
@Mapper
public interface LoanAccountAnalysisMapper {

    // ??? ?? ?? ?? ??
    List<LoanAccountAnalysisDTO> findActiveLoansByUserId(
            @Param("userId") Long userId
    );

    // ??? ?? 1? ??
    LoanAccountAnalysisDTO findLoanById(
            @Param("userId") Long userId,
            @Param("loanAccountId") Long loanAccountId
    );
}