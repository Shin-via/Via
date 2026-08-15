const form = document.getElementById('preferenceForm');
const submitButton = document.getElementById('submitButton');
const errorBox = document.getElementById('errorBox');
const resultSection = document.getElementById('resultSection');
const operationAmountInput = document.getElementById('operationAmount');

const styleLabels = {
    STABLE: '안정형',
    BALANCED: '균형형',
    AGGRESSIVE: '공격형'
};

const assetLabels = {
    CASH: '현금',
    ETF: 'ETF',
    FUND: '펀드'
};

const assetColors = {
    CASH: '#1b2430',
    ETF: '#5271c4',
    FUND: '#a8b6df'
};

const assetOrder = ['CASH', 'ETF', 'FUND'];

const wonFormatter = new Intl.NumberFormat('ko-KR', {
    maximumFractionDigits: 0
});

window.applySurplusFundAmount = (amount) => {
    const normalizedAmount = Number(amount);

    if (!Number.isFinite(normalizedAmount) || normalizedAmount <= 0) {
        return false;
    }

    operationAmountInput.value = String(normalizedAmount);
    operationAmountInput.dispatchEvent(new Event('input', { bubbles: true }));
    operationAmountInput.dispatchEvent(new Event('change', { bubbles: true }));
    return true;
};

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideError();

    if (!form.reportValidity()) {
        return;
    }

    const formData = new FormData(form);
    const operationAmount = Number(formData.get('operationAmount'));

    if (!Number.isFinite(operationAmount) || operationAmount <= 0) {
        showError('운용 가능 금액은 0보다 커야 합니다.');
        return;
    }

    const requestBody = {
        operationAmount,
        investmentPurpose: formData.get('investmentPurpose'),
        investmentPeriodMonths: Number(formData.get('investmentPeriodMonths')),
        lossToleranceLevel: formData.get('lossToleranceLevel'),
        liquidityNeed: formData.get('liquidityNeed'),
        experienceLevel: formData.get('experienceLevel'),
        surplusAmountConfirmed: document.getElementById('surplusAmountConfirmed').checked,
        guideNoticeConfirmed: document.getElementById('guideNoticeConfirmed').checked
    };

    setLoading(true);

    try {
        const response = await fetch('/api/surplus-funds/preferences/analyze', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'same-origin',
            body: JSON.stringify(requestBody)
        });

        const contentType = response.headers.get('content-type') || '';

        if (!response.ok) {
            const message = contentType.includes('application/json')
                ? await readErrorMessage(response)
                : await response.text();

            throw new Error(message || `요청 처리에 실패했습니다. (${response.status})`);
        }

        if (!contentType.includes('application/json')) {
            throw new Error('로그인 세션을 확인해주세요.');
        }

        const result = await response.json();
        renderResult(result);
    } catch (error) {
        showError(error.message || '운용성향 분석 중 오류가 발생했습니다.');
    } finally {
        setLoading(false);
    }
});

async function readErrorMessage(response) {
    const errorBody = await response.json();
    return errorBody.message || errorBody.detail || errorBody.error;
}

function renderResult(result) {
    document.getElementById('styleBadge').textContent =
        `${styleLabels[result.investmentStyle] || result.investmentStyle}`;
    document.getElementById('scoreValue').textContent = result.score;
    document.getElementById('ruleVersion').textContent = result.ruleVersion;
    document.getElementById('planId').textContent = `#${result.surplusFundPlanId}`;
    document.getElementById('guideNotice').textContent = result.guideNotice;

    renderAllocations(result.allocations || []);
    renderReasons(result.reasons || []);

    resultSection.hidden = false;
    resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function renderAllocations(allocations) {
    const allocationGrid = document.getElementById('allocationGrid');
    allocationGrid.replaceChildren();

    const normalizedAllocations = allocations
        .map((allocation) => ({
            ...allocation,
            ratio: Number(allocation.ratio),
            amount: Number(allocation.amount)
        }))
        .filter((allocation) =>
            assetOrder.includes(allocation.assetType)
            && Number.isFinite(allocation.ratio)
            && Number.isFinite(allocation.amount)
        )
        .sort((left, right) =>
            assetOrder.indexOf(left.assetType) - assetOrder.indexOf(right.assetType)
        );

    const totalAmount = normalizedAllocations.reduce(
        (sum, allocation) => sum + allocation.amount,
        0
    );

    let accumulatedRatio = 0;
    const gradientSegments = normalizedAllocations.map((allocation) => {
        const startRatio = accumulatedRatio;
        accumulatedRatio += Math.max(allocation.ratio, 0);
        const color = assetColors[allocation.assetType];
        return `${color} ${startRatio}% ${accumulatedRatio}%`;
    });

    const donut = document.createElement('div');
    donut.className = 'allocation-donut';
    donut.style.setProperty(
        '--allocation-gradient',
        `conic-gradient(${gradientSegments.join(', ')})`
    );
    donut.setAttribute('role', 'img');
    donut.setAttribute(
        'aria-label',
        normalizedAllocations
            .map((allocation) =>
                `${assetLabels[allocation.assetType]} ${allocation.ratio}%, ${wonFormatter.format(allocation.amount)}원`
            )
            .join(', ')
    );

    const donutCenter = document.createElement('div');
    donutCenter.className = 'allocation-donut-center';

    const totalLabel = document.createElement('span');
    totalLabel.textContent = '총 운용금액';

    const totalValue = document.createElement('strong');
    totalValue.textContent = `${wonFormatter.format(totalAmount)}원`;

    donutCenter.append(totalLabel, totalValue);
    donut.append(donutCenter);

    const legend = document.createElement('div');
    legend.className = 'allocation-legend';

    normalizedAllocations.forEach((allocation) => {
        const item = document.createElement('article');
        item.className = 'allocation-legend-item';

        const colorMarker = document.createElement('span');
        colorMarker.className = 'allocation-color-marker';
        colorMarker.style.setProperty('--asset-color', assetColors[allocation.assetType]);

        const copy = document.createElement('div');
        const name = document.createElement('strong');
        name.textContent = `${assetLabels[allocation.assetType]} (${allocation.assetType})`;
        const ratio = document.createElement('span');
        ratio.textContent = `${allocation.ratio}%`;
        copy.append(name, ratio);

        const amount = document.createElement('b');
        amount.textContent = `${wonFormatter.format(allocation.amount)}원`;

        item.append(colorMarker, copy, amount);
        legend.append(item);
    });

    allocationGrid.append(donut, legend);
}

function renderReasons(reasons) {
    const reasonList = document.getElementById('reasonList');
    reasonList.replaceChildren();

    reasons.forEach((reason) => {
        const item = document.createElement('li');
        item.textContent = reason;
        reasonList.append(item);
    });
}

function setLoading(loading) {
    submitButton.disabled = loading;
    submitButton.textContent = loading
        ? '분석하고 있습니다...'
        : '운용 가이드 확인하기';
}

function showError(message) {
    errorBox.textContent = message;
    errorBox.hidden = false;
    errorBox.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function hideError() {
    errorBox.hidden = true;
    errorBox.textContent = '';
}
