package com.via.shinvia.lifecycle.common.model;

public enum CurrentHousingType {
    FAMILY("가족과 거주"), MONTHLY_RENT("월세"), JEONSE("전세"), OWN("자가");

    private final String label;
    CurrentHousingType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
