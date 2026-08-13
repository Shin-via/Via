package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildbirthSurveyRequest {

    // 출산 예정일
    private LocalDate targetDate;

    // 첫째, 둘째, 셋째 등 출생 순서
    // 복지지원 금액 또는 대상조건 판단에도 사용 가능
    private Integer childOrder;

    // 예상 양육비 수준
    private LifestyleLevel lifestyleLevel;

    // 산후조리원 이용 여부
    private Boolean postpartumCare;

    // 출산 시점 예상 거주 시도
    // 예: 서울특별시
    private String regionSido;

    // 출산 시점 예상 거주 시군구
    // 지역별 출산지원 조회 시 사용
    private String regionSigungu;
}