package com.via.shinvia.lifecycle.common.model;

public enum IndustryCode {
    IT("IT · 정보통신"), FINANCE("금융 · 보험"), MANUFACTURING("제조업"),
    CONSTRUCTION("건설업"), SERVICE("유통 · 서비스"), EDUCATION("교육"),
    HEALTHCARE("보건 · 의료"), PUBLIC("공공 · 행정"), ETC("기타");

    private final String label;
    IndustryCode(String label) { this.label = label; }
    public String getLabel() { return label; }
}
