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
            } else if (cardKey === 'SURPLUS_FUND' && card.surplusFundPrintData) {
                slot.innerHTML = window.ReportSurplusFundPdfCard.render(card);
            } else if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
                slot.classList.add('report-lifecycle-card');
                slot.innerHTML = renderLifecycleCard(card, true);
            }else {
                slot.innerHTML = renderGenericPdfCard(card);
            }
            return;
        }
        if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
            slot.innerHTML = renderLifecyclePreviewCard(card, index);
            slot.querySelector('.report-card-remove')?.addEventListener('click', () => removeCard(index));
            slot.querySelector('.report-card-change-plan')?.addEventListener('click', () => openLifecycleResultPickerModal(index));
            return;
        }
        const rows = (card.detailRows || []).map((row) => {
            const section = !row.value;
            return `<li class="report-card-row${section ? ' is-section' : ''}"><span>${escapeHtml(row.label)}</span>${section ? '' : `<span>${escapeHtml(row.value)}</span>`}</li>`;
        }).join('');
        let changeRefButton = '';

        if (!pdfMode && cardKey === 'FUTURESIM') {
            changeRefButton =
                `<button type="button" class="report-card-change-plan" data-index="${index}">계획 변경</button>`;
        } else if (!pdfMode && cardKey === 'SURPLUS_FUND') {
            changeRefButton =
                `<button type="button" class="report-card-change-plan" data-index="${index}">운용기록 변경</button>`;
        } else if (!pdfMode && cardKey === 'FINANCIAL_CYCLE_PLAN') {
            changeRefButton =
                `<button type="button" class="report-card-change-plan" data-index="${index}">시나리오 결과 변경</button>`;
        }
        slot.innerHTML =
            `<button type="button" class="report-card-remove" data-index="${index}" aria-label="카드 삭제">×</button>` +
            `<p class="report-card-title">${card.title}</p>` +
            `<p class="report-card-headline"><span>${card.headlineLabel}</span><br><b>${card.headlineValue}</b></p>` +
            (rows ? `<ul class="report-card-rows">${rows}</ul>` : '') +
            (card.note ? `<p class="report-card-note">${card.note}</p>` : '') +
            changeRefButton;
        const removeButton = slot.querySelector('.report-card-remove');
        if (removeButton) removeButton.addEventListener('click', () => removeCard(index));
        const changeBtn = slot.querySelector('.report-card-change-plan');

        if (changeBtn) {
            changeBtn.addEventListener('click', () => {
                if (cardKey === 'FUTURESIM') {
                    openPlanPickerModal(index);
                } else if (cardKey === 'SURPLUS_FUND') {
                    openSurplusFundPickerModal(index);
                } else if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
                    openLifecycleResultPickerModal(index);
                }
            });
        }
    }

    function renderCardError(index) {
        const slot = gridEl.querySelector(`.report-card-slot[data-index="${index}"]`);
        if (slot) {
            slot.innerHTML = '<p class="report-card-loading">불러오지 못했어요</p>';
        }
    }

    function renderGenericPdfCard(card) {
        const rows = (card.detailRows || []).map((row) => {
            const section = !row.value;
            return `<li class="report-card-row${section ? ' is-section' : ''}"><span>${escapeHtml(row.label)}</span>${section ? '' : `<strong>${escapeHtml(row.value)}</strong>`}</li>`;
        }).join('');
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
            btn.addEventListener('click', () => {
                const cardKey = btn.dataset.cardKey;
                if (cardKey === 'FUTURESIM') {
                    openPlanPickerModal(cards.length, true);
                } else if (cardKey === 'SURPLUS_FUND') {
                    openSurplusFundPickerModal(cards.length, true);
                } else if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
                    openLifecycleResultPickerModal(cards.length, true);
                } else {
                    addCard(cardKey);
                }
            });
        });
        modalBackdropEl.classList.remove('hidden');
    }

    function openPlanPickerModal(cardIndex, isNewCard = false) {
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
                    btn.addEventListener('click', () => selectCardReference(
                        'FUTURESIM', cardIndex, Number(btn.dataset.planId), isNewCard
                    ));
                });
            })
            .catch(() => {
                modalListEl.innerHTML = '<li class="report-modal-empty">불러오지 못했어요</li>';
            });
    }

    function openSurplusFundPickerModal(cardIndex, isNewCard = false) {
        modalTitleEl.textContent = '보여줄 운용기록 선택';
        modalListEl.innerHTML = '<li class="report-modal-empty">불러오는 중…</li>';

        modalBackdropEl.classList.remove('hidden');

        fetch('/api/surplus-funds/guide-versions')
            .then((res) =>
                res.ok ? res.json() : Promise.reject(res.status)
            )
            .then((versions) => {

                if (!versions || versions.length === 0) {
                    modalListEl.innerHTML = '<li class="report-modal-empty">저장된 운용기록이 없어요.</li>';
                    return;
                }

                modalListEl.innerHTML = versions.map((version) => {
                    const name = version.guideName || `운용기록 ${version.guideVersionNo}`;

                    return `
                    <li>
                        <button
                            type="button"
                            class="report-modal-option"
                            data-guide-version-id="${version.surplusFundGuideVersionId}"
                        >
                            <span>${escapeHtml(name)}</span>
                        </button>
                    </li>
                `;
                }).join('');

                modalListEl.querySelectorAll('.report-modal-option')
                        .forEach((btn) => {

                            btn.addEventListener('click', () => {
                                selectCardReference(
                                    'SURPLUS_FUND', cardIndex,
                                    Number(btn.dataset.guideVersionId), isNewCard
                                );
                            });

                        });
            })
            .catch(() => {
                modalListEl.innerHTML = '<li class="report-modal-empty">불러오지 못했어요.</li>';
            });
    }

    function renderLifecyclePreviewCard(card, index) {
        const rows = card.detailRows || [];
        const expenseIndex = rows.findIndex((row) => row.label === '시나리오 순서별 지출 금액');
        const monthlyBreakdownIndex = rows.findIndex((row) => row.label === '월 지출 상세 구성');
        const costIndex = rows.findIndex((row) => row.label === '시나리오 순서별 소요 비용');
        const oneTimeBreakdownIndex = rows.findIndex((row) => row.label === '일회성 비용 상세 구성');
        const analysisIndex = rows.findIndex((row) => row.label === '상세 분석 보고서');
        const expenseCount = expenseIndex < 0 ? 0 : rows.slice(expenseIndex + 1, monthlyBreakdownIndex >= 0 ? monthlyBreakdownIndex : costIndex).length;
        const costCount = costIndex < 0 ? 0 : rows.slice(costIndex + 1, oneTimeBreakdownIndex >= 0 ? oneTimeBreakdownIndex : analysisIndex).length;
        const eventCount = new Set(rows.slice(analysisIndex + 1).map((row) => String(row.label || '').match(/^STEP\s+(\d+)/)?.[1]).filter(Boolean)).size;
        return `<button type="button" class="report-card-remove" data-index="${index}" aria-label="카드 삭제">×</button>
            <p class="report-card-title">${escapeHtml(card.title)}</p>
            <p class="report-card-headline"><span>${escapeHtml(card.headlineLabel)}</span><br><b>${escapeHtml(card.headlineValue)}</b></p>
            <ul class="report-card-rows">
                <li class="report-card-row"><span>일회성 소요 비용</span><span>${costCount}개 STEP</span></li>
                <li class="report-card-row"><span>월 지출 변화</span><span>${expenseCount}개 STEP</span></li>
                <li class="report-card-row"><span>상세 분석</span><span>${eventCount}개 이벤트</span></li>
            </ul>
            <p class="report-card-note">미리보기에서 전체 금융 라이프 플랜 보고서를 확인할 수 있습니다.</p>
            <button type="button" class="report-card-change-plan" data-index="${index}">시나리오 결과 변경</button>`;
    }

    function renderLifecycleCard(card, forPdf, index) {
        const rows = card.detailRows || [];
        const expenseSectionIndex = rows.findIndex((row) => row.label === '시나리오 순서별 지출 금액');
        const monthlyBreakdownSectionIndex = rows.findIndex((row) => row.label === '월 지출 상세 구성');
        const costSectionIndex = rows.findIndex((row) => row.label === '시나리오 순서별 소요 비용');
        const oneTimeBreakdownSectionIndex = rows.findIndex((row) => row.label === '일회성 비용 상세 구성');
        const analysisSectionIndex = rows.findIndex((row) => row.label === '상세 분석 보고서');
        const expenses = expenseSectionIndex < 0 ? [] : rows.slice(expenseSectionIndex + 1, monthlyBreakdownSectionIndex >= 0 ? monthlyBreakdownSectionIndex : costSectionIndex);
        const monthlyBreakdownRows = monthlyBreakdownSectionIndex < 0 ? [] : rows.slice(monthlyBreakdownSectionIndex + 1, costSectionIndex);
        const costs = rows.slice(costSectionIndex + 1, oneTimeBreakdownSectionIndex >= 0 ? oneTimeBreakdownSectionIndex : (analysisSectionIndex < 0 ? rows.length : analysisSectionIndex));
        const oneTimeBreakdownRows = oneTimeBreakdownSectionIndex < 0 ? [] : rows.slice(oneTimeBreakdownSectionIndex + 1, analysisSectionIndex);
        const analysis = analysisSectionIndex < 0 ? [] : rows.slice(analysisSectionIndex + 1);
        const eventGroups = new Map();
        const summaryRows = [];

        analysis.forEach((row) => {
            const match = String(row.label || '').match(/^STEP\s+(\d+)/i);
            if (!match) {
                summaryRows.push(row);
                return;
            }
            const step = Number(match[1]);
            if (!eventGroups.has(step)) eventGroups.set(step, []);
            eventGroups.get(step).push(row);
        });

        let childbirthRecommendationsShown = false;
        const details = Array.from(eventGroups.entries()).map(([step, group], order) => {
            const cost = costs[order];
            const parsed = parseLifecycleStep(cost?.label, step);
            let visibleGroup = group;
            const isChildbirth = parsed.event.includes('출산')
                || group.some((row) => /출산/.test(String(row.label || '')));
            if (isChildbirth) {
                const recommendationPattern = /추천\s*금융\s*상품|맞춤\s*복지\s*혜택|·\s*(상품|복지)\s+/;
                const hasRecommendations = group.some((row) => recommendationPattern.test(String(row.label || '')));
                if (hasRecommendations && childbirthRecommendationsShown) {
                    visibleGroup = group.filter((row) => !recommendationPattern.test(String(row.label || '')));
                } else if (hasRecommendations) {
                    childbirthRecommendationsShown = true;
                }
            }
            let amortizationRows = visibleGroup.filter((row) => /·\s*원금균등상환 추이\s*·/.test(String(row.label || '')));
            if (!amortizationRows.length) {
                amortizationRows = buildEqualPrincipalChartRows(visibleGroup, step, parsed.event);
            }
            const detailRows = forPdf
                ? renderLifecycleDecisionPage(visibleGroup, step, parsed.event, amortizationRows)
                : renderLifecycleDetailRows(visibleGroup, step, parsed.event) + renderAmortizationChart(amortizationRows);
            const hasVisibleRecommendations = visibleGroup.some((row) => /추천\s*금융\s*상품|맞춤\s*복지\s*혜택|·\s*(상품|복지)\s+/.test(String(row.label || '')));
            return `<article class="report-lifecycle-event${hasVisibleRecommendations ? ' has-recommendations' : ' without-recommendations'}">
                ${forPdf ? `<div class="report-lifecycle-detail-page-heading"><span>MY FINANCIAL PLAN</span><b>STEP ${step}</b></div>` : ''}
                <header><span>STEP ${step}</span><h4>${escapeHtml(parsed.event)}</h4></header>
                <div class="report-lifecycle-event-body">${detailRows}</div>
            </article>`;
        }).join('');

        const controls = forPdf ? '' : `<button type="button" class="report-card-remove" data-index="${index}" aria-label="카드 삭제">×</button>`;
        const changeButton = forPdf ? '' : `<button type="button" class="report-card-change-plan" data-index="${index}">시나리오 결과 변경</button>`;
        const visibleExpenses = expenses.filter((row) => parseReportMoney(row.value) > 0);
        const expenseChart = renderLifecycleBarChart(visibleExpenses, '월 지출');
        const costChart = renderLifecycleBarChart(costs, '소요 비용');
        const expenseBreakdown = renderLifecycleBreakdownTable(visibleExpenses, monthlyBreakdownRows);
        const costBreakdown = renderLifecycleBreakdownTable(costs, oneTimeBreakdownRows);
        const titleParts = String(card.title || '').split(/\s*·\s*/);
        const reportEyebrow = titleParts[0] || '금융 라이프 플랜';
        const reportTitle = titleParts.slice(1).join(' · ') || '나의 미래 라이프 플랜';
        const hero = forPdf
            ? `<header class="report-lifecycle-hero"><span>${escapeHtml(reportEyebrow)}</span><h1>${escapeHtml(reportTitle)}</h1><p>시나리오 비용 분석 보고서</p></header>`
            : `<div class="report-lifecycle-hero"><p class="report-card-title">${escapeHtml(card.title)}</p><span>${escapeHtml(card.headlineLabel)}</span><strong>${escapeHtml(card.headlineValue)}</strong></div>`;
        return `${controls}<article class="report-lifecycle-content${forPdf ? ' is-full-report' : ' is-card-preview'}">
            ${hero}
            ${visibleExpenses.length ? `<section class="report-lifecycle-section report-lifecycle-monthly-summary"><div class="report-lifecycle-section-title"><span>01</span><div><h3>시나리오 순서별 월 지출 금액</h3><p>차트는 이벤트별 월 총액, 표는 생활비·원금·이자 구성을 보여줍니다.</p></div></div>${expenseChart}${expenseBreakdown}</section>` : ''}
            <section class="report-lifecycle-section report-lifecycle-one-time-summary"><div class="report-lifecycle-section-title"><span>${visibleExpenses.length ? '02' : '01'}</span><div><h3>시나리오 순서별 소요 비용</h3><p>차트는 이벤트별 총액, 표는 총액의 세부 구성을 보여줍니다.</p></div></div>${costChart}${costBreakdown}</section>
            <section class="report-lifecycle-section report-lifecycle-detail-section"><div class="report-lifecycle-section-title"><span>${visibleExpenses.length ? '03' : '02'}</span><div><h3>상세 분석 보고서</h3><p>이벤트별 자금 구성과 금융 영향을 정리했습니다.</p></div></div><div class="report-lifecycle-events">${details}</div></section>
            ${card.note ? `<p class="report-card-note">${escapeHtml(card.note)}</p>` : ''}${changeButton}
        </article>`;
    }

    function renderLifecycleBreakdownTable(totals, breakdownRows) {
        const groups = totals.map((total, order) => {
            const parsed = parseLifecycleStep(total.label, order + 1);
            const components = breakdownRows.filter((row) => {
                const rowStep = Number(String(row.label || '').match(/^STEP\s+(\d+)/i)?.[1]);
                return rowStep === parsed.step && parseReportMoney(row.value) > 0;
            });
            const componentRows = components.map((row) => {
                const item = String(row.label || '').split(/\s*·\s*/).slice(2).join(' · ') || '상세 항목';
                return `<tr><td></td><td>${escapeHtml(item)}</td><td>${escapeHtml(row.value)}</td></tr>`;
            }).join('');
            return `<tbody><tr class="is-total"><th>STEP ${parsed.step} · ${escapeHtml(parsed.event)}</th><th>총액</th><td>${escapeHtml(total.value)}</td></tr>${componentRows}</tbody>`;
        }).join('');
        return groups ? `<table class="report-lifecycle-breakdown-table"><thead><tr><th>이벤트</th><th>산출 항목</th><th>금액</th></tr></thead>${groups}</table>` : '';
    }

    function renderLifecycleDetailRows(rows, step, event) {
        const visibleRows = rows.filter((row) => {
            const label = String(row.label || '');
            const value = String(row.value || '');
            if (/·\s*(상품 보기|출처|기준일)$/.test(label)) return false;
            if (/·\s*원금균등상환 추이\s*·/.test(label)) return false;
            if (/상환방식\s*\/\s*월 납입액/.test(label)
                    && /^\s*-\s*[·ㆍ]\s*0(?:\.0+)?원\s*\/\s*월\s*$/.test(value.replace(/,/g, ''))) return false;
            return true;
        });
        const html = [];
        for (let index = 0; index < visibleRows.length; index += 1) {
            const row = visibleRows[index];
            const label = trimLifecycleDetailLabel(row.label, step, event);
            if (!row.value && /추천 금융상품/.test(label)) {
                const remainingSection = visibleRows.slice(index + 1).map((nextRow) => ({
                    row: nextRow,
                    label: trimLifecycleDetailLabel(nextRow.label, step, event)
                })).find(({row: nextRow}) => !nextRow.value);
                const sectionEnd = remainingSection ? visibleRows.indexOf(remainingSection.row) : visibleRows.length;
                const hasProduct = visibleRows.slice(index + 1, sectionEnd).some((nextRow) => /^상품\s+/.test(trimLifecycleDetailLabel(nextRow.label, step, event)));
                if (!hasProduct) continue;
            }
            if (/^복지\s+/.test(label)) {
                const benefitRows = [row];
                while (index + 1 < visibleRows.length) {
                    const nextLabel = trimLifecycleDetailLabel(visibleRows[index + 1].label, step, event);
                    if (!/^복지\s+/.test(nextLabel) || !/·\s*(기관|기준일|판정 사유|지원 금액)$/.test(nextLabel)) break;
                    benefitRows.push(visibleRows[index + 1]);
                    index += 1;
                }
                html.push(renderLifecycleBenefitTable(benefitRows, step, event));
                continue;
            }
            if (/^상품\s+/.test(label)) {
                const productRows = [row];
                while (index + 1 < visibleRows.length) {
                    const nextLabel = trimLifecycleDetailLabel(visibleRows[index + 1].label, step, event);
                    if (!/^상품\s+/.test(nextLabel) || !/·\s*(금융기관|상품유형|기준일|판정 사유|금리|한도|기간|상환방식)$/.test(nextLabel)) break;
                    productRows.push(visibleRows[index + 1]);
                    index += 1;
                }
                html.push(renderLifecycleProductTable(productRows, step, event));
                continue;
            }
            html.push(renderLifecycleDetailRow(row, step, event));
        }
        return html.join('');
    }

    function renderLifecycleDecisionPage(rows, step, event, amortizationRows) {
        const normalized = rows.filter((row) => {
            const label = trimLifecycleDetailLabel(row.label, step, event);
            return !/·\s*(상품 보기|출처|기준일)$/.test(String(row.label || ''))
                && !/원금균등상환 추이/.test(label)
                && !(/상환방식\s*\/\s*월 납입액/.test(label)
                    && /^\s*-\s*[·ㆍ]\s*0(?:\.0+)?원\s*\/\s*월\s*$/.test(String(row.value || '').replace(/,/g, '')));
        });
        const labeled = normalized.map((row) => ({row, label: trimLifecycleDetailLabel(row.label, step, event)}));
        const originalSummary = labeled.find(({label}) => label === '분석')?.row.value || '';
        const recommendations = normalized.filter((row) => {
            const label = trimLifecycleDetailLabel(row.label, step, event);
            return /추천 금융상품|맞춤 복지 혜택|^상품\s+|^복지\s+/.test(label);
        });
        const core = labeled.filter(({label}) => label !== '분석'
            && !/추천 금융상품|맞춤 복지 혜택|^상품\s+|^복지\s+/.test(label)
            && !/세부 산출 내역|PLAN CHECK/.test(label));

        const metricPatterns = [
            {label: '총 필요자금', pattern: /총 필요자금/},
            {label: event.includes('주택') ? '주택 구매에 필요한 현금' : '내가 마련할 금액', pattern: /본인 필요자금/},
            {label: '신규 대출', pattern: /신규 대출/},
            {label: '월 부담', pattern: /월 대출상환|월 지출/}
        ];
        const used = new Set();
        const metrics = [];
        metricPatterns.forEach((item) => {
            if (metrics.length >= 3) return;
            const found = core.find(({row, label}) => !used.has(row) && item.pattern.test(label));
            if (found) {
                used.add(found.row);
                metrics.push(`<div class="report-decision-metric"><span>${item.label}</span><strong>${escapeHtml(found.row.value)}</strong></div>`);
            }
        });

        const remaining = core.filter(({row}) => !used.has(row));
        const costRows = remaining.filter(({label}) => /직접 입력 결혼비용|예식장|식대|혼수|신혼여행|산후조리|카시트|유모차|아기침대|기타 준비물|매입·취득 자산가격|취득세|세금|등기비|중개보수/.test(label));
        const fundingRows = remaining.filter(({label}) => /입력 자기자금|부대비용 필요 현금|가족 지원금|공공지원|본인 필요자금|신규 대출/.test(label));
        const impactRows = remaining.filter(({label}) => /월 지출|월 대출상환|첫 달 원금|첫 달 이자|상환방식|대출기간|순자산 변화|월 저축여력|DSR/.test(label));
        const planRows = remaining.filter(({label}) => /진행 가능 여부|계획 진단|부족 현금|권장 연기/.test(label));
        const categorized = new Set([...costRows, ...fundingRows, ...impactRows, ...planRows].map(({row}) => row));
        const otherRows = remaining.filter(({row, label}) => row.value && !categorized.has(row) && !/총 필요자금/.test(label));

        const panel = (title, description, items, className = '') => items.length
            ? `<section class="report-decision-panel ${className}"><div class="report-decision-panel-title"><h5>${title}</h5><p>${description}</p></div><dl>${items.map(({row, label}) => `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(row.value)}</dd></div>`).join('')}</dl></section>`
            : '';
        const recommendationHtml = recommendations.length
            ? `<section class="report-decision-recommendations"><div class="report-decision-panel-title"><h5>추천 정보</h5><p>현재 조건에서 확인할 상품과 지원입니다.</p></div>${renderLifecycleDetailRows(recommendations, step, event)}</section>`
            : '';
        const insight = buildLifecycleDecisionInsight(labeled, event, originalSummary);
        const actionItems = buildLifecycleActionItems(labeled, event);

        return `<section class="report-decision-intro"><span>이번 STEP의 핵심 진단</span><p>${escapeHtml(insight)}</p></section>
            ${metrics.length ? `<section class="report-decision-metrics">${metrics.join('')}</section>` : ''}
            <div class="report-decision-grid">
                ${panel('비용 구성', '무엇에 자금이 필요한지 보여줍니다.', costRows)}
                ${panel('자금 마련', '내 자금과 외부 조달을 구분했습니다.', fundingRows)}
                ${panel('재무 영향', '이벤트 이후 달라지는 부담입니다.', impactRows, 'is-impact')}
                ${panel('추가 정보', '계획에 반영된 주요 조건입니다.', otherRows)}
            </div>
            ${actionItems.length ? `<section class="report-decision-action"><div><span>ACTION PLAN</span><h5>지금 확인할 사항</h5></div><ul>${actionItems.map((item) => `<li><span>${escapeHtml(item.label)}</span><strong>${escapeHtml(item.value)}</strong></li>`).join('')}</ul></section>` : ''}
            ${renderAmortizationChart(amortizationRows)}
            ${recommendationHtml}`;
    }

    function buildLifecycleDecisionInsight(rows, event, originalSummary) {
        const value = (pattern) => rows.find(({label, row}) => pattern.test(label) && row.value)?.row.value || '';
        const total = value(/총 필요자금/);
        const cash = value(/본인 필요자금/);
        const loan = value(/신규 대출/);
        const monthlyExpense = value(/^월 지출$/);
        const monthlyLoan = value(/월 대출상환/);
        const shortage = value(/부족 현금/);
        const savingChange = value(/월 저축여력 변화/);
        const dsr = value(/이벤트 후 DSR/);
        const sentences = [];

        if (total) {
            sentences.push(`${event} 계획에는 총 ${total}이 필요합니다.`);
        }
        if (cash && loan) {
            sentences.push(`이 중 현금으로 준비해야 할 금액은 ${cash}, 신규 대출은 ${loan}으로 구성됩니다.`);
        } else if (cash) {
            sentences.push(`현재 계획에서 직접 마련해야 할 금액은 ${cash}입니다.`);
        } else if (loan) {
            sentences.push(`신규 대출 ${loan}이 계획에 반영됩니다.`);
        }
        if (parseReportMoney(shortage) > 0) {
            sentences.push(`현재 금융 상태를 적용하면 ${shortage}의 자금 공백이 발생하는 계획입니다.`);
        }
        const monthlyParts = [monthlyExpense && `생활·관리비 ${monthlyExpense}`, monthlyLoan && `대출상환 ${monthlyLoan}`].filter(Boolean);
        if (monthlyParts.length) {
            const financialImpact = [`매월 ${monthlyParts.join(', ')}의 고정 부담`, savingChange && `월 저축여력 변화 ${savingChange}`, dsr && `예상 DSR ${dsr}`].filter(Boolean);
            sentences.push(`이벤트 이후 ${financialImpact.join(', ')}이 반영됩니다.`);
        } else if (savingChange || dsr) {
            sentences.push(`이벤트 이후 ${[savingChange && `월 저축여력 변화 ${savingChange}`, dsr && `예상 DSR ${dsr}`].filter(Boolean).join(', ')}이 반영됩니다.`);
        }
        if (sentences.length < 3 && originalSummary && !/저장된 시뮬레이션 결과/.test(originalSummary)) {
            sentences.push(originalSummary);
        }
        return sentences.slice(0, 3).join(' ') || originalSummary || `${event} 계획의 비용과 자금 조달 구조를 확인해주세요.`;
    }

    function buildLifecycleActionItems(rows, event) {
        const value = (pattern) => rows.find(({label, row}) => pattern.test(label) && row.value)?.row.value || '';
        const actions = [];
        const shortage = value(/부족 현금/);
        const cash = value(/본인 필요자금/);
        const monthlyLoan = value(/월 대출상환/);
        const monthlyExpense = value(/^월 지출$/);
        const loanTerms = value(/대출기간\s*\/\s*적용금리/);
        const savingChange = value(/월 저축여력 변화/);
        const dsr = value(/이벤트 후 DSR/);
        const recommendedDelay = value(/권장 연기/);

        if (parseReportMoney(shortage) > 0) {
            actions.push({label: '자금 조달안 보완', value: `자기자금을 늘리거나 ${event} 계획 규모를 조정해 자금 공백을 먼저 해소하세요.`});
        } else if (cash) {
            actions.push({label: '현금성 자산 확보', value: '자기자금과 부대비용을 실행 시점에 바로 사용할 수 있도록 분리해 준비하세요.'});
        }
        if (monthlyLoan || monthlyExpense) {
            actions.push({label: '월 예산 재점검', value: '새로운 고정비를 반영한 뒤에도 생활비와 정기 저축을 유지할 수 있는지 확인하세요.'});
        }
        if (loanTerms) {
            actions.push({label: '대출 조건 비교', value: '기간과 금리가 다른 대안을 두 개 이상 비교하고 중도상환 계획도 함께 검토하세요.'});
        }
        if (actions.length < 3 && (savingChange || dsr)) {
            actions.push({label: '재무 안전선 유지', value: '대출 실행 후에도 비상자금과 최소 저축액이 남도록 조달 규모를 결정하세요.'});
        }
        if (actions.length < 3 && recommendedDelay) {
            actions.push({label: '실행 시점 조정', value: '자금 여력이 회복되는 시점으로 계획을 옮기는 방안도 비교하세요.'});
        }
        return actions.slice(0, 3);
    }

    function renderLifecycleBenefitTable(rows, step, event) {
        return renderLifecycleRecommendationTable(rows, step, event, '복지', 'report-lifecycle-benefit-table');
    }

    function renderLifecycleProductTable(rows, step, event) {
        return renderLifecycleRecommendationTable(rows, step, event, '상품', 'report-lifecycle-product-table');
    }

    function renderLifecycleRecommendationTable(rows, step, event, type, className) {
        const itemLabel = trimLifecycleDetailLabel(rows[0].label, step, event).replace(new RegExp(`^${type}\\s+`), '');
        const status = ({ELIGIBLE: '신청 가능', NEEDS_CONFIRMATION: '확인 필요', NOT_ELIGIBLE: '신청 어려움'})[rows[0].value] || rows[0].value;
        const cells = rows.slice(1).map((row) => {
            const label = trimLifecycleDetailLabel(row.label, step, event).split(/\s*·\s*/).pop();
            return {label, value: row.value};
        });
        const body = [];
        for (let index = 0; index < cells.length; index += 2) {
            const left = cells[index];
            const right = cells[index + 1];
            body.push(`<tr><th>${escapeHtml(left.label)}</th><td>${escapeHtml(left.value)}</td>${right ? `<th>${escapeHtml(right.label)}</th><td>${escapeHtml(right.value)}</td>` : '<th></th><td></td>'}</tr>`);
        }
        return `<table class="report-lifecycle-product-table ${className}"><thead><tr><th colspan="3">${escapeHtml(itemLabel)}</th><td>${escapeHtml(status)}</td></tr></thead><tbody>${body.join('')}</tbody></table>`;
    }

    function renderLifecycleDetailRow(row, step, event) {
        const label = trimLifecycleDetailLabel(row.label, step, event);
        const isSummary = label === '분석';
        const isSubsection = !row.value;
        const isPlan = /진행 가능 여부|계획 진단|부족 현금|권장 연기/.test(label);
        const isMoney = /필요자금|자산가격|취득세|세금|등기비|중개보수|자기자금|지원금|월 지출|대출|상환액|납입액|원금|이자|순자산/.test(label);
        const isBenefit = /^복지\s/.test(label);
        const isMeta = /·\s*(기관|기준일|판정 사유|지원 금액)$/.test(label);
        const visibleLabel = isMeta ? label.split(/\s*·\s*/).pop() : label;
        const displayValue = ({ELIGIBLE: '신청 가능', NEEDS_CONFIRMATION: '확인 필요', NOT_ELIGIBLE: '신청 어려움'})[row.value] || row.value;
        const value = /^https?:\/\//i.test(String(displayValue || ''))
            ? `<a href="${escapeHtml(row.value)}" target="_blank" rel="noopener noreferrer">바로가기</a>`
            : `<strong>${escapeHtml(displayValue)}</strong>`;
        const classes = [isSummary && 'is-summary', isSubsection && 'is-subsection', isPlan && 'is-plan', isMoney && !isPlan && 'is-money', isBenefit && 'is-benefit', isMeta && 'is-meta'].filter(Boolean).join(' ');
        return `<div class="report-lifecycle-detail-row${classes ? ` ${classes}` : ''}"><span>${escapeHtml(isSummary ? '분석 결과' : visibleLabel)}</span>${isSubsection ? '' : value}</div>`;
    }

    function renderLifecycleBarChart(rows, valueLabel) {
        if (!rows.length) return '';
        const values = rows.map((row) => parseReportMoney(row.value));
        const max = Math.max(...values, 1);
        const bars = rows.map((row, index) => {
            const parsed = parseLifecycleStep(row.label, index + 1);
            const height = values[index] <= 0 ? 3 : Math.max(10, Math.round(values[index] / max * 100));
            return `<div class="report-lifecycle-chart-item" title="${escapeHtml(`${parsed.event} ${valueLabel} ${row.value}`)}">
                <div class="report-lifecycle-chart-value">${escapeHtml(row.value)}</div>
                <div class="report-lifecycle-chart-track"><span style="height:${height}%"></span></div>
                <b>STEP ${parsed.step}</b><small>${escapeHtml(parsed.event)}</small>
            </div>`;
        }).join('');
        return `<div class="report-lifecycle-chart" role="img" aria-label="${escapeHtml(valueLabel)} 단계별 막대 차트">${bars}</div>`;
    }

    function renderAmortizationChart(rows) {
        if (!rows.length) return '';
        const values = rows.map((row) => parseReportMoney(row.value));
        const max = Math.max(...values, 1);
        const bars = rows.map((row, index) => {
            const year = String(row.label || '').match(/(\d+)년\s*차/)?.[1] || index + 1;
            const height = Math.max(10, Math.round(values[index] / max * 100));
            return `<div class="report-amortization-bar"><span>${escapeHtml(row.value)}</span><div><i style="height:${height}%"></i></div><b>${escapeHtml(year)}년 차</b></div>`;
        }).join('');
        return `<section class="report-amortization"><div class="report-amortization-title"><b>원금균등상환 월 납입액 변화</b><span>원금이 줄어들수록 월 납입액이 감소합니다.</span></div><div class="report-amortization-chart">${bars}</div></section>`;
    }

    function buildEqualPrincipalChartRows(rows, step, event) {
        const findRow = (pattern) => rows.find((row) => pattern.test(String(row.label || '')));
        const repayment = findRow(/상환방식\s*\/\s*월 납입액/);
        if (!/원금균등상환/.test(String(repayment?.value || ''))) return [];

        const principal = parseReportMoney(findRow(/신규 대출/)?.value);
        const monthlyPrincipal = parseReportMoney(findRow(/첫 달 원금 상환액/)?.value);
        const firstInterest = parseReportMoney(findRow(/첫 달 이자 납부액/)?.value);
        const periodText = String(findRow(/대출기간\s*\/\s*적용금리/)?.value || '');
        const months = Number(periodText.match(/(\d+)개월/)?.[1] || 0);
        if (!principal || !months) return [];

        const principalPerMonth = monthlyPrincipal || principal / months;
        const monthlyRate = firstInterest > 0 ? firstInterest / principal : 0;
        const totalYears = Math.ceil(months / 12);
        return [1, 5, 10, 15, 20, 25, 30, 35, 40]
            .filter((year) => year <= totalYears)
            .map((year) => {
                const month = Math.min((year - 1) * 12 + 1, months);
                const remaining = Math.max(0, principal - principalPerMonth * (month - 1));
                const payment = principalPerMonth + remaining * monthlyRate;
                return {
                    label: `STEP ${step} · ${event} · 원금균등상환 추이 · ${year}년 차`,
                    value: `${Math.round(payment).toLocaleString('ko-KR')}원/월`
                };
            });
    }

    function parseReportMoney(value) {
        const text = String(value || '').replace(/,/g, '');
        const eok = Number(text.match(/(-?[\d.]+)억/)?.[1] || 0) * 100000000;
        const man = Number(text.match(/(-?[\d.]+)만/)?.[1] || 0) * 10000;
        if (eok || man) return Math.abs(eok + man);
        return Math.abs(Number(text.replace(/[^\d.-]/g, '')) || 0);
    }

    function parseLifecycleStep(label, fallbackStep) {
        const text = String(label || '');
        const parts = text.split(/\s*[·ㆍ]\s*/).filter(Boolean);
        const stepMatch = (parts[0] || text).match(/STEP\s+(\d+)/i);
        return {
            step: stepMatch ? Number(stepMatch[1]) : fallbackStep,
            event: parts[1] || `이벤트 ${fallbackStep}`,
            date: parts.slice(2).join(' · ')
        };
    }

    function trimLifecycleDetailLabel(label, step, event) {
        return String(label || '')
            .replace(new RegExp(`^STEP\\s+${step}\\s*[·ㆍ]?\\s*`), '')
            .replace(new RegExp(`^${escapeRegExp(event)}\\s*[·ㆍ]?\\s*`), '')
            .replace(/^분석$/, '분석');
    }

    function escapeRegExp(value) {
        return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    function openLifecycleResultPickerModal(cardIndex, isNewCard = false) {
        modalTitleEl.textContent = '보여줄 시나리오 결과 선택';
        modalListEl.innerHTML = '<li class="report-modal-empty">불러오는 중…</li>';
        modalBackdropEl.classList.remove('hidden');

        fetch('/api/lifecycle/scenarios/results')
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((results) => {
                if (!results || results.length === 0) {
                    modalListEl.innerHTML = '<li class="report-modal-empty">저장된 시나리오 결과가 없어요.</li>';
                    return;
                }
                modalListEl.innerHTML = results.map((result) => `
                    <li>
                        <button type="button" class="report-modal-option"
                                data-lifecycle-result-id="${result.lifecycleScenarioResultId}">
                            <span>${escapeHtml(result.scenarioName || '금융 라이프 플랜')}</span>
                        </button>
                    </li>
                `).join('');
                modalListEl.querySelectorAll('[data-lifecycle-result-id]').forEach((btn) => {
                    btn.addEventListener('click', () => selectCardReference(
                        'FINANCIAL_CYCLE_PLAN', cardIndex,
                        Number(btn.dataset.lifecycleResultId), isNewCard
                    ));
                });
            })
            .catch(() => {
                modalListEl.innerHTML = '<li class="report-modal-empty">불러오지 못했어요.</li>';
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

    function selectCardReference(cardKey, index, refId, isNewCard) {
        if (isNewCard) {
            cards = [...cards, {cardKey, refId}];
            closeModal();
            saveLayout();
            return;
        }
        setCardRefId(index, refId);
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
