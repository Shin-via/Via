const form = document.getElementById('preferenceForm');
const submitButton = document.getElementById('submitButton');
const errorBox = document.getElementById('errorBox');
const resultSection = document.getElementById('resultSection');
const operationAmountInput = document.getElementById('operationAmount');
const stepTabs = Array.from(document.querySelectorAll('[data-step-target]'));
const stepPanels = Array.from(document.querySelectorAll('[data-step-panel]'));
const questionCards = Array.from(document.querySelectorAll('[data-question-index]'));
const productFilterButtons = Array.from(document.querySelectorAll('[data-product-filter]'));

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
const questionRadioNames = [
    'investmentPurpose',
    'investmentPeriodMonths',
    'lossToleranceLevel',
    'liquidityNeed',
    'experienceLevel'
];

const productEmptyMessages = {
    DEFAULT: 'ETF·펀드 상품 데이터를 준비하고 있습니다. 데이터 연동 후 자산배분 결과에 맞는 관련 상품을 확인할 수 있습니다.',
    CASH: '현금 배정분은 유동성 확보를 위한 보유 금액이며, 현재 상품 탐색 대상에서 제외됩니다.'
};

const wonFormatter = new Intl.NumberFormat('ko-KR', {
    maximumFractionDigits: 0
});

let currentStep = 1;
let currentQuestionIndex = 0;
let hasAnalysisResult = false;
let latestAllocations = [];
let currentProductFilter = 'ALL';
let isSubmitting = false;

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

document.getElementById('startSurveyButton').addEventListener('click', () => {
    if (!validateOperationAmount()) {
        return;
    }

    hideError();
    showQuestion(0, false);
    showStep(2);
});

stepTabs.forEach((tab) => {
    tab.addEventListener('click', () => {
        const targetStep = Number(tab.dataset.stepTarget);

        if ((targetStep === 3 || targetStep === 4) && !hasAnalysisResult) {
            return;
        }

        if (targetStep === 2 && !validateOperationAmount()) {
            return;
        }

        hideError();
        showStep(targetStep);
    });
});

questionCards.forEach((card) => {
    card.querySelectorAll('[data-question-action]').forEach((button) => {
        button.addEventListener('click', () => {
            const action = button.dataset.questionAction;

            if (action === 'previous') {
                hideError();
                if (currentQuestionIndex === 0) {
                    showStep(1);
                    return;
                }
                showQuestion(currentQuestionIndex - 1);
                return;
            }

            if (!validateQuestion(currentQuestionIndex)) {
                return;
            }

            hideError();
            showQuestion(currentQuestionIndex + 1);
        });
    });
});

document.getElementById('reviewSurveyButton').addEventListener('click', () => {
    hideError();
    showQuestion(0, false);
    showStep(2);
});

document.getElementById('exploreProductsButton').addEventListener('click', () => {
    if (hasAnalysisResult) {
        hideError();
        showStep(4);
    }
});

document.getElementById('backToResultButton').addEventListener('click', () => {
    if (hasAnalysisResult) {
        hideError();
        showStep(3);
    }
});

productFilterButtons.forEach((button) => {
    button.addEventListener('click', () => {
        currentProductFilter = button.dataset.productFilter;
        updateProductExplorer();
    });
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    if (isSubmitting) {
        return;
    }

    hideError();

    if (!validateSurveyForSubmit()) {
        return;
    }

    const formData = new FormData(form);
    const operationAmount = Number(formData.get('operationAmount'));
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
        showQuestion(questionCards.length - 1, false);
        showStep(2, false);
        showError(error.message || '운용성향 분석 중 오류가 발생했습니다.');
    } finally {
        setLoading(false);
    }
});

function showStep(step, shouldScroll = true) {
    currentStep = step;

    stepPanels.forEach((panel) => {
        panel.hidden = Number(panel.dataset.stepPanel) !== step;
    });

    stepTabs.forEach((tab) => {
        const tabStep = Number(tab.dataset.stepTarget);
        const isActive = tabStep === step;
        const isLocked = tabStep >= 3 && !hasAnalysisResult;
        const isCompleted = tabStep < step || (hasAnalysisResult && tabStep <= 2);

        tab.classList.toggle('active', isActive);
        tab.classList.toggle('completed', !isActive && isCompleted);
        tab.disabled = isLocked;
        tab.setAttribute('aria-disabled', String(isLocked));

        if (isActive) {
            tab.setAttribute('aria-current', 'step');
        } else {
            tab.removeAttribute('aria-current');
        }
    });

    if (shouldScroll) {
        document.querySelector('.guide-steps').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

function showQuestion(index, shouldScroll = true) {
    const safeIndex = Math.min(Math.max(index, 0), questionCards.length - 1);
    currentQuestionIndex = safeIndex;

    questionCards.forEach((card, cardIndex) => {
        card.hidden = cardIndex !== safeIndex;
    });

    if (shouldScroll) {
        questionCards[safeIndex].scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}

function validateOperationAmount() {
    const operationAmount = Number(operationAmountInput.value);

    if (!Number.isFinite(operationAmount) || operationAmount <= 0) {
        showStep(1, false);
        showError('운용 가능 금액은 0보다 커야 합니다.');
        operationAmountInput.focus();
        return false;
    }

    return true;
}

function validateQuestion(index) {
    const radioName = questionRadioNames[index];
    const selectedOption = form.querySelector(`input[name="${radioName}"]:checked`);

    if (!selectedOption) {
        showError('현재 질문의 답변을 선택해주세요.');
        questionCards[index].querySelector(`input[name="${radioName}"]`).focus();
        return false;
    }

    return true;
}

function validateSurveyForSubmit() {
    if (!validateOperationAmount()) {
        return false;
    }

    for (let index = 0; index < questionRadioNames.length; index += 1) {
        if (!form.querySelector(`input[name="${questionRadioNames[index]}"]:checked`)) {
            showStep(2, false);
            showQuestion(index, false);
            showError(`${index + 1}번 질문의 답변을 선택해주세요.`);
            questionCards[index].querySelector(`input[name="${questionRadioNames[index]}"]`).focus();
            return false;
        }
    }

    const surplusAmountConfirmed = document.getElementById('surplusAmountConfirmed');
    const guideNoticeConfirmed = document.getElementById('guideNoticeConfirmed');

    if (!surplusAmountConfirmed.checked || !guideNoticeConfirmed.checked) {
        showStep(2, false);
        showQuestion(questionCards.length - 1, false);
        showError('필수 확인 항목 두 가지에 모두 동의해주세요.');
        (!surplusAmountConfirmed.checked ? surplusAmountConfirmed : guideNoticeConfirmed).focus();
        return false;
    }

    return true;
}

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

    latestAllocations = normalizeAllocations(result.allocations || []);
    renderAllocations(latestAllocations);
    renderReasons(result.reasons || []);

    hasAnalysisResult = true;
    currentProductFilter = 'ALL';
    updateProductExplorer();

    window.dispatchEvent (
        new CustomEvent('surplus:allocation-updated', {
            detail: { allocations: latestAllocations}
        })
    );

    showStep(3);
}

function normalizeAllocations(allocations) {
    const allocationsByType = new Map();

    allocations.forEach((allocation) => {
        const ratio = Number(allocation?.ratio);
        const amount = Number(allocation?.amount);
        const assetType = allocation?.assetType;

        if (assetOrder.includes(assetType) && Number.isFinite(ratio) && Number.isFinite(amount)) {
            allocationsByType.set(assetType, { assetType, ratio, amount });
        }
    });

    return assetOrder
        .map((assetType) => allocationsByType.get(assetType))
        .filter(Boolean);
}

function renderAllocations(allocations) {
    const allocationGrid = document.getElementById('allocationGrid');
    allocationGrid.replaceChildren();

    const totalAmount = allocations.reduce(
        (sum, allocation) => sum + allocation.amount,
        0
    );

    let accumulatedRatio = 0;
    const gradientSegments = allocations.map((allocation) => {
        const startRatio = accumulatedRatio;
        accumulatedRatio += Math.max(allocation.ratio, 0);
        return `${assetColors[allocation.assetType]} ${startRatio}% ${accumulatedRatio}%`;
    });

    if (accumulatedRatio < 100) {
        gradientSegments.push(`#e6e8eb ${accumulatedRatio}% 100%`);
    }

    const donut = document.createElement('div');
    donut.className = 'allocation-donut';
    if (gradientSegments.length > 0) {
        donut.style.setProperty(
            '--allocation-gradient',
            `conic-gradient(${gradientSegments.join(', ')})`
        );
    }
    donut.setAttribute('role', 'img');
    donut.setAttribute(
        'aria-label',
        allocations
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

    allocations.forEach((allocation) => {
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

function updateProductExplorer() {
    const allocationsByType = new Map(
        latestAllocations.map((allocation) => [allocation.assetType, allocation])
    );

    updateProductFilterAmount('productFilterEtfAmount', allocationsByType.get('ETF'));
    updateProductFilterAmount('productFilterFundAmount', allocationsByType.get('FUND'));
    updateProductFilterAmount('productFilterCashAmount', allocationsByType.get('CASH'));

    productFilterButtons.forEach((button) => {
        const isActive = button.dataset.productFilter === currentProductFilter;
        button.classList.toggle('active', isActive);
        button.setAttribute('aria-pressed', String(isActive));
    });

    const productGrid =
        document.getElementById('productGrid');

    const productEmptyState =
        document.getElementById('productEmptyState');

    productGrid.replaceChildren();

    productEmptyState.hidden = false;
    productEmptyState.textContent =
        currentProductFilter === 'CASH'
            ? productEmptyMessages.CASH
            : productEmptyMessages.DEFAULT;
}

function updateProductFilterAmount(elementId, allocation) {
    document.getElementById(elementId).textContent = allocation
        ? `· ${wonFormatter.format(allocation.amount)}원`
        : '· 배정금액 없음';
}

function setLoading(loading) {
    isSubmitting = loading;
    submitButton.disabled = loading;
    submitButton.textContent = loading
        ? '분석하고 있습니다...'
        : '자산배분 결과 확인';
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

showQuestion(0, false);
showStep(1, false);
