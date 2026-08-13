package com.via.shinvia.policy.localbokjiro.repository;

import com.via.shinvia.policy.localbokjiro.entity.LocalBokjiroEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface LocalBokjiroRepository {

    void createTableIfNotExists();

    int upsert(LocalBokjiroEntity entity);

    int deactivateAll();

    List<LocalBokjiroEntity> findAll();

    long count();

    Optional<LocalBokjiroEntity> findByServId(@Param("servId") String servId);
}
