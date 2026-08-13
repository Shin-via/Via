package com.via.shinvia.policy.bokjiro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "복지로 중앙부처 복지서비스 상세 응답 DTO")
public class BokjiroDetailResponseDTO {

    @Schema(description = "서비스 ID", example = "WLF00001138")
    private String servId;

    @Schema(description = "서비스명", example = "자활근로(기초, 차상위)")
    private String servNm;

    @Schema(description = "소관부처명", example = "보건복지부 자립지원과")
    private String jurMnofNm;

    @Schema(description = "소관조직명", example = "자활정책과")
    private String jurOrgNm;

    @Schema(description = "서비스 요약/개요")
    private String wlfareInfoOutlCn;

    @Schema(description = "기준연도", example = "2025")
    private String crtrYr;

    @Schema(description = "문의처", example = "129")
    private String rprsCtadr;

    @Schema(description = "지원주기", example = "월")
    private String sprtCycNm;

    @Schema(description = "제공유형", example = "현금")
    private String srvPvsnNm;

    @Schema(description = "생애주기")
    private String lifeArray;

    @Schema(description = "관심주제")
    private String intrsThemaArray;

    @Schema(description = "가구유형")
    private String trgterIndvdlArray;

    @Schema(description = "대상자 상세내용")
    private String tgtrDtlCn;

    @Schema(description = "선정기준 내용")
    private String slctCritCn;

    @Schema(description = "급여서비스 지원 내용")
    private String alwServCn;

    @Schema(description = "서비스 이용 및 신청방법 목록")
    private List<DetailSubItem> applmetList;

    @Schema(description = "문의처 목록")
    private List<DetailSubItem> inqplCtadrList;

    @Schema(description = "관련 사이트 목록")
    private List<DetailSubItem> inqplHmpgReldList;

    @Schema(description = "서식/자료 목록")
    private List<DetailSubItem> basfrmList;

    @Schema(description = "근거법령 목록")
    private List<DetailSubItem> baslawList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "복지서비스 상세 하위 항목 (신청방법, 문의처, 사이트, 서식, 법령)")
    public static class DetailSubItem {
        @Schema(description = "서비스구분코드", example = "070")
        private String servSeCode;

        @Schema(description = "구분 상세 명칭 (방법명, 문의처명, 사이트명, 서식명, 법령명)")
        private String servSeDetailNm;

        @Schema(description = "상세 링크 / 전화번호 / URL")
        private String servSeDetailLink;
    }
}
