// 화면 실행
document.addEventListener(
    "DOMContentLoaded",
    initializePage
);


// 시나리오 표시순서
const SCENARIO_ORDER = [
    "KEEP",
    "PARTIAL_REPAYMENT",
    "REFINANCE",
    "CASH_HOLDING"
];


// 시나리오 한글명
const SCENARIO_NAMES = {
    KEEP: "현재 유지",
    PARTIAL_REPAYMENT: "부분상환",
    REFINANCE: "대환",
    CASH_HOLDING: "현금보유"
};


// 화면 초기화
function initializePage() {

    // 폼 제출 이벤트
    document
        .getElementById("scenarioForm")
        .addEventListener(
            "submit",
            analyzeScenarios
        );

    // 초기화 버튼 이벤트
    document
        .getElementById("resetButton")
        .addEventListener(
            "click",
            resetForm
        );
}


// 대출 시나리오 분석
async function analyzeScenarios(event) {

    // 기본 제출 차단
    event.preventDefault();

    // 요청값 생성
    const requestData =
        createRequestData();

    // 입력값 검증
    if (!validateRequest(requestData)) {
        return;
    }

    // 분석상태 시작
    setLoadingState(true);

    // 기존 결과 숨김
    hideResult();

    // 기존 오류 숨김
    hideError();

    try {

        // 시나리오 API 요청
        const response = await fetch(
            "/api/loan-analysis/scenarios",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify(requestData)
            }
        );

        // 오류 응답 처리
        if (!response.ok) {

            const errorText =
                await response.text();

            throw new Error(
                errorText ||
                `HTTP 오류 ${response.status}`
            );
        }

        // JSON 변환
        const scenarios =
            await response.json();

        // 응답 검증
        if (!Array.isArray(scenarios)
            || scenarios.length === 0) {

            throw new Error(
                "분석 결과가 없습니다."
            );
        }

        // 결과 정렬
        const sortedScenarios =
            sortScenarios(scenarios);

        // 결과 출력
        renderResult(
            sortedScenarios,
            requestData
        );

    } catch (error) {

        console.error(
            "대출 시나리오 분석 실패:",
            error
        );

        // 오류 표시
        showError(
            parseErrorMessage(error)
        );

    } finally {

        // 분석상태 종료
        setLoadingState(false);
    }
}


// 요청값 생성
function createRequestData() {

    return {
        userId:
            getNumberValue("userId"),

        targetLoanAccountId:
            getNumberValue(
                "targetLoanAccountId"
            ),

        desiredRepaymentAmount:
            getNumberValue(
                "desiredRepaymentAmount"
            ),

        emergencyFundAmount:
            getNumberValue(
                "emergencyFundAmount"
            ),

        refinanceInterestRate:
            getNumberValue(
                "refinanceInterestRate"
            ),

        refinanceCostAmount:
            getNumberValue(
                "refinanceCostAmount"
            ),

        refinancePeriodMonths:
            getNumberValue(
                "refinancePeriodMonths"
            )
    };
}


// 숫자 입력값 조회
function getNumberValue(elementId) {

    const value =
        document
            .getElementById(elementId)
            .value;

    // 빈 문자열 처리
    if (value === "") {
        return null;
    }

    const number = Number(value);

    return Number.isFinite(number)
        ? number
        : null;
}


// 요청값 검증
function validateRequest(requestData) {

    // 회원번호 확인
    if (!requestData.userId
        || requestData.userId <= 0) {

        showError(
            "회원번호를 확인해주세요."
        );

        return false;
    }

    // 대출번호 확인
    if (!requestData.targetLoanAccountId
        || requestData.targetLoanAccountId <= 0) {

        showError(
            "분석할 대출을 선택해주세요."
        );

        return false;
    }

    // 금액 음수 확인
    const amountFields = [
        requestData.desiredRepaymentAmount,
        requestData.emergencyFundAmount,
        requestData.refinanceCostAmount
    ];

    if (amountFields.some(
        value => value !== null && value < 0
    )) {

        showError(
            "금액은 0원 이상이어야 합니다."
        );

        return false;
    }

    // 대환금리 확인
    if (requestData.refinanceInterestRate !== null
        && requestData.refinanceInterestRate < 0) {

        showError(
            "대환 예상금리를 확인해주세요."
        );

        return false;
    }

    // 대환기간 확인
    if (requestData.refinancePeriodMonths !== null
        && requestData.refinancePeriodMonths <= 0) {

        showError(
            "대환 상환기간은 1개월 이상이어야 합니다."
        );

        return false;
    }

    return true;
}


// 결과 순서 정렬
function sortScenarios(scenarios) {

    return [...scenarios].sort(
        (first, second) => {

            return SCENARIO_ORDER.indexOf(
                first.scenarioType
            ) - SCENARIO_ORDER.indexOf(
                second.scenarioType
            );
        }
    );
}


// 전체 결과 출력
function renderResult(
    scenarios,
    requestData
) {

    // 최고 추천 시나리오
    const recommendedScenario =
        findRecommendedScenario(scenarios);

    // 분석대상 표시
    renderAnalysisTarget(requestData);

    // 추천 결과 표시
    renderRecommendation(
        recommendedScenario
    );

    // 비교표 표시
    renderComparisonTable(
        scenarios,
        recommendedScenario
    );

    // 상세 의견 표시
    renderScenarioReasons(scenarios);

    // 추천 열 강조
    highlightRecommendedColumn(
        recommendedScenario.scenarioType
    );

    // 결과영역 표시
    document
        .getElementById("resultArea")
        .classList.remove("hidden");

    // 결과 위치 이동
    document
        .getElementById("resultArea")
        .scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
}


// 최고 추천 시나리오 조회
function findRecommendedScenario(
    scenarios
) {

    return scenarios.reduce(
        (best, current) => {

            const bestScore =
                Number(
                    best.recommendationScore ?? 0
                );

            const currentScore =
                Number(
                    current.recommendationScore ?? 0
                );

            return currentScore > bestScore
                ? current
                : best;
        }
    );
}


// 분석대상 표시
function renderAnalysisTarget(requestData) {

    const select =
        document.getElementById(
            "targetLoanAccountId"
        );

    const selectedText =
        select.options[
            select.selectedIndex
            ]?.text ?? "";

    document
        .getElementById(
            "analysisTargetText"
        )
        .textContent =
        `분석대상: ${selectedText}`;
}


// 추천 결과 표시
function renderRecommendation(scenario) {

    document
        .getElementById(
            "recommendedScenarioName"
        )
        .textContent =
        scenario.scenarioName
        ?? SCENARIO_NAMES[
            scenario.scenarioType
            ]
        ?? "-";

    document
        .getElementById(
            "recommendedReason"
        )
        .textContent =
        scenario.recommendationReason
        ?? "추천 설명이 없습니다.";

    document
        .getElementById(
            "recommendedScore"
        )
        .textContent =
        `${formatScore(
            scenario.recommendationScore
        )}점`;
}


// 비교표 출력
function renderComparisonTable(
    scenarios,
    recommendedScenario
) {

    const tableBody =
        document.getElementById(
            "comparisonTableBody"
        );

    // 비교항목 정의
    const rows = [
        {
            label: "변경 전 대출잔액",
            field: "beforeBalance",
            format: formatCurrency
        },
        {
            label: "변경 후 대출잔액",
            field: "afterBalance",
            format: formatCurrency,
            lowerIsBetter: true
        },
        {
            label: "변경 전 금리",
            field: "beforeInterestRate",
            format: formatRate
        },
        {
            label: "변경 후 금리",
            field: "afterInterestRate",
            format: formatRate,
            lowerIsBetter: true
        },
        {
            label: "변경 전 월 상환액",
            field: "beforeMonthlyPayment",
            format: formatCurrency
        },
        {
            label: "변경 후 월 상환액",
            field: "afterMonthlyPayment",
            format: formatCurrency,
            lowerIsBetter: true
        },
        {
            label: "부분상환 금액",
            field: "repaymentAmount",
            format: formatCurrency
        },
        {
            label: "중도상환수수료",
            field: "prepaymentFeeAmount",
            format: formatCurrency,
            lowerIsBetter: true
        },
        {
            label: "대환 부대비용",
            field: "refinanceCostAmount",
            format: formatCurrency,
            lowerIsBetter: true
        },
        {
            label: "예상 이자 절감액",
            field: "estimatedInterestSaving",
            format: formatSignedCurrency,
            higherIsBetter: true
        },
        {
            label: "비용 차감 후 순효과",
            field: "netBenefitAmount",
            format: formatSignedCurrency,
            higherIsBetter: true,
            signedValue: true
        },
        {
            label: "실행 후 남는 현금",
            field: "remainingCashAmount",
            format: formatCurrency,
            higherIsBetter: true
        },
        {
            label: "현금 유지 가능기간",
            field: "liquidityMonths",
            format: formatMonths,
            higherIsBetter: true
        },
        {
            label: "추천점수",
            field: "recommendationScore",
            format: formatScoreWithUnit,
            higherIsBetter: true
        }
    ];

    tableBody.innerHTML =
        rows.map(row => {

            const bestIndexes =
                findBestValueIndexes(
                    scenarios,
                    row
                );

            const cells =
                scenarios.map(
                    (scenario, index) => {

                        const value =
                            scenario[row.field];

                        const classes = [];

                        // 추천 열 강조
                        if (
                            scenario.scenarioType
                            === recommendedScenario.scenarioType
                        ) {
                            classes.push(
                                "recommended-column"
                            );
                        }

                        // 유리한 값 강조
                        if (
                            bestIndexes.includes(index)
                            && value !== null
                            && value !== undefined
                        ) {
                            classes.push(
                                "best-value"
                            );
                        }

                        // 순효과 색상
                        if (row.signedValue) {

                            const number =
                                Number(value ?? 0);

                            if (number > 0) {
                                classes.push(
                                    "value-positive"
                                );
                            }

                            if (number < 0) {
                                classes.push(
                                    "value-negative"
                                );
                            }
                        }

                        return `
                            <td class="${classes.join(" ")}">
                                ${row.format(value)}
                            </td>
                        `;
                    }
                )
                    .join("");

            return `
                <tr>
                    <th scope="row">
                        ${escapeHtml(row.label)}
                    </th>

                    ${cells}
                </tr>
            `;
        })
            .join("");
}


// 최적값 위치 조회
function findBestValueIndexes(
    scenarios,
    row
) {

    // 비교기준 없음
    if (!row.higherIsBetter
        && !row.lowerIsBetter) {

        return [];
    }

    const values =
        scenarios.map(
            scenario =>
                Number(
                    scenario[row.field] ?? 0
                )
        );

    // 최댓값 조회
    const bestValue =
        row.higherIsBetter
            ? Math.max(...values)
            : Math.min(...values);

    return values
        .map(
            (value, index) =>
                value === bestValue
                    ? index
                    : -1
        )
        .filter(index => index >= 0);
}


// 시나리오별 상세 의견 출력
function renderScenarioReasons(scenarios) {

    const reasonList =
        document.getElementById(
            "scenarioReasonList"
        );

    reasonList.innerHTML =
        scenarios.map(scenario => {

            const scenarioName =
                scenario.scenarioName
                ?? SCENARIO_NAMES[
                    scenario.scenarioType
                    ]
                ?? scenario.scenarioType;

            return `
                <div class="reason-item">

                    <div class="reason-title">
                        ${escapeHtml(
                scenarioName
            )}
                    </div>

                    <div class="reason-content">
                        ${escapeHtml(
                scenario.recommendationReason
                ?? "-"
            )}
                    </div>

                    <div class="reason-score">
                        ${formatScore(
                scenario.recommendationScore
            )}점
                    </div>

                </div>
            `;
        })
            .join("");
}


// 추천 열 제목 강조
function highlightRecommendedColumn(
    scenarioType
) {

    document
        .querySelectorAll(
            ".result-table thead th"
        )
        .forEach(header => {

            header.classList.remove(
                "recommended-column"
            );

            if (
                header.dataset.scenario
                === scenarioType
            ) {
                header.classList.add(
                    "recommended-column"
                );
            }
        });
}


// 분석상태 설정
function setLoadingState(isLoading) {

    const loadingArea =
        document.getElementById(
            "loadingArea"
        );

    const submitButton =
        document.getElementById(
            "submitButton"
        );

    if (isLoading) {

        loadingArea.classList.remove(
            "hidden"
        );

        submitButton.disabled = true;

        submitButton.textContent =
            "분석 중";

    } else {

        loadingArea.classList.add(
            "hidden"
        );

        submitButton.disabled = false;

        submitButton.textContent =
            "대응방안 비교하기";
    }
}


// 결과 숨김
function hideResult() {

    document
        .getElementById("resultArea")
        .classList.add("hidden");
}


// 오류 표시
function showError(message) {

    document
        .getElementById(
            "errorMessage"
        )
        .textContent = message;

    document
        .getElementById(
            "errorArea"
        )
        .classList.remove("hidden");
}


// 오류 숨김
function hideError() {

    document
        .getElementById(
            "errorArea"
        )
        .classList.add("hidden");
}


// 오류메시지 변환
function parseErrorMessage(error) {

    const message =
        error?.message
        ?? "분석 중 오류가 발생했습니다.";

    // 긴 서버 오류 축약
    if (message.length > 200) {

        return "서버에서 분석을 처리하지 못했습니다. "
            + "입력값과 서버 로그를 확인해주세요.";
    }

    return message;
}


// 입력 초기화
function resetForm() {

    document
        .getElementById(
            "scenarioForm"
        )
        .reset();

    // 기본값 재설정
    document.getElementById(
        "userId"
    ).value = 1;

    document.getElementById(
        "targetLoanAccountId"
    ).value = 1;

    document.getElementById(
        "desiredRepaymentAmount"
    ).value = 5000000;

    document.getElementById(
        "emergencyFundAmount"
    ).value = 8000000;

    document.getElementById(
        "refinanceInterestRate"
    ).value = 5.2;

    document.getElementById(
        "refinanceCostAmount"
    ).value = 300000;

    document.getElementById(
        "refinancePeriodMonths"
    ).value = 36;

    // 결과와 오류 숨김
    hideResult();
    hideError();

    // 상단 이동
    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}


// 원화 표시
function formatCurrency(value) {

    const number =
        Number(value ?? 0);

    return `${new Intl.NumberFormat(
        "ko-KR",
        {
            maximumFractionDigits: 0
        }
    ).format(number)}원`;
}


// 부호 포함 원화 표시
function formatSignedCurrency(value) {

    const number =
        Number(value ?? 0);

    const formatted =
        new Intl.NumberFormat(
            "ko-KR",
            {
                maximumFractionDigits: 0
            }
        ).format(
            Math.abs(number)
        );

    if (number > 0) {
        return `+${formatted}원`;
    }

    if (number < 0) {
        return `-${formatted}원`;
    }

    return "0원";
}


// 금리 표시
function formatRate(value) {

    const number =
        Number(value ?? 0);

    return `${number.toFixed(2)}%`;
}


// 개월 표시
function formatMonths(value) {

    const number =
        Number(value ?? 0);

    return `${number.toFixed(2)}개월`;
}


// 점수 표시
function formatScore(value) {

    const number =
        Number(value ?? 0);

    return Number.isInteger(number)
        ? number.toString()
        : number.toFixed(2);
}


// 단위 포함 점수
function formatScoreWithUnit(value) {

    return `${formatScore(value)}점`;
}


// HTML 특수문자 처리
function escapeHtml(value) {

    const text =
        String(value ?? "-");

    return text
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}