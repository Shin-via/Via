package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.mapper.PlanSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlanSnapshotServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PlanSnapshotMapper mapper;

    @Captor
    private ArgumentCaptor<String> jsonCaptor;

    private PlanSnapshotService service() {
        return new PlanSnapshotService(mapper);
    }

    @Test
    void 레버가_없어도_빈_배열로_저장된다() {
        service().save(USER_ID, new BigDecimal("100000000"), null, List.of(), 100, 100, new BigDecimal("5000000"));

        verify(mapper).upsert(
                eqLong(USER_ID), eqAmount("100000000"), eqNull(), jsonCaptor.capture(), eqInt(100), eqInt(100), eqAmount("5000000")
        );
        assertThat(jsonCaptor.getValue()).isEqualTo("[]");
    }

    @Test
    void 선택한_레버가_JSON_배열로_직렬화된다() {
        List<LeverIntensityCalculator.LeverSelection> selections = List.of(
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.INCOME_CHANGE, new BigDecimal("20")),
                new LeverIntensityCalculator.LeverSelection(LeverIntensityCalculator.LeverType.LOAN_PREPAYMENT, new BigDecimal("30000000"))
        );

        service().save(USER_ID, new BigDecimal("100000000"), "SEED_MONEY", selections, 120, 80, new BigDecimal("100000000"));

        verify(mapper).upsert(
                eqLong(USER_ID), eqAmount("100000000"), org.mockito.ArgumentMatchers.eq("SEED_MONEY"),
                jsonCaptor.capture(), eqInt(120), eqInt(80), eqAmount("100000000")
        );
        assertThat(jsonCaptor.getValue())
                .contains("\"leverType\":\"INCOME_CHANGE\"")
                .contains("\"leverType\":\"LOAN_PREPAYMENT\"")
                .contains("\"intensity\":30000000");
    }

    private Long eqLong(Long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private BigDecimal eqAmount(String value) {
        return org.mockito.ArgumentMatchers.eq(new BigDecimal(value));
    }

    private int eqInt(int value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private String eqNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
