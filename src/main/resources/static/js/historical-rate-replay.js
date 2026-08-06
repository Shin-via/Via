const form = document.querySelector('#replayForm');
const submitButton = document.querySelector('#submitButton');
const errorMessage = document.querySelector('#errorMessage');
const resultArea = document.querySelector('#resultArea');
const resultTruncated = document.querySelector('#resultTruncated');
const resultStatusBadge = document.querySelector('#resultStatusBadge');
const resultTableBody = document.querySelector('#resultTableBody');
const resultDisclaimer = document.querySelector('#resultDisclaimer');

let monthlyPaymentChart = null;

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    errorMessage.classList.add('hidden');
    resultArea.classList.add('hidden');
    submitButton.disabled = true;

    const loanId = document.querySelector('#loanId').value;
    const startDate = document.querySelector('#startDate').value;
    const endDate = document.querySelector('#endDate').value;

    try {
        const data = await fetchHistoricalRateReplay(loanId, startDate, endDate);
        renderResult(data);
    } catch (error) {
        errorMessage.textContent = error.message;
        errorMessage.classList.remove('hidden');
    } finally {
        submitButton.disabled = false;
    }
});

async function fetchHistoricalRateReplay(loanId, startDate, endDate) {
    const params = new URLSearchParams({startDate, endDate});
    const response = await fetch(`/api/loans/${loanId}/historical-rate-replay?${params}`);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || `요청 처리 중 오류가 발생했습니다. (HTTP ${response.status})`);
    }

    return data;
}

function renderResult(data) {
    document.querySelector('#resultLoanType').textContent = data.loanType;
    document.querySelector('#resultRateType').textContent = data.rateType;
    document.querySelector('#resultInitialRate').textContent = data.initialRate;
    document.querySelector('#resultInitialMonthlyPayment').textContent = formatNumber(data.initialMonthlyPayment);
    document.querySelector('#resultChangePointCount').textContent = data.changePointCount;
    document.querySelector('#resultTotalSegmentInterest').textContent = formatNumber(data.totalSegmentInterest);
    resultDisclaimer.textContent = data.disclaimer;

    resultTruncated.classList.toggle('hidden', !data.truncated);
    setBadge(data.truncated ? 'badge-warning' : 'badge-good', data.truncated ? '만기로 일부만 계산됨' : '계산 완료');

    renderTable(data.path);
    renderChart(data.path);

    resultArea.classList.remove('hidden');
}

function setBadge(className, label) {
    resultStatusBadge.className = `badge ${className}`;
    resultStatusBadge.textContent = label;
}

function renderTable(path) {
    resultTableBody.innerHTML = '';

    for (const step of path) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${step.monthOffset}</td>
            <td>${step.appliedRate}%</td>
            <td>${formatNumber(step.remainingBalance)}원</td>
            <td>${formatNumber(step.monthlyPayment)}원</td>
            <td>${step.segmentInterest === null ? '-' : formatNumber(step.segmentInterest) + '원'}</td>
        `;
        resultTableBody.appendChild(row);
    }
}

function renderChart(path) {
    const ctx = document.querySelector('#monthlyPaymentChart');

    if (monthlyPaymentChart) {
        monthlyPaymentChart.destroy();
    }

    monthlyPaymentChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: path.map((step) => `${step.monthOffset}개월`),
            datasets: [{
                label: '월상환액',
                data: path.map((step) => step.monthlyPayment),
                stepped: 'before',
                borderColor: '#2a78d6',
                backgroundColor: 'rgba(42, 120, 214, 0.12)',
                pointBackgroundColor: '#2a78d6',
                pointRadius: 3,
                borderWidth: 2,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {
                        color: '#52514e',
                        font: {family: 'system-ui'}
                    }
                }
            },
            scales: {
                x: {
                    grid: {color: '#e1e0d9'},
                    ticks: {color: '#898781', maxRotation: 0, autoSkip: true}
                },
                y: {
                    beginAtZero: false,
                    grid: {color: '#e1e0d9'},
                    ticks: {color: '#898781'}
                }
            }
        }
    });
}

function formatNumber(value) {
    return new Intl.NumberFormat('ko-KR').format(value);
}
