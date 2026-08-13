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
@Schema(description = "복지로 중앙부처 복지서비스 목록 응답 DTO")
public class BokjiroListResponseDTO {

    @Schema(description = "전체 데이터 수", example = "100")
    private int totalCount;

    @Schema(description = "페이지 번호", example = "1")
    private int pageNo;

    @Schema(description = "한 페이지 결과 수", example = "10")
    private int numOfRows;

    @Schema(description = "결과 코드 (0 또는 00: 성공)", example = "0")
    private String resultCode;

    @Schema(description = "결과 메시지", example = "SUCCESS")
    private String resultMessage;

    @Schema(description = "복지서비스 목록")
    private List<WelfareItem> servList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "복지서비스 목록 항목")
    public static class WelfareItem {
        @Schema(description = "서비스 ID", example = "WLF00001188")
        private String servId;

        @Schema(description = "서비스명", example = "산모·신생아 건강관리 지원사업")
        private String servNm;

        @Schema(description = "소관부처명", example = "보건복지부")
        private String jurMnofNm;

        @Schema(description = "소관조직명", example = "출산정책과")
        private String jurOrgNm;

        @Schema(description = "조회수", example = "12345")
        private String inqNum;

        @Schema(description = "서비스 요약")
        private String servDgst;

        @Schema(description = "서비스 상세링크")
        private String servDtlLink;

        @Schema(description = "서비스 등록일", example = "20220122")
        private String svcfrstRegTs;

        @Schema(description = "생애주기")
        private String lifeArray;

        @Schema(description = "관심주제")
        private String intrsThemaArray;

        @Schema(description = "가구유형")
        private String trgterIndvdlArray;

        @Schema(description = "지원주기", example = "1회성 / 수시 / 월")
        private String sprtCycNm;

        @Schema(description = "제공유형", example = "전자바우처(바우처) / 현금지급")
        private String srvPvsnNm;

        @Schema(description = "문의처", example = "129")
        private String rprsCtadr;

        @Schema(description = "온라인 신청 가능 여부 (Y/N)", example = "Y")
        private String onapPsbltYn;
    }
}
