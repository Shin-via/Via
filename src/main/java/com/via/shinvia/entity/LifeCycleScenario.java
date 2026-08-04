package com.via.shinvia.entity;

import java.util.List;

public class LifeCycleScenario {
    //시나리오 식별을 위한 식벽자
    int ScenarioId;
    // 시나리오 이름
    String ScenarioName;
    //시나리오 설명
    String Description;
    // 시나리오별 대출 추천을 위한 카테고리 분류를 위한 데이터(임시)
    List<?> ItemType;
}
