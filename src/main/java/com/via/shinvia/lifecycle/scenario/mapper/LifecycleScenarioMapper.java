package com.via.shinvia.lifecycle.scenario.mapper;

import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LifecycleScenarioMapper {

    Long findActiveScenarioIdByUserId(@Param("userId") Long userId);

    int insertScenario(@Param("scenario") LifecycleScenarioRecord scenario);
}
