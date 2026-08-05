// 화면 실행
document.addEventListener(
    "DOMContentLoaded",
    loadDebtPriorities
);


// 부채 상환순위 조회
async function loadDebtPriorities() {

    // 테스트 사용자번호
    const userId = 1;

    // 화면요소 조회
    const loadingArea =
        document.getElementById("loadingArea");

    const errorArea =
        document.getElementById("errorArea");

    const priorityList =
        document.getElementById("priorityList");

    try {

        // 부채순위 API 요청
        const response = await fetch(
            `/api/loan-analysis/debt-priority/${userId}`,
            {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                }
            }
        );

        // API 오류 처리
        if (!response.ok) {
            throw new Error(
                `HTTP 오류: ${response.status}`
            );
        }

        // JSON 변환
        const priorities =
            await response.json();

        // 로딩 숨김
        loadingArea.classList.add("hidden");

        // 대출 없음 처리
        if (!Array.isArray(priorities)
            || priorities.length === 0) {

            priorityList.innerHTML = `
                <div class="loading-area">
                    분석할 대출정보가 없습니다.
                </div>
            `;

            updateSummary([]);

            return;
        }

        // 요약정보 출력
        updateSummary(priorities);

        // 순위카드 출력
        renderPriorityCards(priorities);

    } catch (error) {

        // 오류 로그
        console.error(
            "부채 상환순위 조회 실패:",
            error
        );

        // 로딩 숨김
        loadingArea.classList.add("hidden");

        // 오류 표시
        errorArea.classList.remove("hidden");
    }
}


// 요약정보 출력
function updateSummary(priorities) {

    const loanCount =
        document.getElementById("loanCount");

    const topLoanType =
        document.getElementById("topLoanType");

    const topPriorityScore =
        document.getElementById(
            "topPriorityScore"
        );

    // 대출 없음
    if (priorities.length === 0) {

        loanCount.textContent = "0건";
        topLoanType.textContent = "-";
        topPriorityScore.textContent = "-";

        return;
    }

    // 1순위 대출
    const firstLoan = priorities[0];

    loanCount.textContent =
        `${priorities.length}건`;

    topLoanType.textContent =
        firstLoan.loanType ?? "-";

    topPriorityScore.textContent =
        formatScore(firstLoan.priorityScore);
}


// 대출 순위카드 출력
function renderPriorityCards(priorities) {

    const priorityList =
        document.getElementById("priorityList");

    priorityList.innerHTML =
        priorities
            .map(createPriorityCard)
            .join("");
}


// 대출카드 HTML 생성
function createPriorityCard(loan) {

    // 1순위 스타일
    const firstClass =
        loan.priorityRank === 1
            ? "first"
            : "";

    return `
        <article class="priority-card ${firstClass}">

            <div class="rank-box">
                ${loan.priorityRank}위
            </div>

            <div>

                <div class="loan-title-row">

                    <h2>
                        ${escapeHtml(loan.loanType)}
                    </h2>

                    <span class="priority-score">
                        RPS ${formatScore(
        loan.priorityScore
    )}
                    </span>

                </div>

                <div class="loan-info-grid">

                    <div class="loan-info-item">
                        <span>현재 잔액</span>
                        <strong>
                            ${formatCurrency(
        loan.currentBalance
    )}
                        </strong>
                    </div>

                    <div class="loan-info-item">
                        <span>적용 금리</span>
                        <strong>
                            ${formatRate(
        loan.interestRate
    )}
                        </strong>
                    </div>

                    <div class="loan-info-item">
                        <span>금리 유형</span>
                        <strong>
                            ${escapeHtml(
        loan.rateType
    )}
                        </strong>
                    </div>

                    <div class="loan-info-item">
                        <span>대출 상태</span>
                        <strong>
                            ${escapeHtml(
        loan.loanStatus
    )}
                        </strong>
                    </div>

                </div>

                <div class="reason-box">
                    ${escapeHtml(loan.reason)}
                </div>

            </div>

        </article>
    `;
}


// 원화 표시
function formatCurrency(value) {

    const number =
        Number(value ?? 0);

    return new Intl.NumberFormat(
        "ko-KR",
        {
            style: "currency",
            currency: "KRW",
            maximumFractionDigits: 0
        }
    ).format(number);
}


// 금리 표시
function formatRate(value) {

    const number =
        Number(value ?? 0);

    return `${number.toFixed(2)}%`;
}


// 점수 표시
function formatScore(value) {

    const number =
        Number(value ?? 0);

    return number.toFixed(2);
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