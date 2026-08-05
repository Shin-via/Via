package com.via.shinvia.policy.client;

import com.via.shinvia.policy.dto.FinancialProductDTO;
import com.via.shinvia.policy.dto.FinancialProductDetailDTO;
import com.via.shinvia.policy.dto.FinancialProductPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
// 서민금융 상품 외부 API 호출 기능
public class KinfaFinancialProductClient {

    private static final String BASE_URL =
            "https://www.kinfa.or.kr/financialProduct/";

    private static final Pattern WELFARE_CARD_PATTERN = Pattern.compile(
            "<p class=\"card-tit\">(.*?)</p>.*?" +
                    "<span class=\"dd fc-pri\">(.*?)</span>.*?" +
                    "<span class=\"dd fc-pri\">(.*?)</span>.*?" +
                    "<span class=\"dd\">(.*?)</span>",
            Pattern.DOTALL
    );

    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "allCount\"\\)\\.text\\(\"(\\d+)\"\\)"
    );

    private static final Pattern WELFARE_ID_PATTERN = Pattern.compile(
            "name=\"sn\"\\s+value=\"([^\"]+)\""
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FinancialProductPageDTO findProducts(
            ProductType productType,
            String keyword,
            int page,
            int size
    ) {
        return findProducts(productType, keyword, page, size, Map.of());
    }

    public FinancialProductPageDTO findProducts(
            ProductType productType,
            String keyword,
            int page,
            int size,
            Map<String, Object> filters
    ) {
        boolean localFiltering = hasCriteria(keyword, filters);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("selectKeyword1", "all");
        request.put("searchKeyword1", keyword == null ? "" : keyword.trim());
        request.put("recordCountPerPage", localFiltering ? 1000 : size);
        request.put("currentPageNo", localFiltering ? 1 : page + 1);
        request.put("sortGbn", "");
        request.put("prdDs", productType.productCode);
        request.putAll(filters);

        String response = restClient
                .post()
                .uri(BASE_URL + productType.endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("서민금융 상품 응답이 비어 있습니다.");
        }

        FinancialProductPageDTO result = productType == ProductType.WELFARE
                ? parseWelfare(response, page, size)
                : parseJson(response, productType, page, size);

        return localFiltering
                ? filterLocally(result.getProducts(), keyword, filters, page, size)
                : result;
    }

    public FinancialProductDetailDTO findDetail(
            ProductType productType,
            String id
    ) {
        String response = restClient
                .post()
                .uri(BASE_URL + productType.detailEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("sn", id))
                .retrieve()
                .body(String.class);

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("상품 상세정보를 찾을 수 없습니다.");
        }

        try {
            JsonNode item = objectMapper.readTree(response);
            return switch (productType) {
                case ASSET -> assetDetail(item);
                case SOCIAL -> socialDetail(item);
                case WELFARE -> welfareDetail(item);
            };
        } catch (Exception e) {
            throw new IllegalStateException("상품 상세정보 파싱에 실패했습니다.", e);
        }
    }

    private FinancialProductPageDTO parseJson(
            String response,
            ProductType productType,
            int page,
            int size
    ) {
        try {
            JsonNode root = objectMapper.readTree(response);
            List<FinancialProductDTO> products = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode item : root) {
                    products.add(productType == ProductType.ASSET
                            ? assetProduct(item)
                            : socialProduct(item));
                }
            }

            long total = root.isEmpty()
                    ? 0
                    : parseLong(text(root.get(0), "countAll"));

            return page(products, page, size, total);
        } catch (Exception e) {
            throw new IllegalStateException("서민금융 상품 JSON 파싱에 실패했습니다.", e);
        }
    }

    private FinancialProductDTO assetProduct(JsonNode item) {
        return FinancialProductDTO.builder()
                .id(text(item, "sn"))
                .searchText(item.toString())
                .title(text(item, "fincPrdNm"))
                .badge(defaultValue(text(item, "prdDs"), "자산형성"))
                .firstLabel("저축·적립금액")
                .firstValue(text(item, "svnAmt"))
                .secondLabel("금리/매칭금액")
                .secondValue(text(item, "highestInrtMxmMtcnAmt"))
                .institution(text(item, "trtmInsttVal"))
                .build();
    }

    private FinancialProductDTO socialProduct(JsonNode item) {
        String id = text(item, "gno");
        return FinancialProductDTO.builder()
                .id(id)
                .searchText(item.toString())
                .title(text(item, "spprtIsttNm"))
                .badge("사회연대금융")
                .firstLabel("지원대상")
                .firstValue(text(item, "spprtTrgt"))
                .secondLabel("분류")
                .secondValue(text(item, "clsf"))
                .institution(text(item, "ofrInsttNm"))
                .build();
    }

    private FinancialProductPageDTO parseWelfare(
            String response,
            int page,
            int size
    ) {
        List<FinancialProductDTO> products = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        Matcher idMatcher = WELFARE_ID_PATTERN.matcher(response);
        while (idMatcher.find()) {
            ids.add(idMatcher.group(1));
        }

        Matcher cards = WELFARE_CARD_PATTERN.matcher(response);

        while (cards.find()) {
            String id = ids.size() > products.size()
                    ? ids.get(products.size())
                    : "";
            products.add(FinancialProductDTO.builder()
                    .id(id)
                    .searchText(clean(cards.group(0)))
                    .title(clean(cards.group(1)))
                    .badge("복합지원")
                    .firstLabel("지원대상")
                    .firstValue(clean(cards.group(2)))
                    .secondLabel("대상(연령)")
                    .secondValue(clean(cards.group(3)))
                    .institution(clean(cards.group(4)))
                    .build());
        }

        Matcher totalMatcher = TOTAL_PATTERN.matcher(response);
        long total = totalMatcher.find()
                ? parseLong(totalMatcher.group(1))
                : products.size();

        return page(products, page, size, total);
    }

    private FinancialProductDetailDTO assetDetail(JsonNode item) {
        Map<String, String> summary = fields(item,
                "상품유형", "prdDs",
                "상품특징", "prdChrct",
                "저축·적립금액", "svnAmt",
                "금리/매칭금액", "highestInrtMxmMtcnAmt",
                "가입기간", "joinPrid",
                "지급방법", "ipawy",
                "제공기관", "ofrInsttNm");
        Map<String, String> conditions = fields(item,
                "지원대상", "trgt",
                "상세 지원조건", "spprtSpprtDetlCnd",
                "연령", "age",
                "소득기준", "incmeRcgnzAmnt",
                "거주지역", "rsdnZone");
        Map<String, String> application = fields(item,
                "신청방법", "etcMthod",
                "취급기관", "trtmInsttVal",
                "문의처", "inqy",
                "기타 참고사항", "etcNoitm");

        return detail(text(item, "fincPrdNm"), "자산형성상품",
                "/asset-products", summary, conditions, application,
                text(item, "relatSite"));
    }

    private FinancialProductDetailDTO socialDetail(JsonNode item) {
        Map<String, String> summary = fields(item,
                "분류", "clsf",
                "모집일정", "rcritSchdl",
                "주관기관", "mngeInstt",
                "운영기관", "oprInstt");
        Map<String, String> conditions = fields(item,
                "지원대상", "spprtTrgt",
                "사업 개요", "projOverview",
                "지원대상 상세조건", "spprtTrgtDetlCnd");
        Map<String, String> application = fields(item,
                "신청방법", "aplyMthod",
                "문의처", "inqy",
                "기타 참고사항", "etcNoitm");

        return detail(text(item, "spprtIsttNm"), "사회연대금융",
                "/social-finance", summary, conditions, application, null);
    }

    private FinancialProductDetailDTO welfareDetail(JsonNode item) {
        Map<String, String> summary = fields(item,
                "지원사업명", "spprtBizNm",
                "지원대상", "trgtSttn",
                "대상 상세", "detlSttn",
                "취급 재단·기관", "fndtnNm");
        Map<String, String> conditions = fields(item,
                "지원대상 상세조건", "detlCtns");
        Map<String, String> application = fields(item,
                "대표전화번호", "rprsTelno");

        return detail(text(item, "spprtBizNm"), "복합지원",
                "/welfare-support", summary, conditions, application, null);
    }

    private FinancialProductDetailDTO detail(
            String title,
            String badge,
            String listPath,
            Map<String, String> summary,
            Map<String, String> conditions,
            Map<String, String> application,
            String relatedSite
    ) {
        return FinancialProductDetailDTO.builder()
                .title(title)
                .badge(badge)
                .listPath(listPath)
                .summary(summary)
                .conditions(conditions)
                .application(application)
                .relatedSite(normalizeUrl(relatedSite))
                .build();
    }

    private Map<String, String> fields(JsonNode item, String... names) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i += 2) {
            String value = text(item, names[i + 1]);
            if (!value.isBlank() && !"-".equals(value.trim())) {
                result.put(names[i], value);
            }
        }
        return result;
    }

    private FinancialProductPageDTO page(
            List<FinancialProductDTO> products,
            int page,
            int size,
            long total
    ) {
        int totalPages = total == 0
                ? 0
                : (int) Math.ceil((double) total / size);

        return FinancialProductPageDTO.builder()
                .products(products)
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .first(page == 0)
                .last(totalPages == 0 || page >= totalPages - 1)
                .build();
    }

    private boolean hasCriteria(String keyword, Map<String, Object> filters) {
        if (keyword != null && !keyword.isBlank()) return true;
        return filters.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("chbt_"))
                .anyMatch(entry -> entry.getValue() != null && !entry.getValue().toString().isBlank());
    }

    private FinancialProductPageDTO filterLocally(
            List<FinancialProductDTO> source,
            String keyword,
            Map<String, Object> filters,
            int page,
            int size
    ) {
        List<String> groups = filters.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("chbt_"))
                .map(entry -> entry.getValue() == null ? "" : entry.getValue().toString())
                .filter(value -> !value.isBlank() && !"전국".equals(value))
                .toList();

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<FinancialProductDTO> filtered = source.stream()
                .filter(product -> {
                    String haystack = product.getSearchText() == null ? "" : product.getSearchText();
                    if (!normalizedKeyword.isBlank() && !haystack.contains(normalizedKeyword)) return false;
                    for (String group : groups) {
                        boolean matched = java.util.Arrays.stream(group.split(","))
                                .map(String::trim)
                                .anyMatch(haystack::contains);
                        if (!matched) return false;
                    }
                    return true;
                })
                .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return page(filtered.subList(from, to), page, size, filtered.size());
    }

    private String text(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private String clean(String value) {
        return HtmlUtils.htmlUnescape(value)
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String normalizeUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return "https://" + value;
    }

    public enum ProductType {
        ASSET("fineUsePropProdList.do", "fineUsePropProdListDtl.do", "2"),
        SOCIAL("fineUseFinanceList.do", "fineUseFinanceListDtl.do", "3"),
        WELFARE("fineUseWelfareProdList.do", "fineUseWelfareListDtl.do", "4");

        private final String endpoint;
        private final String detailEndpoint;
        private final String productCode;

        ProductType(String endpoint, String detailEndpoint, String productCode) {
            this.endpoint = endpoint;
            this.detailEndpoint = detailEndpoint;
            this.productCode = productCode;
        }
    }
}
