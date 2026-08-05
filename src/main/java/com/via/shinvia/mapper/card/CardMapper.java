package com.via.shinvia.mapper.card;

import com.via.shinvia.entity.card.CardAccount;
import com.via.shinvia.entity.card.CardTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CardMapper {

    Long findInstitutionIdByOrgCode(@Param("orgCode") String orgCode);

    CardAccount findByExternalCardKey(@Param("externalCardKey") String externalCardKey);

    void insertCardAccount(CardAccount cardAccount);

    void updateCardAccount(CardAccount cardAccount);

    Long findCardAccountIdByExternalCardKey(@Param("externalCardKey") String externalCardKey);

    void upsertCardTransactions(@Param("transactions") List<CardTransaction> transactions);

    void deleteCardAccountsByAppUserId(@Param("appUserId") Long appUserId);
}
