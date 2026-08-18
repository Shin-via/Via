package com.via.shinvia.lifecycle.scenario.mapper;

import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioRecord;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LifecycleScenarioMapper {

    Long findActiveScenarioIdByUserId(@Param("userId") Long userId);

    int insertScenario(@Param("scenario") LifecycleScenarioRecord scenario);

    List<LifecycleScenarioResponse> findScenariosByUserId(
            @Param("userId") Long userId
    );

    LifecycleScenarioResponse findScenarioByIdAndUserId(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    int updateScenario(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId,
            @Param("scenarioName") String scenarioName,
            @Param("description") String description,
            @Param("status") String status
    );

    int archiveScenario(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );

    int updateSimulationResult(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId,
            @Param("resultJson") String resultJson
    );

    String findSimulationResult(
            @Param("scenarioId") Long scenarioId,
            @Param("userId") Long userId
    );
}
