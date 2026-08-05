package com.via.shinvia.policy.social.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
// 사회연대금융 검색조건 전달 기능
public class SocialFinanceSearchDTO {
    private String keyword = "";
    private int page;
    private int size = 20;
    private List<String> categories = List.of();
    private List<String> targets = List.of();

    public Map<String, Object> filters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("lonNedAmt", join(categories));
        filters.put("spprtTrgtCmns", join(targets));
        flags(filters, "9", categories, List.of("대출(보증)", "투자", "지원제도", "교육/컨설팅", "기타"));
        flags(filters, "1", targets, List.of("사회적기업", "사회적협동조합", "마을기업", "자활기업", "소셜벤처", "기타"));
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
