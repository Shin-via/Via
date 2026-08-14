package com.via.shinvia.lifecycle.common.model;

public enum RepaymentType {
    EQUAL_PAYMENT("원리금균등상환"), EQUAL_PRINCIPAL("원금균등상환"), BULLET("만기일시상환");

    private final String label;
    RepaymentType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
