package com.via.shinvia.policy.asset.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AssetProductSearchDTO {
    private String keyword = "";
    private int page;
    private int size = 20;
    private List<String> targets = List.of();
    private List<String> products = List.of();
    private List<String> terms = List.of();
    private List<String> ages = List.of();
    private List<String> incomes = List.of();
    private List<String> regions = List.of();

    public Map<String, Object> filters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("spprtTrgtCmns", join(targets));
        filters.put("prd", join(products));
        filters.put("tofpl", join(terms));
        filters.put("agegrpAsstFrmn", join(ages));
        filters.put("incmeBaseAsstFrmn", join(incomes));
        filters.put("svcOfrZone", join(regions));
        flags(filters, "1", targets, List.of("근로자", "사업자", "소상공인", "대학생·청년", "채무조정자", "금융취약계층", "사회적경제기업", "농림어업인", "기타"));
        flags(filters, "4", ages, List.of("청년층", "중장년층"));
        flags(filters, "5", regions, List.of("서울", "경기", "인천", "부산", "광주", "대구", "대전", "울산", "세종", "강원", "전남", "전북", "충남", "충북", "경남", "경북", "제주"));
        flags(filters, "6", products, List.of("적금", "예금", "매립적립통장"));
        flags(filters, "7", terms, List.of("1년 이하", "3년 이하", "5년 이하"));
        flags(filters, "8", incomes, List.of("소득기준 있음", "소득기준 없음"));
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
