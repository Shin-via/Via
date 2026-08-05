// ?? ??
document.addEventListener(
    "DOMContentLoaded",
    loadDebtPriorities
);


// ?? ???? ??
async function loadDebtPriorities() {

    // ??? ?????
    const userId = 1;

    // ???? ??
    const loadingArea =
        document.getElementById("loadingArea");

    const errorArea =
        document.getElementById("errorArea");

    const priorityList =
        document.getElementById("priorityList");

    try {

        // ???? API ??
        const response = await fetch(
            `/api/loan-analysis/debt-priority/${userId}`,
            {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                }
            }
        );

        // API ?? ??
        if (!response.ok) {
            throw new Error(
                `HTTP ??: ${response.status}`
            );
        }

        // JSON ??
        const priorities =
            await response.json();

        // ?? ??
        loadingArea.classList.add("hidden");

        // ?? ?? ??
        if (!Array.isArray(priorities)
            || priorities.length === 0) {

            priorityList.innerHTML = `
                <div class="loading-area">
                    ??? ????? ????.
                </div>
            `;

            updateSummary([]);

            return;
        }

        // ???? ??
        updateSummary(priorities);

        // ???? ??
        renderPriorityCards(priorities);

    } catch (error) {

        // ?? ??
        console.error(
            "?? ???? ?? ??:",
            error
        );

        // ?? ??
        loadingArea.classList.add("hidden");

        // ?? ??
        errorArea.classList.remove("hidden");
    }
}


// ???? ??
function updateSummary(priorities) {

    const loanCount =
        document.getElementById("loanCount");

    const topLoanType =
        document.getElementById("topLoanType");

    const topPriorityScore =
        document.getElementById(
            "topPriorityScore"
        );

    // ?? ??
    if (priorities.length === 0) {

        loanCount.textContent = "0?";
        topLoanType.textContent = "-";
        topPriorityScore.textContent = "-";

        return;
    }

    // 1?? ??
    const firstLoan = priorities[0];

    loanCount.textContent =
        `${priorities.length}?`;

    topLoanType.textContent =
        firstLoan.loanType ?? "-";

    topPriorityScore.textContent =
        formatScore(firstLoan.priorityScore);
}


// ?? ???? ??
function renderPriorityCards(priorities) {

    const priorityList =
        document.getElementById("priorityList");

    priorityList.innerHTML =
        priorities
            .map(createPriorityCard)
            .join("");
}


// ???? HTML ??
function createPriorityCard(loan) {

    // 1?? ???
    const firstClass =
        loan.priorityRank === 1
            ? "first"
            : "";

    return `
        <article class="priority-card ${firstClass}">

            <div class="rank-box">
                ${loan.priorityRank}?
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
                        <span>?? ??</span>
                        <strong>
                            ${formatCurrency(
        loan.currentBalance
    )}
                        </strong>
                    </div>

                    <div class="loan-info-item">
                        <span>?? ??</span>
                        <strong>
                            ${formatRate(
        loan.interestRate
    )}
                        </strong>
                    </div>

                    <div class="loan-info-item">
                        <span>?? ??</span>
                        <strong>
                            ${escapeHtml(
        loan.rateType
    )}
                        </strong>
                    </div>

                    <div class="loan-info-item">
                        <span>?? ??</span>
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


// ?? ??
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


// ?? ??
function formatRate(value) {

    const number =
        Number(value ?? 0);

    return `${number.toFixed(2)}%`;
}


// ?? ??
function formatScore(value) {

    const number =
        Number(value ?? 0);

    return number.toFixed(2);
}


// HTML ???? ??
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