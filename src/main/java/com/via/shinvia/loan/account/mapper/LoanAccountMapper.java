package com.via.shinvia.loan.account.mapper;

import com.via.shinvia.loan.account.entity.LoanAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoanAccountMapper {

    LoanAccount findByExternalLoanKey(
            @Param("connectionId") Long connectionId,
            @Param("externalLoanKey") String externalLoanKey
    );

    int insertLoanAccount(LoanAccount loanAccount);

    int updateLoanAccount(LoanAccount loanAccount);
}