package com.via.shinvia.lifecycle.common.model;

public enum SalaryGrowthScenario {
    CONSERVATIVE("보수적"), BASE("기준"), OPTIMISTIC("낙관적"), CUSTOM("직접 입력");

    private final String label;
    SalaryGrowthScenario(String label) { this.label = label; }
    public String getLabel() { return label; }
}
