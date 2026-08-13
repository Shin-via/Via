package com.via.shinvia.policy.bokjiro.repository;

import com.via.shinvia.policy.bokjiro.entity.BokjiroEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface BokjiroRepository {

    void createTableIfNotExists();

    int upsert(BokjiroEntity entity);

    int deactivateAll();

    List<BokjiroEntity> findAll();

    long count();

    Optional<BokjiroEntity> findByServId(@Param("servId") String servId);
}
