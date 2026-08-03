package com.via.shinvia.policy.welfare.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class WelfareSupportSearchDTO {
    private String keyword = "";
    private int page;
    private int size = 20;
    private List<String> targets = List.of();
    private List<String> ages = List.of();

    public Map<String, Object> filters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("spprtTrgtCmns", join(targets));
        filters.put("agegrp", join(ages));
        flags(filters, "1", targets, List.of("자영업·소상공인", "초중고등학생", "대학생", "취업준비생", "장애인", "북한이탈주민", "환자 등", "한부모가정", "다문화가정", "특수직종사자", "여성", "기타"));
        flags(filters, "4", ages, List.of("영유아", "아동", "청소년", "아동·청소년", "청년·성인", "성인"));
        return filters;
    }

    private void flags(Map<String, Object> filters, String group, List<String> selected, List<String> options) {
        if (selected == null) return;
        for (String value : selected) {
            int index = options.indexOf(value);
            if (index >= 0) filters.put("chbt_" + group + "_" + (index + 1), "Y");
        }
    }

    private String join(List<String> values) {
        return values == null ? "" : String.join(",", values);
    }
}
