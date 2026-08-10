document.addEventListener("DOMContentLoaded", () => {
    const loanType = document.getElementById("loanType");
    const mortgageFields =
        document.getElementById("mortgageFields");
    const interestRateFields =
        document.getElementById("interestRateFields");
    const jeonseFields =
        document.getElementById("jeonseFields");

    function updateConditionalFields() {
        const selectedLoanType = loanType.value;

        mortgageFields.hidden =
            selectedLoanType !== "MORTGAGE_LOAN";

        interestRateFields.hidden =
            selectedLoanType !== "MORTGAGE_LOAN"
            && selectedLoanType !== "CREDIT_LOAN";

        jeonseFields.hidden =
            selectedLoanType !== "JEONSE_LOAN";
    }

    loanType.addEventListener(
        "change",
        updateConditionalFields
    );

    updateConditionalFields();
});