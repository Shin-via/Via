(function () {
    const MAX_CARDS = 4;

    const headerLoadingEl = document.getElementById('reportHeaderLoading');
    const headerBoxEl = document.getElementById('reportHeaderBox');
    const totalScoreEl = document.getElementById('reportTotalScore');
    const gradeEl = document.getElementById('reportGrade');
    const barListEl = document.getElementById('reportBarList');
    const gridEl = document.getElementById('reportCardGrid');
    const modalBackdropEl = document.getElementById('reportModalBackdrop');
    const modalTitleEl = document.getElementById('reportModalTitle');
    const modalListEl = document.getElementById('reportModalList');
    const modalCloseEl = document.getElementById('reportModalClose');
    if (!gridEl) {
        return;
    }

    const csrfHeaderName = document.getElementById('csrfHeaderName').value;
    const csrfTokenValue = document.getElementById('csrfTokenValue').value;
    const pdfMode = gridEl.closest('[data-pdf-mode]')?.dataset.pdfMode === 'true';

    let cards = [];
    let options = [];

    function authHeaders() {
        return {'Content-Type': 'application/json', [csrfHeaderName]: csrfTokenValue};
    }

    function loadHeader() {
        return fetch('/api/report/header')
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((header) => {
                headerLoadingEl.classList.add('hidden');
                headerBoxEl.classList.remove('hidden');
                totalScoreEl.textContent = Math.round(Number(header.totalScore));
                gradeEl.textContent = header.grade;
                barListEl.innerHTML = header.dimensions.map((dimension) => {
                    const score = Math.round(Number(dimension.score));
                    return '<li class="report-bar-item">' +
                        `<span class="report-bar-label">${dimension.label} <b>${score}점</b></span>` +
                        '<span class="report-bar-track">' +
                            `<span class="report-bar-fill" style="width:${score}%"></span>` +
                        '</span>' +
                        '</li>';
                }).join('');
            })
            .catch(() => {
                headerLoadingEl.textContent = '불러오지 못했어요';
            });
    }

    function renderGrid() {
        const slots = pdfMode ? cards : [];
        if (!pdfMode) {
            for (let i = 0; i < MAX_CARDS; i += 1) {
                slots.push(cards[i] || null);
            }
        }
        if (pdfMode && slots.length === 0) {
            gridEl.innerHTML = '<p class="report-card-loading">선택한 카드가 없어요.</p>';
            return Promise.resolve();
        }
        gridEl.innerHTML = slots.map((card, index) => card
            ? `<div class="report-card-slot report-card-filled" data-index="${index}"><p class="report-card-loading">불러오는 중…</p></div>`
            : `<button type="button" class="report-card-slot report-card-empty" data-index="${index}">+ 카드 추가</button>`
        ).join('');

        if (!pdfMode) gridEl.querySelectorAll('.report-card-empty').forEach((el) => {
            el.addEventListener('click', () => openAddCardModal());
        });

        return Promise.all(slots.filter(Boolean).map((card, index) => {
            const cardIndex = slots.indexOf(card, index);
            const query = card.refId
                ? `cardKey=${encodeURIComponent(card.cardKey)}&refId=${encodeURIComponent(card.refId)}`
                : `cardKey=${encodeURIComponent(card.cardKey)}`;
            return fetch(`/api/report/card-data?${query}`)
                .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
                .then((data) => renderCard(cardIndex, card.cardKey, data))
                .catch(() => renderCardError(cardIndex));
        }));
    }

    function renderCard(index, cardKey, card) {
        const slot = gridEl.querySelector(`.report-card-slot[data-index="${index}"]`);
        if (!slot) {
            return;
        }
        if (pdfMode) {
            slot.className = 'report-card-slot report-card-filled report-pdf-card-page';
            if (cardKey === 'FUTURESIM' && card.futuresimPrintData) {
                slot.innerHTML = window.ReportFuturesimPdfCard.render(card);
                window.ReportFuturesimPdfCard.renderChart(slot, card);
            } else {
                slot.innerHTML = renderGenericPdfCard(card);
            }
            return;
        }
        const rows = (card.detailRows || []).map((row) =>
            `<li class="report-card-row"><span>${row.label}</span><span>${row.value}</span></li>`
        ).join('');
        const changePlanButton = !pdfMode && cardKey === 'FUTURESIM'
            ? `<button type="button" class="report-card-change-plan" data-index="${index}">계획 변경</button>`
            : '';
        slot.innerHTML =
            `<button type="button" class="report-card-remove" data-index="${index}" aria-label="카드 삭제">×</button>` +
            `<p class="report-card-title">${card.title}</p>` +
            `<p class="report-card-headline"><span>${card.headlineLabel}</span><br><b>${card.headlineValue}</b></p>` +
            (rows ? `<ul class="report-card-rows">${rows}</ul>` : '') +
            (card.note ? `<p class="report-card-note">${card.note}</p>` : '') +
            changePlanButton;
        const removeButton = slot.querySelector('.report-card-remove');
        if (removeButton) removeButton.addEventListener('click', () => removeCard(index));
        const changeBtn = slot.querySelector('.report-card-change-plan');
        if (changeBtn) {
            changeBtn.addEventListener('click', () => openPlanPickerModal(index));
        }
    }

    function renderCardError(index) {
        const slot = gridEl.querySelector(`.report-card-slot[data-index="${index}"]`);
        if (slot) {
            slot.innerHTML = '<p class="report-card-loading">불러오지 못했어요</p>';
        }
    }

    function renderGenericPdfCard(card) {
        const rows = (card.detailRows || []).map((row) =>
            `<li class="report-card-row"><span>${escapeHtml(row.label)}</span><strong>${escapeHtml(row.value)}</strong></li>`
        ).join('');
        return `<article class="report-pdf-card-detail"><p class="report-pdf-card-eyebrow">선택한 카드</p><h2>${escapeHtml(card.title)}</h2><p class="report-pdf-card-headline"><span>${escapeHtml(card.headlineLabel)}</span><strong>${escapeHtml(card.headlineValue)}</strong></p>${rows ? `<ul class="report-card-rows">${rows}</ul>` : ''}${card.note ? `<p class="report-card-note">${escapeHtml(card.note)}</p>` : ''}</article>`;
    }

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>'"]/g, (character) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'}[character]));
    }

    function openAddCardModal() {
        modalTitleEl.textContent = '카드 추가';
        const placed = new Set(cards.map((c) => c.cardKey));
        modalListEl.innerHTML = options.map((option) => {
            const disabled = !option.available || placed.has(option.cardKey);
            const badge = !option.available ? '<span class="report-modal-badge">준비중</span>' : '';
            return `<li><button type="button" class="report-modal-option" data-card-key="${option.cardKey}" ${disabled ? 'disabled' : ''}>` +
                `<span>${option.label}</span>${badge}</button></li>`;
        }).join('');
        modalListEl.querySelectorAll('.report-modal-option:not([disabled])').forEach((btn) => {
            btn.addEventListener('click', () => addCard(btn.dataset.cardKey));
        });
        modalBackdropEl.classList.remove('hidden');
    }

    function openPlanPickerModal(cardIndex) {
        modalTitleEl.textContent = '보여줄 계획 선택';
        modalListEl.innerHTML = '<li class="report-modal-empty">불러오는 중…</li>';
        modalBackdropEl.classList.remove('hidden');

        fetch('/api/future-simulation/plans')
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((plans) => {
                if (!plans || plans.length === 0) {
                    modalListEl.innerHTML = '<li class="report-modal-empty">저장된 계획이 없어요. 5단계 "나만의 실행 계획"에서 먼저 저장해보세요.</li>';
                    return;
                }
                modalListEl.innerHTML = plans.map((plan) =>
                    `<li><button type="button" class="report-modal-option" data-plan-id="${plan.id}">` +
                        `<span>${plan.planName}</span></button></li>`
                ).join('');
                modalListEl.querySelectorAll('.report-modal-option').forEach((btn) => {
                    btn.addEventListener('click', () => setCardRefId(cardIndex, Number(btn.dataset.planId)));
                });
            })
            .catch(() => {
                modalListEl.innerHTML = '<li class="report-modal-empty">불러오지 못했어요</li>';
            });
    }

    function closeModal() {
        modalBackdropEl.classList.add('hidden');
    }

    function addCard(cardKey) {
        if (cards.length >= MAX_CARDS || cards.some((c) => c.cardKey === cardKey)) {
            closeModal();
            return;
        }
        cards = [...cards, {cardKey, refId: null}];
        closeModal();
        saveLayout();
    }

    function removeCard(index) {
        cards = cards.filter((_, i) => i !== index);
        saveLayout();
    }

    function setCardRefId(index, refId) {
        cards = cards.map((c, i) => (i === index ? {...c, refId} : c));
        closeModal();
        saveLayout();
    }

    function saveLayout() {
        renderGrid();
        fetch('/api/report/layout', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({cards}),
        }).catch(() => {});
    }

    if (!pdfMode) {
        modalCloseEl.addEventListener('click', closeModal);
        modalBackdropEl.addEventListener('click', (event) => {
            if (event.target === modalBackdropEl) {
                closeModal();
            }
        });
    }

    const headerPromise = loadHeader();

    Promise.all([
        pdfMode ? Promise.resolve([]) : fetch('/api/report/card-options').then((res) => (res.ok ? res.json() : [])),
        fetch('/api/report/layout').then((res) => (res.ok ? res.json() : {cards: []})),
    ]).then(([optionList, layout]) => {
        options = optionList || [];
        cards = (layout && layout.cards) || [];
        return Promise.all([headerPromise, renderGrid()]);
    }).catch(() => headerPromise).finally(() => {
        if (pdfMode) document.body.dataset.chartsReady = 'true';
    });
})();
