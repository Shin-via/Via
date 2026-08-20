document.addEventListener("DOMContentLoaded", () => {

    /*
     * =========================================================
     * 0. 기본 설정
     * =========================================================
     */

    const requestedScenarioId = new URLSearchParams(
        window.location.search
    ).get("scenarioId");
    let scenarioId = null;
    let scenarioName = null;
    let baseSurveyReady = true;

    async function ensureScenario() {
        if (!scenarioId) {
            throw new Error("진행할 시나리오를 먼저 선택해주세요.");
        }
        return scenarioId;
    }

    const START_YEAR = new Date().getFullYear();

    const regionData = {
        "서울특별시": ["종로구", "중구", "용산구", "성동구", "광진구", "동대문구", "중랑구", "성북구", "강북구", "도봉구", "노원구", "은평구", "서대문구", "마포구", "양천구", "강서구", "구로구", "금천구", "영등포구", "동작구", "관악구", "서초구", "강남구", "송파구", "강동구"],
        "부산광역시": ["중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구", "해운대구", "사하구", "금정구", "강서구", "연제구", "수영구", "사상구", "기장군"],
        "대구광역시": ["중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군", "군위군"],
        "인천광역시": ["중구", "동구", "미추홀구", "연수구", "남동구", "부평구", "계양구", "서구", "강화군", "옹진군"],
        "광주광역시": ["동구", "서구", "남구", "북구", "광산구"],
        "대전광역시": ["동구", "중구", "서구", "유성구", "대덕구"],
        "울산광역시": ["중구", "남구", "동구", "북구", "울주군"],
        "세종특별자치시": ["세종특별자치시"],
        "경기도": ["수원시", "용인시", "고양시", "화성시", "성남시", "부천시", "남양주시", "안산시", "평택시", "안양시", "시흥시", "파주시", "김포시", "의정부시", "광주시", "하남시", "광명시", "군포시", "양주시", "오산시", "이천시", "안성시", "구리시", "의왕시", "포천시", "양평군", "여주시", "동두천시", "과천시", "가평군", "연천군"],
        "강원특별자치도": ["춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시", "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "고성군", "양양군"],
        "충청북도": ["청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "증평군", "진천군", "괴산군", "음성군", "단양군"],
        "충청남도": ["천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시", "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군"],
        "전북특별자치도": ["전주시", "군산시", "익산시", "정읍시", "남원시", "김제시", "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"],
        "전라남도": ["목포시", "여수시", "순천시", "나주시", "광양시", "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군", "해남군", "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군"],
        "경상북도": ["포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시", "문경시", "경산시", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군", "봉화군", "울진군", "울릉군"],
        "경상남도": ["창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시", "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군"],
        "제주특별자치도": ["제주시", "서귀포시"]
    };


    /*
     * 현재 화면에서 선택한 이벤트
     */
    let selectedEventType = null;
    const selectedEventTypes = new Set();
    let lifecycleEvents = [];

    const lifecycleEventNames = {
        marriage: "결혼",
        childbirth: "출산",
        vehicle: "차량 구매",
        "monthly-rent": "월세",
        jeonse: "전세",
        "home-purchase": "주택 구매",
        repayment: "대출 상환"
    };

    const lifecycleEventTypes = {
        MARRIAGE: "marriage",
        CHILDBIRTH: "childbirth",
        VEHICLE_PURCHASE: "vehicle",
        MONTHLY_RENT: "monthly-rent",
        JEONSE: "jeonse",
        HOME_PURCHASE: "home-purchase",
        REPAYMENT: "repayment"
    };

    const lifecycleEventDetailPaths = {
        marriage: "marriage",
        childbirth: "childbirth",
        vehicle: "vehicle",
        "monthly-rent": "monthly-rent",
        jeonse: "jeonse",
        "home-purchase": "home-purchase",
        repayment: "repayment"
    };

    function toServerDate(yearMonth) {
        if (!yearMonth) {
            return new Date().toISOString().substring(0, 10);
        }
        return yearMonth.length === 7 ? `${yearMonth}-01` : yearMonth;
    }

    function toYearMonth(serverDate) {
        return serverDate ? serverDate.substring(0, 7) : "";
    }

    function formatRelativePeriod(targetDateStr) {
        if (!targetDateStr) return "시기 미정";
        const [y, m] = targetDateStr.split("-").map(Number);
        const totalMonthDiff = (y - START_YEAR) * 12 + (m - 1);

        if (totalMonthDiff <= 0) {
            return "현재";
        }
        const years = Math.floor(totalMonthDiff / 12);
        const months = totalMonthDiff % 12;

        if (years === 0) {
            return `${months}개월 후`;
        }
        if (months === 0) {
            return `${years}년 후`;
        }
        return `${years}년 ${months}개월 후`;
    }

    function formatYearMonth(yearMonth) {
        if (!yearMonth) {
            return "시기 미정";
        }
        return formatRelativePeriod(yearMonth);
    }


    /*
     * 저장된 이벤트 ID 관리
     *
     * 수정 API를 붙일 때 사용할 수 있다.
     */
    const savedEventIds = {
        marriage: null,
        childbirth: null,
        vehicle: null,
        "monthly-rent": null,
        jeonse: null,
        "home-purchase": null,
        repayment: null
    };


    /*
     * =========================================================
     * 1. DOM 요소
     * =========================================================
     */

    // STEP 버튼
    const stepButtons =
        document.querySelectorAll(".lifecycle-step");

    // STEP별 화면
    const stepPanels =
        document.querySelectorAll("[data-step-panel]");

    // 이전/다음 이동 버튼
    const stepMoveButtons =
        document.querySelectorAll("[data-go-step]");

    // 이벤트 선택 버튼
    const eventButtons =
        document.querySelectorAll("[data-event-type]");

    // 이벤트별 상세 폼 영역
    const eventForms =
        document.querySelectorAll("[data-event-form]");

    const scenarioGate = document.getElementById("lifecycleScenarioGate");
    const scenarioList = document.getElementById("lifecycleScenarioList");
    const newScenarioNameInput = document.getElementById("newScenarioName");
    const createScenarioButton = document.getElementById("createLifecycleScenarioBtn");
    const toggleScenarioListButton = document.getElementById("toggleLifecycleScenarioListBtn");
    const changeScenarioButton = document.getElementById("changeLifecycleScenarioBtn");
    const activeScenarioName = document.getElementById("activeLifecycleScenarioName");

    function updateSigunguOptions(sidoSelect, selectedValue = "") {
        const sigunguSelect = document.getElementById(
            sidoSelect.dataset.regionTarget
        );

        if (!sigunguSelect) {
            return;
        }

        const sigunguList = regionData[sidoSelect.value] ?? [];
        sigunguSelect.replaceChildren();

        const placeholder = document.createElement("option");
        placeholder.value = "";
        placeholder.textContent = sigunguList.length
            ? "시·군·구를 선택해주세요"
            : "먼저 시·도를 선택해주세요";
        sigunguSelect.appendChild(placeholder);

        sigunguList.forEach(sigungu => {
            const option = document.createElement("option");
            option.value = sigungu;
            option.textContent = sigungu;
            sigunguSelect.appendChild(option);
        });

        sigunguSelect.disabled = sigunguList.length === 0;
        sigunguSelect.value = sigunguList.includes(selectedValue)
            ? selectedValue
            : "";
    }

    document.querySelectorAll("[data-region-sido]")
        .forEach(sidoSelect => {
            Object.keys(regionData).forEach(sido => {
                const option = document.createElement("option");
                option.value = sido;
                option.textContent = sido;
                sidoSelect.appendChild(option);
            });

            sidoSelect.addEventListener("change", () => {
                updateSigunguOptions(sidoSelect);
            });
        });

    const moneyInputs = Array.from(
        document.querySelectorAll(
            ".lifecycle-input-unit > input"
        )
    ).filter(input =>
        input.nextElementSibling?.textContent.trim() === "원"
    );

    function normalizeMoneyDigits(value) {
        const digits = String(value ?? "")
            .replace(/[^0-9]/g, "");

        if (digits === "") {
            return "";
        }

        return digits.replace(/^0+(?=\d)/, "");
    }

    function formatMoneyInput(input) {
        const digits = normalizeMoneyDigits(input.value);
        input.value = digits.replace(
            /\B(?=(\d{3})+(?!\d))/g,
            ","
        );
    }

    function parseMoneyValue(value) {
        const digits = normalizeMoneyDigits(value);
        return digits === "" ? 0 : Number(digits);
    }

    moneyInputs.forEach(input => {
        input.type = "text";
        input.inputMode = "numeric";
        input.autocomplete = "off";
        input.setAttribute("aria-label", "금액");
        formatMoneyInput(input);
        input.addEventListener("input", () => {
            formatMoneyInput(input);
        });
    });

    const vehiclePriceInput = document.getElementById("vehiclePrice");
    const vehicleCashPaymentInput = document.getElementById(
        "vehicleCashPaymentAmount"
    );
    const vehicleLoanAmountInput = document.getElementById(
        "vehicleLoanAmount"
    );
    const vehicleLoanPeriodInput = document.getElementById(
        "vehicleLoanPeriodMonths"
    );

    function updateVehicleFinancingAmounts() {
        if (
            !vehiclePriceInput
            || !vehicleCashPaymentInput
            || !vehicleLoanAmountInput
        ) {
            return;
        }

        const vehiclePrice = parseMoneyValue(vehiclePriceInput.value);
        const loanAmount = Math.min(
            parseMoneyValue(vehicleLoanAmountInput.value),
            vehiclePrice
        );
        vehicleLoanAmountInput.value = loanAmount;
        vehicleCashPaymentInput.value = Math.max(
            vehiclePrice - loanAmount,
            0
        );
        formatMoneyInput(vehicleCashPaymentInput);
        formatMoneyInput(vehicleLoanAmountInput);

        if (vehicleLoanPeriodInput) {
            const hasLoan = parseMoneyValue(
                vehicleLoanAmountInput.value
            ) > 0;
            vehicleLoanPeriodInput.disabled = !hasLoan;
            if (!hasLoan) {
                vehicleLoanPeriodInput.value = "";
            }
        }
    }

    [vehiclePriceInput, vehicleLoanAmountInput].forEach(input => {
        input?.addEventListener("input", updateVehicleFinancingAmounts);
    });

    const jeonseDesiredAmountInput =
        document.getElementById("jeonseDesiredAmount");
    const jeonseOwnFundAmountInput =
        document.getElementById("jeonseOwnFundAmount");
    const jeonseDesiredLoanAmountInput =
        document.getElementById("jeonseDesiredLoanAmount");

    function updateJeonseLoanAmount() {
        if (
            !jeonseDesiredAmountInput
            || !jeonseOwnFundAmountInput
            || !jeonseDesiredLoanAmountInput
        ) {
            return;
        }

        const desiredAmount = parseMoneyValue(
            jeonseDesiredAmountInput.value
        );
        const ownFundAmount = parseMoneyValue(
            jeonseOwnFundAmountInput.value
        );

        jeonseDesiredLoanAmountInput.value = Math.max(
            desiredAmount - ownFundAmount,
            0
        );
        formatMoneyInput(jeonseDesiredLoanAmountInput);
    }

    [jeonseDesiredAmountInput, jeonseOwnFundAmountInput].forEach(input => {
        input?.addEventListener("input", updateJeonseLoanAmount);
    });

    updateJeonseLoanAmount();

    function isSurveyControlIncomplete(control, form) {
        if (
            !control
            || control.dataset.optional === "true"
            || ["hidden", "button", "submit", "reset", "checkbox"].includes(control.type)
        ) {
            return false;
        }

        // Check if control is inside a hidden conditional subsection inside this form (e.g. #marriageCustomCostArea)
        let el = control;
        while (el && el !== form && el !== document.body) {
            if (el.hidden || el.style.display === "none") {
                return false;
            }
            el = el.parentElement;
        }

        if (control.disabled) {
            return false;
        }

        if (control.type === "radio") {
            const hasRequired = form.querySelector(`input[type="radio"][name="${control.name}"][required]`)
                || form.querySelector(`input[type="radio"][name="${control.name}"]`)?.hasAttribute("required");
            if (!hasRequired && !control.required && !control.hasAttribute("required")) {
                return false;
            }
            return !form.querySelector(
                `input[type="radio"][name="${control.name}"]:checked`
            );
        }

        const isRequired = control.required || control.hasAttribute("required");
        if (!isRequired) {
            return false;
        }

        const val = control.value;
        if (val === null || val === undefined || String(val).trim() === "") {
            return true;
        }

        if (control.type === "number" && control.hasAttribute("min")) {
            const num = Number(val);
            const min = Number(control.getAttribute("min"));
            if (isNaN(num) || num < min) {
                return true;
            }
        }

        return !control.checkValidity();
    }

    function isEventFormIncomplete(eventType) {
        const formArea = document.querySelector(`[data-event-form="${eventType}"]`);
        const form = formArea?.querySelector("form");
        if (!form) return false;

        const controls = Array.from(
            form.querySelectorAll("input, select, textarea")
        );
        return controls.some(control => isSurveyControlIncomplete(control, form));
    }

    function clearSurveyFieldError(group) {
        group.classList.remove("has-error");
        group.querySelector(
            ":scope > .lifecycle-field-error"
        )?.remove();
    }

    function showSurveyFieldError(group, message) {
        clearSurveyFieldError(group);
        group.classList.add("has-error");

        const error = document.createElement("small");
        error.className = "lifecycle-field-error";
        error.textContent = message;
        error.setAttribute("role", "alert");
        group.appendChild(error);
    }

    function validateSurveyForm(form) {
        const groups = Array.from(
            form.querySelectorAll(".lifecycle-form-group")
        );
        let firstInvalidControl = null;

        groups.forEach(group => {
            clearSurveyFieldError(group);

            const controls = Array.from(
                group.querySelectorAll(
                    "input, select, textarea"
                )
            );
            const invalidControl = controls.find(
                control => isSurveyControlIncomplete(control, form)
            );

            if (!invalidControl) {
                return;
            }

            firstInvalidControl ||= invalidControl;
            showSurveyFieldError(
                group,
                invalidControl.type === "radio"
                    ? "항목을 선택해주세요."
                    : "필수 항목을 입력해주세요."
            );
        });

        if (firstInvalidControl) {
            firstInvalidControl.focus();
            firstInvalidControl.closest(
                ".lifecycle-form-group"
            )?.scrollIntoView({
                behavior: "smooth",
                block: "center"
            });
        }

        return firstInvalidControl === null;
    }

    function validateAllPlacedEvents() {
        for (let i = 0; i < lifecycleEvents.length; i++) {
            const ev = lifecycleEvents[i];
            const formArea = document.querySelector(`[data-event-form="${ev.type}"]`);
            const form = formArea?.querySelector("form");
            if (form) {
                const isInvalid = isEventFormIncomplete(ev.type);
                if (isInvalid) {
                    openEventForm(ev.type, true);
                    renderFlowSequence(false);
                    updateEventSelectionUi();

                    const cardEl = document.querySelector(`.flow-step-card[data-event-type="${ev.type}"]`);
                    if (cardEl) {
                        cardEl.classList.add("has-error");
                    }

                    // Highlight all invalid fields in the opened form
                    validateSurveyForm(form);

                    alert(`[STEP ${i + 1}. ${ev.title}] 이벤트의 필수 항목을 모두 입력해주세요.`);
                    return ev;
                }
            }
        }
        return null;
    }

    document.addEventListener("click", event => {
        const saveButton = event.target.closest(
            ".lifecycle-form .lifecycle-primary-button"
        );

        if (!saveButton) {
            return;
        }

        const form = saveButton.closest("form");

        if (form && !validateSurveyForm(form)) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    }, true);

    document.querySelectorAll(
        ".lifecycle-form input, .lifecycle-form select, .lifecycle-form textarea"
    ).forEach(control => {
        ["input", "change"].forEach(eventName => {
            control.addEventListener(eventName, () => {
                const form = control.closest("form");
                const eventFormArea = control.closest("[data-event-form]");
                const eventType = eventFormArea?.dataset?.eventForm;

                const group = control.closest(".lifecycle-form-group");
                if (group) {
                    const isInvalid = isSurveyControlIncomplete(control, form);
                    if (!isInvalid) {
                        clearSurveyFieldError(group);
                    }
                }

                if (eventType) {
                    const cardEl = document.querySelector(`.flow-step-card[data-event-type="${eventType}"]`);
                    const isIncomplete = isEventFormIncomplete(eventType);
                    if (cardEl) {
                        cardEl.classList.toggle("has-error", isIncomplete);
                        const badge = cardEl.querySelector(".flow-step-error-badge, .flow-step-status-badge");
                        if (badge) {
                            badge.outerHTML = isIncomplete ? `
                                <span class="flow-step-error-badge">
                                    <span class="material-symbols-outlined">error</span>
                                    <span>필수 미입력</span>
                                </span>
                            ` : `
                                <span class="flow-step-status-badge">
                                    <span class="material-symbols-outlined">check_circle</span>
                                    <span>설정 완료</span>
                                </span>
                            `;
                        }
                    }
                    updateEventSelectionUi();
                }
            });
        });
    });


    function setBaseSurveyReady(ready) {
        baseSurveyReady = ready;
        stepButtons.forEach(button => {
            button.disabled = !ready || !scenarioId;
        });
    }

    function resetScenarioWorkspace() {
        selectedEventType = null;
        selectedEventTypes.clear();
        lifecycleEvents = [];
        Object.keys(savedEventIds).forEach(type => {
            savedEventIds[type] = null;
        });
        eventForms.forEach(formArea => {
            formArea.hidden = true;
            formArea.querySelector("form")?.reset();
        });
        updateEventSelectionUi();
        renderFlowSequence();
    }

    function updateScenarioUrl(selectedScenarioId) {
        const url = new URL(window.location.href);
        if (selectedScenarioId) {
            url.searchParams.set("scenarioId", selectedScenarioId);
        } else {
            url.searchParams.delete("scenarioId");
        }
        window.history.replaceState({}, "", url);
    }

    async function selectScenario(selectedScenarioId) {
        const response = await fetch(`/api/lifecycle/scenarios/${selectedScenarioId}`);
        if (!response.ok) {
            throw new Error(`시나리오 조회 실패: ${response.status}`);
        }

        const scenario = await response.json();
        scenarioId = Number(getResponseField(scenario, "scenarioId"));
        scenarioName = getResponseField(scenario, "scenarioName");
        updateScenarioUrl(scenarioId);
        resetScenarioWorkspace();
        if (activeScenarioName) {
            activeScenarioName.textContent = scenarioName;
        }
        if (scenarioGate) {
            scenarioGate.hidden = true;
        }
        setBaseSurveyReady(true);
        await loadTimelineEvents();
        showStep("events");
    }

    function scenarioStatusLabel(status) {
        return {
            DRAFT: "작성 중",
            ACTIVE: "진행 중",
            COMPLETED: "입력 완료"
        }[status] ?? status;
    }

    function renderScenarioList(scenarios) {
        if (!scenarioList) {
            return;
        }
        if (scenarios.length === 0) {
            scenarioList.innerHTML = '<p class="lifecycle-scenario-empty">이전에 진행한 시나리오가 없습니다.</p>';
            return;
        }

        scenarioList.innerHTML = scenarios.map(scenario => {
            const itemId = getResponseField(scenario, "scenarioId");
            const itemStatus = getResponseField(scenario, "status");
            const itemName = getResponseField(scenario, "scenarioName");
            const eventCount = getResponseField(scenario, "eventCount") ?? 0;
            const baseDate = getResponseField(scenario, "baseDate") ?? "-";
            return `
            <article class="lifecycle-scenario-card">
                <div>
                    <span>${escapeHtml(scenarioStatusLabel(itemStatus))}</span>
                    <strong>${escapeHtml(itemName)}</strong>
                    <small>생활 이벤트 ${Number(eventCount)}개 · 기준일 ${escapeHtml(baseDate)}</small>
                </div>
                <div class="lifecycle-scenario-card-actions">
                    <button type="button" class="lifecycle-primary-button"
                            data-select-scenario="${itemId}">
                        ${itemStatus === "COMPLETED" ? "내용 보기" : "이어서 작성"}
                    </button>
                    <button type="button" class="lifecycle-secondary-button"
                            data-archive-scenario="${itemId}">삭제</button>
                </div>
            </article>
        `;
        }).join("");

        scenarioList.querySelectorAll("[data-select-scenario]").forEach(button => {
            button.addEventListener("click", async () => {
                button.disabled = true;
                try {
                    await selectScenario(button.dataset.selectScenario);
                } catch (error) {
                    console.error(error);
                    alert("시나리오를 불러오지 못했습니다.");
                } finally {
                    button.disabled = false;
                }
            });
        });

        scenarioList.querySelectorAll("[data-archive-scenario]").forEach(button => {
            button.addEventListener("click", async () => {
                if (!window.confirm("이 시나리오를 삭제하시겠습니까?")) {
                    return;
                }
                const archivedId = Number(button.dataset.archiveScenario);
                const response = await fetch(`/api/lifecycle/scenarios/${archivedId}`, {
                    method: "DELETE"
                });
                if (!response.ok) {
                    alert("시나리오를 삭제하지 못했습니다.");
                    return;
                }
                if (scenarioId === archivedId) {
                    scenarioId = null;
                    scenarioName = null;
                    updateScenarioUrl(null);
                    resetScenarioWorkspace();
                    if (activeScenarioName) {
                        activeScenarioName.textContent = "선택된 시나리오 없음";
                    }
                    setBaseSurveyReady(true);
                }
                await loadScenarioList();
            });
        });
    }

    async function loadScenarioList() {
        const response = await fetch("/api/lifecycle/scenarios");
        if (!response.ok) {
            throw new Error(`시나리오 목록 조회 실패: ${response.status}`);
        }
        renderScenarioList(await response.json());
    }

    async function createScenario() {
        const name = newScenarioNameInput?.value.trim();
        if (!name) {
            alert("새 시나리오 이름을 입력해주세요.");
            newScenarioNameInput?.focus();
            return;
        }

        createScenarioButton.disabled = true;
        try {
            const response = await fetch("/api/lifecycle/scenarios", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ scenarioName: name })
            });
            if (!response.ok) {
                throw new Error(`시나리오 생성 실패: ${response.status}`);
            }
            const scenario = await response.json();
            newScenarioNameInput.value = "";
            await selectScenario(getResponseField(scenario, "scenarioId"));
        } catch (error) {
            console.error(error);
            alert("새 시나리오를 만들지 못했습니다.");
        } finally {
            createScenarioButton.disabled = false;
        }
    }

    createScenarioButton?.addEventListener("click", createScenario);
    newScenarioNameInput?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            createScenario();
        }
    });
    function setScenarioListExpanded(expanded) {
        scenarioList.hidden = !expanded;
        toggleScenarioListButton.setAttribute(
            "aria-expanded",
            String(expanded)
        );
        toggleScenarioListButton.classList.toggle("expanded", expanded);
    }

    toggleScenarioListButton?.addEventListener("click", async () => {
        const willOpen = scenarioList.hidden;
        setScenarioListExpanded(willOpen);
        if (willOpen) {
            try {
                await loadScenarioList();
            } catch (error) {
                console.error(error);
                alert("이전 시나리오를 불러오지 못했습니다.");
            }
        }
    });
    changeScenarioButton?.addEventListener("click", async () => {
        const willOpen = scenarioGate.hidden;
        scenarioGate.hidden = !willOpen;
        if (willOpen) {
            setScenarioListExpanded(true);
            try {
                await loadScenarioList();
            } catch (error) {
                console.error(error);
            }
            scenarioGate.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    });

    /*
     * =========================================================
     * 2. STEP 화면 이동
     * =========================================================
     */

    /**
     * 원하는 STEP 화면을 표시한다.
     *
     * events -> 생활 이벤트 계획
     * review -> 입력 확인
     * result -> 결과 보기
     */
    /**
     * 원하는 STEP 화면을 표시한다.
     *
     * events -> 생활 이벤트 계획
     * review -> 입력 확인
     * result -> 결과 보기
     */
    async function showStep(stepName) {
        if (!stepName || stepName === "base") {
            stepName = "events";
        }

        if (stepName !== "events" && !scenarioId) {
            alert("진행할 시나리오를 먼저 선택하거나 새로 만들어주세요.");
            stepName = "events";
            if (scenarioGate) {
                scenarioGate.hidden = false;
                setScenarioListExpanded(true);
            }
        }

        // review나 result로 넘어갈 때, 타임라인에 배치된 모든 이벤트를 유효성 검사 및 자동 저장
        if (stepName === "review" || stepName === "result") {
            if (lifecycleEvents.length === 0) {
                alert("최소 1개 이상의 생애 이벤트를 순서에 배치해주세요.");
                return;
            }

            const invalidEvent = validateAllPlacedEvents();
            if (invalidEvent) {
                return;
            }

            const saveOk = await autoSaveAllPlacedEvents();
            if (!saveOk) {
                return;
            }
        }

        // 모든 STEP 패널 숨김/표시
        stepPanels.forEach(panel => {
            panel.hidden = panel.dataset.stepPanel !== stepName;
        });

        // 상단 STEP 버튼 active 처리
        stepButtons.forEach(button => {
            button.classList.toggle(
                "active",
                button.dataset.step === stepName
            );
        });

        // review 화면 진입 시 요약 갱신
        if (stepName === "review") {
            loadReview();
        }

        if (stepName === "result") {
            if (latestSimulationResult) {
                renderSimulationResult(latestSimulationResult);
            } else {
                loadSavedSimulationResult();
            }
        }

        const activePanel = document.querySelector(
            `[data-step-panel="${stepName}"]`
        );

        if (activePanel) {
            requestAnimationFrame(() => {
                activePanel.scrollIntoView({
                    behavior: "smooth",
                    block: "start"
                });
            });
        }
    }


    /*
     * 상단 STEP 버튼 클릭
     */
    stepButtons.forEach(button => {

        button.addEventListener("click", () => {

            showStep(button.dataset.step);
        });
    });


    /*
     * 이전 / 다음 버튼 클릭
     */
    stepMoveButtons.forEach(button => {

        button.addEventListener("click", () => {

            showStep(button.dataset.goStep);
        });
    });


    /*
     * =========================================================
     * 3. 기본 생활정보
     * =========================================================
     */

    const saveBaseSurveyBtn =
        document.getElementById("saveBaseSurveyBtn");


    /*
     * 급여 상승 시나리오
     */
    const salaryScenarioRadios =
        document.querySelectorAll(
            'input[name="salaryGrowthScenario"]'
        );

    const customSalaryGrowthArea =
        document.getElementById(
            "customSalaryGrowthArea"
        );

    const customSalaryGrowthRate =
        document.getElementById(
            "customSalaryGrowthRate"
        );

    const baseSurveyRequiredFieldIds = [
        "monthlyLivingExpense",
        "currentHousingType",
        "monthlyHousingExpense",
        "industryCode",
        "customSalaryGrowthRate"
    ];

    function checkBaseSurveyComplete() {

        if (!saveBaseSurveyBtn) {
            return false;
        }

        const monthlyLivingExpense =
            document.getElementById("monthlyLivingExpense");
        const currentHousingType =
            document.getElementById("currentHousingType");
        const monthlyHousingExpense =
            document.getElementById("monthlyHousingExpense");
        const industryCode =
            document.getElementById("industryCode");
        const salaryScenario =
            document.querySelector(
                'input[name="salaryGrowthScenario"]:checked'
            );

        let complete =
            monthlyLivingExpense.value !== ""
            && monthlyLivingExpense.checkValidity()
            && currentHousingType.value !== ""
            && monthlyHousingExpense.value !== ""
            && monthlyHousingExpense.checkValidity()
            && industryCode.value !== ""
            && salaryScenario !== null;

        if (
            salaryScenario
            && salaryScenario.value === "CUSTOM"
        ) {
            complete =
                complete
                && customSalaryGrowthRate.value !== ""
                && customSalaryGrowthRate.checkValidity();
        }

        saveBaseSurveyBtn.hidden = !complete;
        return complete;
    }

    baseSurveyRequiredFieldIds.forEach(id => {

        const element = document.getElementById(id);

        if (!element) {
            return;
        }

        element.addEventListener(
            "input",
            checkBaseSurveyComplete
        );
        element.addEventListener(
            "change",
            checkBaseSurveyComplete
        );
    });


    /*
     * CUSTOM 선택 시에만
     * 직접 입력 영역 표시
     */
    salaryScenarioRadios.forEach(radio => {

        radio.addEventListener("change", () => {

            const selected =
                document.querySelector(
                    'input[name="salaryGrowthScenario"]:checked'
                );

            if (!selected) {
                return;
            }

            if (selected.value === "CUSTOM") {

                customSalaryGrowthArea.hidden = false;

            } else {

                customSalaryGrowthArea.hidden = true;

                if (customSalaryGrowthRate) {
                    customSalaryGrowthRate.value = "";
                }
            }

            checkBaseSurveyComplete();
        });
    });


    /**
     * 기본 생활정보 저장
     */
    async function saveBaseSurvey() {

        const monthlyLivingExpense =
            document.getElementById(
                "monthlyLivingExpense"
            ).value;

        const currentHousingType =
            document.getElementById(
                "currentHousingType"
            ).value;

        const monthlyHousingExpense =
            document.getElementById(
                "monthlyHousingExpense"
            ).value;

        const industryCode =
            document.getElementById(
                "industryCode"
            ).value;

        const salaryScenario =
            document.querySelector(
                'input[name="salaryGrowthScenario"]:checked'
            );


        /*
         * 필수값 검사
         */
        if (!monthlyLivingExpense) {

            alert("현재 월평균 생활비를 입력해주세요.");
            return;
        }

        if (!currentHousingType) {

            alert("현재 주거형태를 선택해주세요.");
            return;
        }

        if (monthlyHousingExpense === "") {

            alert("현재 월 주거비를 입력해주세요.");
            return;
        }

        if (!salaryScenario) {

            alert("미래 소득 상승 가정을 선택해주세요.");
            return;
        }

        if (!industryCode) {

            alert("현재 종사 산업군을 선택해주세요.");
            return;
        }


        /*
         * CUSTOM을 선택한 경우
         * 직접 입력 상승률 필수
         */
        if (
            salaryScenario.value === "CUSTOM"
            && !customSalaryGrowthRate.value
        ) {

            alert(
                "예상 연평균 소득 상승률을 입력해주세요."
            );

            return;
        }


        /*
         * 서버에 전달할 Request DTO
         *
         * LifecycleBaseSurveyRequest와
         * 필드명이 동일해야 한다.
         */
        const requestData = {

            monthlyLivingExpense:
                parseMoneyValue(monthlyLivingExpense),

            currentHousingType:
            currentHousingType,

            monthlyHousingExpense:
                parseMoneyValue(monthlyHousingExpense),

            industryCode:
                industryCode || null,

            salaryGrowthScenario:
            salaryScenario.value,

            /*
             * 화면에서는 %
             * DB에서는 소수 비율 사용
             *
             * 예:
             * 사용자 입력 3%
             * -> 서버 전달 0.03
             */
            customSalaryGrowthRate:
                salaryScenario.value === "CUSTOM"
                    ? Number(customSalaryGrowthRate.value) / 100
                    : null
        };


        try {

            const response = await fetch(
                "/api/lifecycle/survey/base",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify(requestData)
                }
            );


            if (!response.ok) {

                throw new Error(
                    `기본정보 저장 실패: ${response.status}`
                );
            }


            alert("기본 생활정보가 저장되었습니다.");
            setBaseSurveyReady(true);
            await loadScenarioList();
            setScenarioListExpanded(false);
            scenarioGate.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });


        } catch (error) {

            console.error(error);

            alert(
                "기본 생활정보 저장 중 오류가 발생했습니다."
            );
        }
    }


    /*
     * 기본정보 저장 버튼
     */
    if (saveBaseSurveyBtn) {

        saveBaseSurveyBtn.addEventListener(
            "click",
            saveBaseSurvey
        );
    }


    /**
     * 저장된 기본 생활정보 조회
     *
     * 사용자가 다시 설문 화면에 들어왔을 때
     * 기존 데이터를 폼에 다시 채운다.
     */
    async function loadBaseSurvey() {

        try {

            const response = await fetch(
                "/api/lifecycle/survey/base"
            );


            /*
             * 아직 기본설문이 없는 사용자라면
             * 아무 처리하지 않는다.
             */
            if (response.status === 404) {
                setBaseSurveyReady(false);
                checkBaseSurveyComplete();
                return false;
            }


            if (!response.ok) {

                throw new Error(
                    `기본정보 조회 실패: ${response.status}`
                );
            }


            const data = await response.json();


            if (!data) {
                return;
            }


            /*
             * 기존 저장값을 화면에 표시
             */
            document.getElementById(
                "monthlyLivingExpense"
            ).value =
                data.monthlyLivingExpense ?? "";


            document.getElementById(
                "currentHousingType"
            ).value =
                data.currentHousingType ?? "";


            document.getElementById(
                "monthlyHousingExpense"
            ).value =
                data.monthlyHousingExpense ?? "";


            document.getElementById(
                "industryCode"
            ).value =
                data.industryCode ?? "";


            /*
             * 급여 시나리오 선택
             */
            if (data.salaryGrowthScenario) {
                salaryScenarioRadios.forEach(radio => {
                    radio.checked = radio.value === data.salaryGrowthScenario;
                });
            }

            if (data.salaryGrowthScenario === "CUSTOM") {
                customSalaryGrowthArea.hidden = false;
                if (data.customSalaryGrowthRate != null) {
                    customSalaryGrowthRate.value = Number(data.customSalaryGrowthRate) * 100;
                }
            } else {
                customSalaryGrowthArea.hidden = true;
            }

            moneyInputs.forEach(formatMoneyInput);
            const complete = checkBaseSurveyComplete();
            setBaseSurveyReady(complete);

            return complete;
        } catch (error) {
            console.error("기본 생활정보 조회 오류", error);
            setBaseSurveyReady(false);
            return false;
        }
    }

    /*
     * =========================================================
     * 4. 이벤트 선택
     * =========================================================
     */

    const lifecycleEventSubtitles = {
        marriage: "예식 · 혼수 · 신혼여행",
        childbirth: "출산 · 양육 · 지원제도",
        vehicle: "차량가격 · 대출 · 유지비",
        "monthly-rent": "보증금 · 월세 · 관리비",
        jeonse: "전세금 · 자기자금 · 대출",
        "home-purchase": "주택가격 · 자기자금 · 주담대",
        repayment: "부분상환 · 추가상환 · 전액상환"
    };

    function getEventIcon(eventType) {
        return {
            marriage: "favorite",
            childbirth: "child_care",
            vehicle: "directions_car",
            "monthly-rent": "apartment",
            jeonse: "key",
            "home-purchase": "home",
            repayment: "payments"
        }[eventType] ?? "event";
    }

    function getSequentialDate(stepIndex) {
        const year = START_YEAR + stepIndex;
        return `${year}-01-01`;
    }

    function syncStepDatesToFormsAndEvents() {
        lifecycleEvents.forEach((ev, idx) => {
            ev.targetDate = getSequentialDate(idx);
            const dateInput = document.querySelector(
                `[data-event-form="${ev.type}"] input[name="targetDate"]`
            );
            if (dateInput) {
                dateInput.value = `${START_YEAR + idx}-01`;
            }
        });
    }

    function openEventForm(eventType, shouldScroll = false) {
        selectedEventType = eventType;
        selectedEventTypes.add(eventType);

        if (eventType === "repayment") {
            loadUserLoansForRepayment();
        }

        eventForms.forEach(form => {
            form.hidden = true;
        });

        const targetForm = document.querySelector(
            `[data-event-form="${eventType}"]`
        );

        if (targetForm) {
            targetForm.hidden = false;
        }

        updateEventSelectionUi();
        renderFlowSequence(false);

        if (shouldScroll && targetForm) {
            requestAnimationFrame(() => {
                targetForm.scrollIntoView({
                    behavior: "smooth",
                    block: "start"
                });
            });
        }
    }

    function updateEventSelectionUi() {
        const paletteCards = document.querySelectorAll("#scenarioCardPalette .lifecycle-event-card");
        paletteCards.forEach(card => {
            const type = card.dataset.eventType;
            const stepIdx = lifecycleEvents.findIndex(e => e.type === type);
            const isPlaced = stepIdx >= 0;
            const isIncomplete = isPlaced && isEventFormIncomplete(type);
            const badge = card.querySelector(`[data-placement-status="${type}"]`);

            card.classList.toggle("selected", isPlaced);
            card.classList.toggle("has-error", isIncomplete);
            if (badge) {
                badge.classList.toggle("is-placed", isPlaced);
                const textSpan = badge.querySelector(".status-text");
                if (textSpan) {
                    if (isPlaced) {
                        textSpan.textContent = `STEP ${stepIdx + 1}`;
                    } else {
                        textSpan.textContent = "미배치";
                    }
                }
            }
        });
    }

    async function removeEvent(eventType) {
        const eventId = savedEventIds[eventType];

        if (eventId) {
            const confirmed = window.confirm(
                `${lifecycleEventNames[eventType]} 이벤트를 순서에서 제거하시겠습니까?\n저장된 세부 설정 내용도 함께 삭제됩니다.`
            );

            if (!confirmed) {
                openEventForm(eventType, false);
                return;
            }

            const response = await fetch(
                `/api/lifecycle/survey/event/${eventId}`,
                { method: "DELETE" }
            );

            if (!response.ok) {
                throw new Error(`이벤트 삭제 실패: ${response.status}`);
            }
        }

        selectedEventTypes.delete(eventType);
        savedEventIds[eventType] = null;
        lifecycleEvents = lifecycleEvents.filter(
            event => event.type !== eventType
        );

        const formArea = document.querySelector(
            `[data-event-form="${eventType}"]`
        );
        const form = formArea?.querySelector("form");
        form?.reset();

        if (formArea) {
            formArea.hidden = true;
        }
        if (selectedEventType === eventType) {
            selectedEventType = null;
        }

        updateEventSelectionUi();
        renderFlowSequence(false);
    }

    function moveEventInFlow(fromIndex, toIndex) {
        if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0) return;
        const [moved] = lifecycleEvents.splice(fromIndex, 1);
        if (!moved) return;
        const safeIndex = Math.max(0, Math.min(lifecycleEvents.length, toIndex));
        lifecycleEvents.splice(safeIndex, 0, moved);
        syncStepDatesToFormsAndEvents();
        renderFlowSequence(false);
        updateEventSelectionUi();
        openEventForm(moved.type, false);
    }

    function insertEventInFlow(type, targetIndex) {
        const existingIdx = lifecycleEvents.findIndex(e => e.type === type);
        let eventObj = {
            type,
            title: lifecycleEventNames[type] ?? type,
            eventId: savedEventIds[type]
        };
        if (existingIdx >= 0) {
            eventObj = lifecycleEvents.splice(existingIdx, 1)[0];
        }
        selectedEventTypes.add(type);
        const safeIndex = Math.max(0, Math.min(lifecycleEvents.length, targetIndex));
        lifecycleEvents.splice(safeIndex, 0, eventObj);
        syncStepDatesToFormsAndEvents();
        renderFlowSequence(false);
        updateEventSelectionUi();
        openEventForm(type, false);
    }

    function addEventToEndOfFlow(type) {
        insertEventInFlow(type, lifecycleEvents.length);
    }

    function addOrUpdateTimelineEvent(type, targetDate = null, eventId = null) {
        const index = lifecycleEvents.findIndex(event => event.type === type);
        const timelineEvent = {
            type,
            targetDate: targetDate || getSequentialDate(index >= 0 ? index : lifecycleEvents.length),
            title: lifecycleEventNames[type] ?? type,
            eventId
        };

        if (index >= 0) {
            lifecycleEvents[index] = timelineEvent;
        } else {
            lifecycleEvents.push(timelineEvent);
        }

        syncStepDatesToFormsAndEvents();
        renderFlowSequence(false);
    }

    function renderFlowSequence(shouldScroll = false) {
        const stream = document.getElementById("lifecycleTimeline");
        const empty = document.getElementById("lifecycleTimelineEmpty");

        if (!stream || !empty) {
            return;
        }

        syncStepDatesToFormsAndEvents();

        if (lifecycleEvents.length === 0) {
            empty.hidden = false;
            stream.hidden = true;
            stream.replaceChildren();
            return;
        }

        empty.hidden = true;
        stream.hidden = false;
        stream.innerHTML = lifecycleEvents.map((event, index) => {
            const stepNumber = index + 1;
            const isLast = index === lifecycleEvents.length - 1;
            const isActive = selectedEventType === event.type ? "active" : "";
            const isIncomplete = isEventFormIncomplete(event.type);
            const errorClass = isIncomplete ? "has-error" : "";
            const icon = getEventIcon(event.type);

            return `
                <div class="flow-step-item" data-step-index="${index}">
                    <div class="flow-step-card ${isActive} ${errorClass}"
                         draggable="true"
                         data-step-index="${index}"
                         data-event-type="${event.type}"
                         tabindex="0"
                         role="button"
                         title="${escapeHtml(event.title)} (STEP ${stepNumber}) - 드래그하여 순서 변경">
                        <div class="flow-step-card-header">
                            <span class="flow-step-badge">STEP ${stepNumber}</span>
                            <button type="button"
                                    class="flow-step-remove-btn"
                                    data-remove-event="${event.type}"
                                    title="순서에서 제거"
                                    aria-label="삭제">✕</button>
                        </div>
                        <span class="flow-step-icon material-symbols-outlined" data-event-type="${event.type}">${icon}</span>
                        <div class="flow-step-info">
                            <strong class="flow-step-title">${escapeHtml(event.title)}</strong>
                            <small class="flow-step-subtitle">${escapeHtml(lifecycleEventSubtitles[event.type] || "생애 이벤트")}</small>
                        </div>
                        ${isIncomplete ? `
                            <span class="flow-step-error-badge">
                                <span class="material-symbols-outlined">error</span>
                                <span>필수 미입력</span>
                            </span>
                        ` : `
                            <span class="flow-step-status-badge">
                                <span class="material-symbols-outlined">check_circle</span>
                                <span>설정 완료</span>
                            </span>
                        `}
                    </div>
                    ${!isLast ? `
                        <div class="flow-connector" aria-hidden="true">
                            <span class="material-symbols-outlined">arrow_forward</span>
                        </div>
                    ` : ''}
                </div>
            `;
        }).join("");

        // Flow cards drag & click listeners
        stream.querySelectorAll(".flow-step-card").forEach(card => {
            const type = card.dataset.eventType;
            const fromIndex = Number(card.dataset.stepIndex);

            card.addEventListener("dragstart", e => {
                card.classList.add("is-dragging");
                const payload = JSON.stringify({ type, fromIndex, isFromFlow: true });
                e.dataTransfer.setData("text/plain", payload);
                e.dataTransfer.setData("application/json", payload);
                e.dataTransfer.effectAllowed = "move";
            });

            card.addEventListener("dragend", () => {
                card.classList.remove("is-dragging");
                const track = document.getElementById("timelineDropTrack");
                if (track) track.classList.remove("is-over");
                stream.querySelectorAll(".flow-step-item").forEach(item => {
                    item.classList.remove("drop-before", "drop-after");
                });
            });

            card.addEventListener("click", e => {
                if (e.target.closest("[data-remove-event]")) return;
                openEventForm(type, false);
            });

            card.addEventListener("keydown", e => {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    openEventForm(type, false);
                }
            });
        });

        // Drop on flow items to re-order (Left half = insert before, Right half = insert after)
        stream.querySelectorAll(".flow-step-item").forEach(item => {
            const targetIndex = Number(item.dataset.stepIndex);

            item.addEventListener("dragover", e => {
                e.preventDefault();
                e.stopPropagation();
                e.dataTransfer.dropEffect = "move";

                const rect = item.getBoundingClientRect();
                const isLeftHalf = (e.clientX - rect.left) < (rect.width / 2);

                if (isLeftHalf) {
                    item.classList.add("drop-before");
                    item.classList.remove("drop-after");
                } else {
                    item.classList.add("drop-after");
                    item.classList.remove("drop-before");
                }
            });

            item.addEventListener("dragleave", () => {
                item.classList.remove("drop-before", "drop-after");
            });

            item.addEventListener("drop", e => {
                e.preventDefault();
                e.stopPropagation();
                item.classList.remove("drop-before", "drop-after");
                const track = document.getElementById("timelineDropTrack");
                if (track) track.classList.remove("is-over");

                let data;
                try {
                    const raw = e.dataTransfer.getData("application/json") || e.dataTransfer.getData("text/plain");
                    data = JSON.parse(raw);
                } catch {
                    return;
                }

                if (!data || !data.type) return;

                const rect = item.getBoundingClientRect();
                const isLeftHalf = (e.clientX - rect.left) < (rect.width / 2);
                let insertIndex = isLeftHalf ? targetIndex : targetIndex + 1;

                if (data.isFromFlow && typeof data.fromIndex === "number") {
                    if (data.fromIndex < insertIndex) {
                        insertIndex--;
                    }
                    moveEventInFlow(data.fromIndex, insertIndex);
                } else {
                    insertEventInFlow(data.type, insertIndex);
                }
            });
        });

        // Remove buttons
        stream.querySelectorAll("[data-remove-event]").forEach(btn => {
            btn.addEventListener("click", async e => {
                e.stopPropagation();
                const type = btn.dataset.removeEvent;
                await removeEvent(type);
            });
        });
    }

    function initDragAndDrop() {
        const dropTrack = document.getElementById("timelineDropTrack");
        const paletteCards = document.querySelectorAll("#scenarioCardPalette .lifecycle-event-card");

        paletteCards.forEach(card => {
            const type = card.dataset.eventType;

            card.addEventListener("dragstart", e => {
                card.classList.add("is-dragging");
                const payload = JSON.stringify({ type, isFromPalette: true });
                e.dataTransfer.setData("text/plain", payload);
                e.dataTransfer.setData("application/json", payload);
                e.dataTransfer.effectAllowed = "copyMove";
            });

            card.addEventListener("dragend", () => {
                card.classList.remove("is-dragging");
                if (dropTrack) dropTrack.classList.remove("is-over");
            });

            card.addEventListener("click", () => {
                if (selectedEventTypes.has(type)) {
                    openEventForm(type, false);
                } else {
                    addEventToEndOfFlow(type);
                }
            });
        });

        if (!dropTrack) return;

        dropTrack.addEventListener("dragenter", e => {
            e.preventDefault();
            dropTrack.classList.add("is-over");
        });

        dropTrack.addEventListener("dragover", e => {
            e.preventDefault();
            e.dataTransfer.dropEffect = "move";
            dropTrack.classList.add("is-over");
        });

        dropTrack.addEventListener("dragleave", e => {
            if (!dropTrack.contains(e.relatedTarget)) {
                dropTrack.classList.remove("is-over");
            }
        });

        dropTrack.addEventListener("drop", e => {
            e.preventDefault();
            dropTrack.classList.remove("is-over");

            let data;
            try {
                const raw = e.dataTransfer.getData("application/json") || e.dataTransfer.getData("text/plain");
                data = JSON.parse(raw);
            } catch {
                return;
            }

            if (!data || !data.type) return;

            if (data.isFromFlow && typeof data.fromIndex === "number") {
                moveEventInFlow(data.fromIndex, lifecycleEvents.length - 1);
            } else {
                addEventToEndOfFlow(data.type);
            }
        });
    }

    function getResponseField(data, fieldName) {
        if (Object.hasOwn(data, fieldName)) {
            return data[fieldName];
        }
        const snakeCaseName = fieldName.replace(
            /[A-Z]/g,
            letter => `_${letter.toLowerCase()}`
        );
        return data[snakeCaseName];
    }

    function populateEventForm(eventType, data) {
        const formArea = document.querySelector(
            `[data-event-form="${eventType}"]`
        );
        const form = formArea?.querySelector("form");
        if (!form) {
            return;
        }

        const sidoSelect = form.querySelector("[data-region-sido]");
        const sigunguValue = getResponseField(data, "regionSigungu") ?? "";
        if (sidoSelect) {
            sidoSelect.value = getResponseField(data, "regionSido") ?? "";
            updateSigunguOptions(sidoSelect, sigunguValue);
        }

        form.querySelectorAll("input, select, textarea").forEach(control => {
            if (!control.name || control.matches("[data-region-sigungu]")) {
                return;
            }
            const fieldName = lifestyleFieldNames.has(control.name)
                ? "lifestyleLevel"
                : control.name;
            let value = getResponseField(data, fieldName);

            if (control.name === "targetDate") {
                value = toYearMonth(value);
            } else if (control.name === "userContributionRate") {
                value = Number(value) * 100;
            }
            if (value === undefined || value === null) {
                return;
            }
            if (control.type === "radio") {
                control.checked = control.value === String(value);
            } else if (control.type === "checkbox") {
                control.checked = Boolean(value);
            } else {
                control.value = value;
            }
        });

        form.querySelectorAll('input[type="radio"]:checked').forEach(radio => {
            radio.dispatchEvent(new Event("change", { bubbles: true }));
        });
        moneyInputs.filter(input => form.contains(input)).forEach(formatMoneyInput);
        if (form.id === "vehicleSurveyForm") {
            updateVehicleFinancingAmounts();
        }
        if (form.id === "jeonseSurveyForm") {
            updateJeonseLoanAmount();
        }
    }

    async function loadEventDetails(events) {
        await Promise.all(events.map(async event => {
            const type = lifecycleEventTypes[
                getResponseField(event, "eventType")
            ];
            const eventId = getResponseField(event, "eventId");
            const apiPath = lifecycleEventDetailPaths[type];
            if (!type || !apiPath) {
                return;
            }
            const response = await fetch(
                `/api/lifecycle/survey/${apiPath}/${eventId}`
            );
            if (!response.ok) {
                console.error(`이벤트 상세 조회 실패: ${type} ${response.status}`);
                return;
            }
            populateEventForm(type, await response.json());
        }));
    }

    async function loadTimelineEvents() {
        const currentScenarioId = await ensureScenario();
        const response = await fetch(
            `/api/lifecycle/survey/scenario/${currentScenarioId}/timeline`
        );

        if (!response.ok) {
            throw new Error(`타임라인 조회 실패: ${response.status}`);
        }

        const events = await response.json();
        lifecycleEvents = events.flatMap(event => {
            const type = lifecycleEventTypes[
                getResponseField(event, "eventType")
            ];
            const eventId = getResponseField(event, "eventId");
            const targetDate = toYearMonth(
                getResponseField(event, "targetDate")
            );

            if (!type || !targetDate) {
                return [];
            }

            savedEventIds[type] = eventId;
            selectedEventTypes.add(type);

            const targetDateInput = document.querySelector(
                `[data-event-form="${type}"] input[name="targetDate"]`
            );
            if (targetDateInput) {
                targetDateInput.value = targetDate;
            }

            return [{
                type,
                targetDate,
                title: lifecycleEventNames[type] ?? type,
                eventId
            }];
        }).sort((a, b) => a.targetDate.localeCompare(b.targetDate));

        updateEventSelectionUi();
        renderFlowSequence();
        await loadEventDetails(events);
    }

    const eventApiPaths = {
        marriage: "marriage",
        childbirth: "childbirth",
        vehicle: "vehicle",
        "monthly-rent": "monthly-rent",
        jeonse: "jeonse",
        "home-purchase": "home-purchase",
        repayment: "repayment"
    };
    const lifestyleFieldNames = new Set([
        "childbirthLifestyleLevel",
        "monthlyRentLifestyleLevel",
        "jeonseLifestyleLevel",
        "homePurchaseLifestyleLevel"
    ]);
    const numericFieldNames = new Set([
        "childOrder", "desiredArea", "loanPeriodMonths", "loanAccountId"
    ]);

    function buildEventRequest(form) {
        const request = {};
        const controls = Array.from(
            form.querySelectorAll("input, select, textarea")
        );

        controls.forEach(control => {
            if (!control.name || control.disabled) {
                return;
            }

            if (control.type === "radio" && !control.checked) {
                return;
            }

            const fieldName = lifestyleFieldNames.has(control.name)
                ? "lifestyleLevel"
                : control.name;

            if (control.type === "checkbox") {
                request[fieldName] = control.checked;
            } else if (moneyInputs.includes(control)) {
                request[fieldName] = parseMoneyValue(control.value);
            } else if (numericFieldNames.has(fieldName)) {
                request[fieldName] = control.value === ""
                    ? null
                    : Number(control.value);
            } else if (control.type === "month") {
                request[fieldName] = toServerDate(control.value);
            } else {
                request[fieldName] = control.value === ""
                    ? null
                    : control.value;
            }
        });

        return request;
    }

    async function saveEventSurvey(eventType, form, button, isSilent = false) {
        if (!form) return false;
        if (button) button.disabled = true;

        const currentScenarioId = await ensureScenario();
        const apiPath = eventApiPaths[eventType] || eventType;
        const eventId = savedEventIds[eventType];
        const url = eventId
            ? `/api/lifecycle/survey/${apiPath}/${eventId}`
            : `/api/lifecycle/survey/scenario/${currentScenarioId}/${apiPath}`;

        try {
            const requestBody = buildEventRequest(form);
            // 기본값 안전장치 (타겟 날짜 등이 비어있을 경우)
            if (!requestBody.targetDate) {
                const targetIdx = lifecycleEvents.findIndex(e => e.type === eventType);
                requestBody.targetDate = getSequentialDate(targetIdx >= 0 ? targetIdx : 0);
            }

            const response = await fetch(url, {
                method: eventId ? "PUT" : "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(
                    `이벤트 저장 실패: ${response.status}`
                );
            }

            if (!eventId) {
                savedEventIds[eventType] = await response.json();
            }

            selectedEventTypes.add(eventType);
            const targetDate = form.querySelector(
                'input[name="targetDate"]'
            )?.value;
            addOrUpdateTimelineEvent(
                eventType,
                targetDate,
                savedEventIds[eventType]
            );

            const cardEl = document.querySelector(`.flow-step-card[data-event-type="${eventType}"]`);
            if (cardEl) {
                cardEl.classList.remove("has-error");
            }

            if (button) button.textContent = "저장 완료";
            if (!isSilent) {
                alert(`${lifecycleEventNames[eventType] || "이벤트"} 계획이 저장되었습니다.`);
            }
            return true;
        } catch (error) {
            console.error(error);

            // Focus on failed scenario / event
            const cardEl = document.querySelector(`.flow-step-card[data-event-type="${eventType}"]`);
            if (cardEl) {
                cardEl.classList.add("has-error");
            }
            openEventForm(eventType, true);

            if (!isSilent) {
                alert(`[${lifecycleEventNames[eventType] || "이벤트"}] 계획 저장 중 오류가 발생했습니다. 입력 정보를 확인해주세요.`);
            }
            return false;
        } finally {
            if (button) button.disabled = false;
        }
    }

    Object.keys(eventApiPaths).forEach(eventType => {
        const formArea = document.querySelector(
            `[data-event-form="${eventType}"]`
        );
        const form = formArea?.querySelector("form");
        const saveButton = form?.querySelector(
            ".lifecycle-primary-button"
        );

        if (form && saveButton) {
            saveButton.addEventListener("click", () => {
                saveEventSurvey(eventType, form, saveButton, false);
            });
        }
    });


    /*
     * =========================================================
     * 5. 결혼 설문
     * =========================================================
     */

    const saveMarriageSurveyBtn =
        document.getElementById(
            "saveMarriageSurveyBtn"
        );


    /*
     * 결혼 LifestyleLevel
     */
    const marriageLifestyleRadios =
        document.querySelectorAll(
            'input[name="marriageLifestyleLevel"]'
        );

    const marriageCustomCostArea =
        document.getElementById(
            "marriageCustomCostArea"
        );

    const marriageCustomEstimatedCost =
        document.getElementById(
            "marriageCustomEstimatedCost"
        );


    /*
     * 결혼 CUSTOM 선택 시
     * 직접 예상비용 입력 영역 표시
     */
    marriageLifestyleRadios.forEach(radio => {

        radio.addEventListener("change", () => {

            const selected =
                document.querySelector(
                    'input[name="marriageLifestyleLevel"]:checked'
                );


            if (!selected) {
                return;
            }


            if (selected.value === "CUSTOM") {

                marriageCustomCostArea.hidden = false;

            } else {

                marriageCustomCostArea.hidden = true;

                if (marriageCustomEstimatedCost) {

                    marriageCustomEstimatedCost.value = "";
                }
            }
        });
    });


    /**
     * 결혼 이벤트 저장
     */
    async function saveMarriageSurvey(isSilent = false) {

        const targetDate =
            document.getElementById(
                "marriageTargetDate"
            )?.value;

        let lifestyle =
            document.querySelector(
                'input[name="marriageLifestyleLevel"]:checked'
            );

        let guestCount =
            document.getElementById(
                "marriageGuestCount"
            )?.value;

        const furnitureIncluded =
            document.getElementById(
                "marriageFurnitureIncluded"
            )?.checked ?? false;

        const honeymoonIncluded =
            document.getElementById(
                "marriageHoneymoonIncluded"
            )?.checked ?? false;

        const contributionRate =
            document.getElementById(
                "marriageUserContributionRate"
            )?.value || "50";

        const familySupportAmount =
            document.getElementById(
                "marriageFamilySupportAmount"
            )?.value;


        /*
         * 필수값 검사 및 기본값 보정
         */
        if (!lifestyle) {
            if (!isSilent) {
                alert("결혼 준비 수준을 선택해주세요.");
                openEventForm("marriage", true);
                return false;
            }
            lifestyle = { value: "AVERAGE" };
        }

        if (!guestCount) {
            if (!isSilent) {
                alert("예상 하객 수를 입력해주세요.");
                openEventForm("marriage", true);
                return false;
            }
            guestCount = "200";
        }


        /*
         * CUSTOM이면 예상 결혼비용 필수
         */
        if (
            lifestyle.value === "CUSTOM"
            && !marriageCustomEstimatedCost?.value
        ) {
            if (!isSilent) {
                alert("예상 결혼 총비용을 입력해주세요.");
                openEventForm("marriage", true);
                return false;
            }
        }


        /*
         * MarriageSurveyRequest와 동일한 구조
         */
        const requestData = {

            targetDate:
            toServerDate(targetDate) || getSequentialDate(0),

            lifestyleLevel:
            lifestyle.value,

            guestCount:
                Number(guestCount),

            furnitureIncluded:
            furnitureIncluded,

            honeymoonIncluded:
            honeymoonIncluded,

            /*
             * 화면 50%
             * -> 서버 0.5
             */
            userContributionRate:
                Number(contributionRate) / 100,

            familySupportAmount:
                familySupportAmount
                    ? parseMoneyValue(familySupportAmount)
                    : 0,

            customEstimatedCost:
                lifestyle.value === "CUSTOM"
                    ? parseMoneyValue(
                        marriageCustomEstimatedCost?.value
                    )
                    : null
        };


        try {

            const currentScenarioId = await ensureScenario();

            /*
             * 기존 이벤트 ID가 없으면 신규 저장
             */
            if (!savedEventIds.marriage) {

                const response = await fetch(
                    `/api/lifecycle/survey/scenario/${currentScenarioId}/marriage`,
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body:
                            JSON.stringify(requestData)
                    }
                );


                if (!response.ok) {

                    throw new Error(
                        `결혼 설문 저장 실패: ${response.status}`
                    );
                }

                savedEventIds.marriage = await response.json();
                selectedEventTypes.add("marriage");

                if (!isSilent) {
                    alert("결혼 계획이 저장되었습니다.");
                }

            } else {

                /*
                 * 이미 저장된 이벤트라면 PUT 수정
                 */
                const response = await fetch(
                    `/api/lifecycle/survey/marriage/${savedEventIds.marriage}`,
                    {
                        method: "PUT",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body:
                            JSON.stringify(requestData)
                    }
                );


                if (!response.ok) {

                    throw new Error(
                        `결혼 설문 수정 실패: ${response.status}`
                    );
                }

                if (!isSilent) {
                    alert("결혼 계획이 수정되었습니다.");
                }
            }

            addOrUpdateTimelineEvent(
                "marriage",
                targetDate,
                savedEventIds.marriage
            );

            const cardEl = document.querySelector('.flow-step-card[data-event-type="marriage"]');
            if (cardEl) {
                cardEl.classList.remove("has-error");
            }
            return true;

        } catch (error) {

            console.error(error);

            // Focus on failed marriage scenario
            const cardEl = document.querySelector('.flow-step-card[data-event-type="marriage"]');
            if (cardEl) {
                cardEl.classList.add("has-error");
            }
            openEventForm("marriage", true);

            if (!isSilent) {
                alert(
                    "결혼 계획 저장 중 오류가 발생했습니다. 입력 정보를 확인해주세요."
                );
            }
            return false;
        }
    }

    async function autoSaveAllPlacedEvents() {
        if (!scenarioId || lifecycleEvents.length === 0) {
            return true;
        }

        for (let i = 0; i < lifecycleEvents.length; i++) {
            const ev = lifecycleEvents[i];
            const eventType = ev.type;
            let success = false;

            if (eventType === "marriage") {
                success = await saveMarriageSurvey(true);
            } else {
                const formArea = document.querySelector(
                    `[data-event-form="${eventType}"]`
                );
                const form = formArea?.querySelector("form");
                if (form) {
                    success = await saveEventSurvey(eventType, form, null, true);
                } else {
                    success = true;
                }
            }

            if (!success) {
                const cardEl = document.querySelector(`.flow-step-card[data-event-type="${eventType}"]`);
                if (cardEl) {
                    cardEl.classList.add("has-error");
                    cardEl.scrollIntoView({ behavior: "smooth", block: "nearest" });
                }
                openEventForm(eventType, true);

                alert(`[STEP ${i + 1}. ${ev.title}] 이벤트 정보 저장에 실패했습니다. 입력 내용을 확인 후 다시 시도해주세요.`);
                return false;
            }
        }
        return true;
    }

    /*
     * 결혼 저장 버튼
     */
    if (saveMarriageSurveyBtn) {
        saveMarriageSurveyBtn.addEventListener(
            "click",
            () => saveMarriageSurvey(false)
        );
    }


    /*
     * =========================================================
     * 6. 입력 내용 확인
     * =========================================================
     */

    /**
     * STEP 2에서 타임라인 이벤트를 표시한다.
     */
    async function loadReview() {
        loadEventSummary();
    }


    /**
     * 기본 생활정보 요약
     */
    async function loadBaseSurveySummary() {

        const summaryArea =
            document.getElementById(
                "baseSurveySummary"
            );


        if (!summaryArea) {
            return;
        }


        try {

            const response = await fetch(
                "/api/lifecycle/survey/base"
            );


            if (!response.ok) {

                summaryArea.innerHTML =
                    "<p>저장된 기본 생활정보가 없습니다.</p>";

                return;
            }


            const data = await response.json();


            /*
             * 주거형태 한글명
             */
            const housingNames = {

                FAMILY: "가족과 거주",

                MONTHLY_RENT: "월세",

                JEONSE: "전세",

                OWN: "자가"
            };


            /*
             * 소득 전망 한글명
             */
            const salaryScenarioNames = {

                CONSERVATIVE: "보수적",

                BASE: "기준",

                OPTIMISTIC: "낙관적",

                CUSTOM: "직접입력"
            };


            summaryArea.innerHTML = `
                <div class="lifecycle-review-item">
                    <span>월 생활비</span>
                    <strong>
                        ${formatMoney(
                data.monthlyLivingExpense
            )}원
                    </strong>
                </div>

                <div class="lifecycle-review-item">
                    <span>현재 주거형태</span>
                    <strong>
                        ${housingNames[
                data.currentHousingType
                ] ?? data.currentHousingType}
                    </strong>
                </div>

                <div class="lifecycle-review-item">
                    <span>월 주거비</span>
                    <strong>
                        ${formatMoney(
                data.monthlyHousingExpense
            )}원
                    </strong>
                </div>

                <div class="lifecycle-review-item">
                    <span>산업군</span>
                    <strong>
                        ${data.industryCode ?? "-"}
                    </strong>
                </div>

                <div class="lifecycle-review-item">
                    <span>소득 상승 가정</span>
                    <strong>
                        ${
                salaryScenarioNames[
                    data.salaryGrowthScenario
                    ]
                ?? data.salaryGrowthScenario
            }
                    </strong>
                </div>
            `;


        } catch (error) {

            console.error(error);

            summaryArea.innerHTML =
                "<p>기본 생활정보를 불러오지 못했습니다.</p>";
        }
    }


    /**
     * 현재 선택한 이벤트 요약
     *
     * 이후 각 이벤트가 완성되면
     * 실제 저장 데이터를 조회하는 방식으로 확장한다.
     */
    function loadEventSummary() {
        const summaryArea = document.getElementById("eventSurveySummary");
        if (!summaryArea) {
            return;
        }

        if (lifecycleEvents.length === 0) {
            summaryArea.innerHTML = "<p>선택한 생활 이벤트가 없습니다.</p>";
            return;
        }

        summaryArea.innerHTML = lifecycleEvents.map((event, index) => {
            const stepNumber = index + 1;
            const eventType = event.type;
            const eventForm = document.querySelector(
                `[data-event-form="${eventType}"]`
            );
            const fields = eventForm
                ? collectEventFormValues(eventForm)
                : [];

            const fieldMarkup = fields.length > 0
                ? fields.map(field => `
                    <div class="lifecycle-review-item">
                        <span>${escapeHtml(field.label)}</span>
                        <strong>${escapeHtml(field.value)}</strong>
                    </div>
                `).join("")
                : "<p>입력된 상세 내용이 없습니다.</p>";

            return `
                <section class="lifecycle-event-review-group">
                    <h4>STEP ${stepNumber}. ${escapeHtml(
                        lifecycleEventNames[eventType] ?? eventType
                    )}</h4>
                    ${fieldMarkup}
                </section>
            `;
        }).join("");
    }

    function collectEventFormValues(eventForm) {

        return Array.from(
            eventForm.querySelectorAll(
                "input, select, textarea"
            )
        ).filter(control => {
            if (
                control.disabled
                || control.type === "hidden"
                || control.type === "button"
                || control.type === "submit"
            ) {
                return false;
            }

            if (
                control.type === "radio"
                && !control.checked
            ) {
                return false;
            }

            return control.type === "checkbox"
                || control.value !== "";
        }).map(control => ({
            label: getControlLabel(control),
            value: getControlDisplayValue(control)
        }));
    }

    function getControlLabel(control) {

        const explicitLabel = control.id
            ? document.querySelector(
                `label[for="${control.id}"]`
            )
            : null;
        const wrappingLabel = control.closest("label");

        return (
            explicitLabel?.textContent
            || wrappingLabel?.textContent
            || control.name
            || control.id
            || "입력값"
        ).trim().replace(/\s+/g, " ");
    }

    function getControlDisplayValue(control) {

        if (control.type === "checkbox") {
            return control.checked ? "예" : "아니오";
        }

        if (control.type === "radio") {
            return control.closest("label")?.textContent
                ?.trim().replace(/\s+/g, " ")
                || control.value;
        }

        if (control.tagName === "SELECT") {
            return control.selectedOptions[0]?.textContent
                ?.trim()
                || control.value;
        }

        return control.value;
    }

    function escapeHtml(value) {
        const element = document.createElement("div");
        element.textContent = String(value ?? "");
        return element.innerHTML;
    }


    /*
     * =========================================================
     * 7. 시뮬레이션 시작 버튼
     * =========================================================
     */

    const completeSurveyBtn =
        document.getElementById(
            "completeLifecycleSurveyBtn"
        );


    if (completeSurveyBtn) {

        completeSurveyBtn.addEventListener("click", async () => {
            if (!scenarioId) {
                alert("완료할 시나리오를 먼저 선택해주세요.");
                return;
            }

            completeSurveyBtn.disabled = true;
            try {
                const response = await fetch(
                    `/api/lifecycle/scenarios/${scenarioId}`,
                    {
                        method: "PATCH",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ status: "COMPLETED" })
                    }
                );
                if (!response.ok) {
                    throw new Error(`시나리오 완료 처리 실패: ${response.status}`);
                }
                alert("시나리오 입력이 완료되었습니다.");
                showStep("base");
                setScenarioListExpanded(true);
                await loadScenarioList();
            } catch (error) {
                console.error(error);
                alert("시나리오를 완료 처리하지 못했습니다.");
            } finally {
                completeSurveyBtn.disabled = false;
            }
        });
    }
    /*
     * =========================================================
     * 7. 시뮬레이션 결과
     * =========================================================
     */

    const runSimulationBtn =
        document.getElementById("runLifecycleSimulationBtn");

    const simulationEmpty =
        document.getElementById("lifecycleSimulationEmpty");

    const simulationSummary =
        document.getElementById("lifecycleSimulationSummary");

    const simulationSnapshots =
        document.getElementById("lifecycleSimulationSnapshots");

    const assetJourney =
        document.getElementById("lifecycleAssetJourney");

    const assetTimeline =
        document.getElementById("lifecycleAssetTimeline");

    const assetChart =
        document.getElementById("lifecycleAssetChart");

    const eventCostChart =
        document.getElementById("lifecycleEventCostChart");

    const snapshotModal =
        document.getElementById("lifecycleSnapshotModal");

    const snapshotModalTitle =
        document.getElementById("snapshotModalTitle");

    const snapshotModalEventDate =
        document.getElementById("snapshotModalEventDate");

    const snapshotModalBody =
        document.getElementById("snapshotModalBody");

    let latestSimulationResult = null;

    function buildSimulationBaseState() {
        const today = new Date().toISOString().substring(0, 10);
        return {
            baseDate: today,
            base_date: today,
            loans: []
        };
    }

    async function loadUserLoansForRepayment() {
        const loanSelect = document.querySelector('select[name="loanAccountId"]');
        if (!loanSelect) return;

        try {
            const res = await fetch("/api/lifecycle/loans", {
                headers: { "Accept": "application/json" }
            });
            if (!res.ok) return;
            const loans = await res.json();
            if (!loans || loans.length === 0) {
                loanSelect.innerHTML = `<option value="">보유 중인 금융 대출 없음 (고금리 대출 우선 상환)</option>`;
                return;
            }

            const currentVal = loanSelect.value;
            loanSelect.innerHTML = `<option value="">선택 안 함 (금리 높은 대출부터 우선 상환)</option>` +
                loans.map(loan => {
                    const typeLabel = {
                        MORTGAGE: "주택담보대출",
                        JEONSE: "전세자금대출",
                        CREDIT: "신용대출",
                        AUTO: "자동차대출"
                    }[loan.loanType] ?? loan.loanType ?? "대출";
                    const rate = loan.interestRate ? `${loan.interestRate}%` : "";
                    const balance = loan.currentBalance ? formatCompactMoney(loan.currentBalance) : "0원";
                    return `<option value="${loan.loanAccountId}">${escapeHtml(typeLabel)} (잔액: ${balance}, 금리: ${rate})</option>`;
                }).join("");

            if (currentVal) {
                loanSelect.value = currentVal;
            }
        } catch (e) {
            console.error("Failed to load active loans:", e);
        }
    }

    async function loadSavedSimulationResult() {
        if (!scenarioId) return;
        try {
            const res = await fetch(`/api/lifecycle/scenarios/${scenarioId}/result`, {
                headers: { "Accept": "application/json" }
            });
            if (res.ok && res.status === 200) {
                const savedResult = await res.json();
                if (savedResult && savedResult.eventSnapshots && savedResult.eventSnapshots.length > 0) {
                    latestSimulationResult = savedResult;
                    renderSimulationResult(savedResult);
                }
            }
        } catch (e) {
            console.log("No saved simulation result yet.");
        }
    }

    async function runLifecycleSimulation() {
        if (!scenarioId) {
            alert("시뮬레이션할 시나리오를 먼저 선택해주세요.");
            return;
        }

        runSimulationBtn.disabled = true;
        
        const steps = [
            "1/3 기초 금융 데이터 및 자산 분석 중...",
            "2/3 복리 소득/물가 및 생애 이벤트 전진 중...",
            "3/3 DSR 정밀 산출 및 맞춤 상품 진단 중..."
        ];
        let stepIdx = 0;
        runSimulationBtn.textContent = steps[0];
        const stepInterval = setInterval(() => {
            stepIdx = (stepIdx + 1) % steps.length;
            runSimulationBtn.textContent = steps[stepIdx];
        }, 350);

        try {
            const response = await fetch(
                `/api/lifecycle/scenarios/${scenarioId}/simulate`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    body: JSON.stringify(buildSimulationBaseState())
                }
            );

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    alert("로그인이 필요하거나 세션이 만료되었습니다. 로그인 후 다시 시도해주세요.");
                    return;
                }
                if (response.status === 404) {
                    alert("선택한 시나리오를 찾을 수 없습니다. 시나리오 목록에서 다시 선택해주세요.");
                    return;
                }
                const errorText = await response.text();
                console.error("Simulation API failed:", response.status, errorText);
                throw new Error(`시뮬레이션 실패 (${response.status})`);
            }

            latestSimulationResult = await response.json();
            renderSimulationResult(latestSimulationResult);

        } catch (error) {
            console.error(error);
            alert(`시뮬레이션 실행 중 오류가 발생했습니다.\n(${error.message || "서버 통신 실패"})`);
        } finally {
            clearInterval(stepInterval);
            runSimulationBtn.disabled = false;
            runSimulationBtn.textContent = "시뮬레이션 다시 실행";
        }
    }

    function formatCompactMoney(value) {
        if (value === null || value === undefined || value === "") return "0원";
        const amount = Number(String(value).replace(/,/g, ""));
        if (!Number.isFinite(amount)) return "0원";
        const sign = amount < 0 ? "-" : "";
        const absolute = Math.abs(amount);
        const eok = Math.floor(absolute / 100000000);
        const remainder = absolute % 100000000;
        const man = Math.round(remainder / 10000);
        if (eok > 0 && man > 0) return `${sign}${eok}억 ${man.toLocaleString("ko-KR")}만원`;
        if (eok > 0) return `${sign}${eok}억원`;
        if (man > 0) return `${sign}${man.toLocaleString("ko-KR")}만원`;
        if (absolute > 0) return `${sign}${Math.round(absolute).toLocaleString("ko-KR")}원`;
        return "0원";
    }

    function formatApproxMoney(value) {
        return `약 ${formatCompactMoney(value)}`;
    }

    function formatAxisMoney(value) {
        const amount = Number(value ?? 0);
        if (!Number.isFinite(amount)) return "0";
        if (Math.abs(amount) >= 100000000) {
            return `${(amount / 100000000).toFixed(1).replace(".0", "")}억`;
        }
        return `${Math.round(amount / 10000).toLocaleString("ko-KR")}만`;
    }

    function journeyYear(dateValue) {
        const year = String(dateValue ?? "").substring(0, 4);
        return /^\d{4}$/.test(year) ? year : "현재";
    }

    function renderSnapshotCard(snapshot, index) {
        const stepNum = index + 1;
        const typeKey = lifecycleEventTypes[snapshot.eventType] || String(snapshot.eventType).toLowerCase();
        const icon = getEventIcon(typeKey);
        const title = lifecycleEventNames[typeKey] || eventTypeLabel(snapshot.eventType);
        const supportCount = (snapshot.supports ?? []).length;
        const productCount = (snapshot.recommendedProducts ?? []).length;
        const calcDetailsHtml = buildEventCalculationDetails(snapshot, index);

        return `
            <div class="lifecycle-snapshot-result-card" data-snapshot-index="${index}">
                <div class="snapshot-card-header">
                    <div class="snapshot-card-title-group">
                        <span class="snapshot-step-badge">STEP ${stepNum}</span>
                        <span class="snapshot-icon material-symbols-outlined">${icon}</span>
                        <h4 class="snapshot-title">${escapeHtml(title)}</h4>
                    </div>
                </div>

                ${calcDetailsHtml}

                <div class="snapshot-card-footer">
                    <div class="snapshot-benefits-tags">
                        ${supportCount > 0 ? `
                            <span class="benefit-chip is-welfare" title="추천 복지 지원">
                                <span class="material-symbols-outlined">verified</span>
                                복지 지원 ${supportCount}건
                            </span>
                        ` : ''}
                        ${productCount > 0 ? `
                            <span class="benefit-chip is-product" title="추천 금융 상품">
                                <span class="material-symbols-outlined">account_balance</span>
                                추천 상품 ${productCount}건
                            </span>
                        ` : ''}
                    </div>
                    <button type="button" class="snapshot-detail-btn" data-snapshot-index="${index}">
                        상세 분석 보기 ➜
                    </button>
                </div>
            </div>
        `;
    }

    function renderSimulationResult(result) {
        latestSimulationResult = result;
        const snapshots = result.eventSnapshots ?? [];

        if (simulationEmpty) {
            simulationEmpty.hidden = snapshots.length > 0;
        }

        if (simulationSnapshots) {
            simulationSnapshots.hidden = true;
            simulationSnapshots.replaceChildren();
        }

        renderAssetJourney(result, snapshots);
    }

    const REGIONAL_MEAL_PRICES = {
        "서울특별시": 75000,
        "경기도": 65000,
        "인천광역시": 65000,
        "부산광역시": 60000,
        "대구광역시": 60000,
        "광주광역시": 60000,
        "대전광역시": 60000,
        "울산광역시": 60000,
        "세종특별자치시": 60000,
        "강원특별자치도": 55000,
        "충청북도": 55000,
        "충청남도": 55000,
        "전북특별자치도": 55000,
        "전라남도": 55000,
        "경상북도": 55000,
        "경상남도": 55000,
        "제주특별자치도": 55000,
        "DEFAULT": 65000
    };

    function buildEventCalculationDetails(snapshot, index) {
        const eventType = snapshot.eventType;

        if (eventType === "MARRIAGE") {
            const form = document.getElementById("marriageSurveyForm");
            const sido = form?.querySelector('[name="regionSido"]')?.value || "서울특별시";
            const sigungu = form?.querySelector('[name="regionSigungu"]')?.value || "";
            const regionText = sigungu ? `${sido} ${sigungu}` : sido;

            const guestCount = Number(form?.querySelector('[name="guestCount"]')?.value || 200);
            const mealPrice = REGIONAL_MEAL_PRICES[sido] || REGIONAL_MEAL_PRICES["DEFAULT"];
            const mealPriceMan = (mealPrice / 10000).toFixed(1).replace(".0", "");
            const totalMealCost = guestCount * mealPrice;

            const lifestyle = form?.querySelector('input[name="marriageLifestyleLevel"]:checked')?.value || snapshot.lifestyleLevel || "AVERAGE";
            const baseHallCostMap = {
                PRACTICAL: 9680000,
                AVERAGE: 11390000,
                RELAXED: 13100000,
                PREMIUM: 14800000,
                CUSTOM: 11390000
            };
            const hallCost = baseHallCostMap[lifestyle] || 11390000;
            const lifestyleNameMap = {
                PRACTICAL: "실속형",
                AVERAGE: "평균형",
                RELAXED: "여유형",
                PREMIUM: "프리미엄형",
                CUSTOM: "직접입력"
            };

            const furnitureIncluded = form?.querySelector('[name="furnitureIncluded"]')?.checked ?? false;
            const honeymoonIncluded = form?.querySelector('[name="honeymoonIncluded"]')?.checked ?? false;
            const furnitureCost = furnitureIncluded ? 12000000 : 0;
            const honeymoonCost = honeymoonIncluded ? 6000000 : 0;

            const totalEstCost = snapshot.estimatedCost > 0
                ? Number(snapshot.estimatedCost)
                : (hallCost + totalMealCost + furnitureCost + honeymoonCost);

            const contributionRate = form?.querySelector('[name="userContributionRate"]')?.value
                ? Number(form.querySelector('[name="userContributionRate"]').value) / 100
                : 0.5;
            const userShare = snapshot.userContributionAmount > 0
                ? Number(snapshot.userContributionAmount)
                : totalEstCost * contributionRate;

            const familySupport = form?.querySelector('[name="familySupportAmount"]')?.value
                ? parseMoneyValue(form.querySelector('[name="familySupportAmount"]').value)
                : (snapshot.familySupportAmount ? Number(snapshot.familySupportAmount) : 0);

            const supportBenefit = snapshot.supportBenefit ? Number(snapshot.supportBenefit) : 0;
            const userRequired = snapshot.userRequiredAmount > 0
                ? Number(snapshot.userRequiredAmount)
                : Math.max(0, userShare - familySupport - supportBenefit);

            return `
                <div class="result-calc-breakdown">
                    <div class="result-calc-meta-row">
                        <span class="calc-meta-item"><strong>예상 거주지</strong> ${escapeHtml(regionText)}</span>
                        <span class="calc-meta-item"><strong>준비 수준</strong> ${escapeHtml(lifestyleNameMap[lifestyle] || "평균형")}</span>
                    </div>
                    <div class="result-calc-list">
                        <div class="calc-item-row">
                            <span class="calc-name">평균 예식장 및 스드메 패키지</span>
                            <strong class="calc-price">${formatCompactMoney(hallCost)}</strong>
                        </div>
                        <div class="calc-item-row is-featured">
                            <div class="calc-name-group">
                                <span class="calc-name">식대 (예상 하객 ${guestCount}명 × ${mealPriceMan}만원)</span>
                                <span class="calc-pill-note">(지역별 평균 식대: ${mealPriceMan}만원)</span>
                            </div>
                            <strong class="calc-price">${formatCompactMoney(totalMealCost)}</strong>
                        </div>
                        ${furnitureIncluded ? `
                            <div class="calc-item-row">
                                <span class="calc-name">혼수 준비비 (포함)</span>
                                <strong class="calc-price">${formatCompactMoney(furnitureCost)}</strong>
                            </div>
                        ` : ''}
                        ${honeymoonIncluded ? `
                            <div class="calc-item-row">
                                <span class="calc-name">신혼여행 경비 (포함)</span>
                                <strong class="calc-price">${formatCompactMoney(honeymoonCost)}</strong>
                            </div>
                        ` : ''}
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">총 예상 결혼 비용</span>
                            <strong class="calc-total-val">${formatCompactMoney(totalEstCost)}</strong>
                        </div>
                    </div>
                    <div class="result-funding-summary">
                        <div class="funding-pill">
                            <span>본인 분담 (${Math.round(contributionRate * 100)}%)</span>
                            <b>${formatCompactMoney(userShare)}</b>
                        </div>
                        ${familySupport > 0 ? `
                            <div class="funding-pill">
                                <span>가족 지원금</span>
                                <b class="is-minus">-${formatCompactMoney(familySupport)}</b>
                            </div>
                        ` : ''}
                        ${supportBenefit > 0 ? `
                            <div class="funding-pill">
                                <span>공공 지원금</span>
                                <b class="is-minus">-${formatCompactMoney(supportBenefit)}</b>
                            </div>
                        ` : ''}
                        <div class="funding-pill is-final-target">
                            <span>최종 본인 필요 자금</span>
                            <b class="final-val">${formatCompactMoney(userRequired)}</b>
                        </div>
                    </div>
                </div>
            `;
        }

        if (eventType === "CHILDBIRTH") {
            const form = document.getElementById("childbirthSurveyForm");
            const sido = form?.querySelector('[name="regionSido"]')?.value || "서울특별시";
            const sigungu = form?.querySelector('[name="regionSigungu"]')?.value || "";
            const regionText = sigungu ? `${sido} ${sigungu}` : sido;
            const postpartumCare = form?.querySelector('[name="postpartumCare"]')?.checked ?? true;
            const careCost = postpartumCare ? 2865000 : 1255000;
            const careLabel = postpartumCare ? "산후조리원 이용 (평균 286.5만원)" : "재가 산후조리 (평균 125.5만원)";
            const monthlyChildcare = 800000;
            const supportBenefit = snapshot.supportBenefit ? Number(snapshot.supportBenefit) : 0;
            const userRequired = snapshot.userRequiredAmount > 0 ? Number(snapshot.userRequiredAmount) : Math.max(0, careCost - supportBenefit);

            return `
                <div class="result-calc-breakdown">
                    <div class="result-calc-meta-row">
                        <span class="calc-meta-item"><strong>출산 예정 지역</strong> ${escapeHtml(regionText)}</span>
                    </div>
                    <div class="result-calc-list">
                        <div class="calc-item-row is-featured">
                            <span class="calc-name">산후조리 비용 (${careLabel})</span>
                            <strong class="calc-price">${formatCompactMoney(careCost)}</strong>
                        </div>
                        <div class="calc-item-row">
                            <span class="calc-name">월 예상 양육비 (기저귀/분유/보육 등)</span>
                            <strong class="calc-price">월 ${formatCompactMoney(monthlyChildcare)} (연 ${formatCompactMoney(monthlyChildcare * 12)})</strong>
                        </div>
                        ${supportBenefit > 0 ? `
                            <div class="calc-item-row is-benefit">
                                <span class="calc-name">정부·지자체 출산지원 혜택 (첫만남이용권·부모급여 등)</span>
                                <strong class="calc-price is-minus">-${formatCompactMoney(supportBenefit)}</strong>
                            </div>
                        ` : ''}
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">초기 필요 순 자부담금</span>
                            <strong class="calc-total-val">${formatCompactMoney(userRequired)}</strong>
                        </div>
                    </div>
                </div>
            `;
        }

        if (eventType === "VEHICLE_PURCHASE") {
            const form = document.getElementById("vehicleSurveyForm");
            const vehicleCondition = form?.querySelector('[name="vehicleCondition"]')?.value || "NEW";
            const vehicleClass = form?.querySelector('[name="vehicleClass"]')?.value || "MIDSIZE";
            const conditionLabel = vehicleCondition === "USED" ? "중고차" : "신차";
            const classLabelMap = {
                COMPACT: "경차",
                SMALL: "소형차",
                SEMI_MIDSIZE: "준중형차",
                MIDSIZE: "중형차",
                LARGE: "준대형차",
                SUV: "SUV"
            };
            const classLabel = classLabelMap[vehicleClass] || "중형차";
            const price = snapshot.estimatedCost > 0 ? Number(snapshot.estimatedCost) : 36000000;
            const tax = Math.round(price * 0.07);
            const inputMaintenance = form?.querySelector('[name="monthlyMaintenanceCost"]')?.value;
            let monthlyCost = 450000;
            if (inputMaintenance !== undefined && inputMaintenance !== null && String(inputMaintenance).trim() !== "") {
                monthlyCost = parseMoneyValue(inputMaintenance);
            } else if (snapshot.additionalMonthlyExpense !== undefined && snapshot.additionalMonthlyExpense !== null && Number(snapshot.additionalMonthlyExpense) >= 0) {
                monthlyCost = Number(snapshot.additionalMonthlyExpense);
            }
            const userRequired = snapshot.userRequiredAmount > 0 ? Number(snapshot.userRequiredAmount) : price;

            return `
                <div class="result-calc-breakdown">
                    <div class="result-calc-meta-row">
                        <span class="calc-meta-item"><strong>선택 차량</strong> ${conditionLabel} · ${classLabel}</span>
                    </div>
                    <div class="result-calc-list">
                        <div class="calc-item-row is-featured">
                            <span class="calc-name">차량 기본 가격</span>
                            <strong class="calc-price">${formatCompactMoney(price)}</strong>
                        </div>
                        <div class="calc-item-row">
                            <span class="calc-name">취등록세 및 부대비용 (약 7%)</span>
                            <strong class="calc-price">${formatCompactMoney(tax)}</strong>
                        </div>
                        <div class="calc-item-row">
                            <span class="calc-name">월 예상 유지비 (보험/세금/유류비)</span>
                            <strong class="calc-price">월 ${formatCompactMoney(monthlyCost)}</strong>
                        </div>
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">초기 필요 자금 (차량가 + 부대비용)</span>
                            <strong class="calc-total-val">${formatCompactMoney(userRequired)}</strong>
                        </div>
                    </div>
                </div>
            `;
        }

        if (eventType === "HOME_PURCHASE") {
            const form = document.getElementById("homePurchaseSurveyForm");
            const housingType = form?.querySelector('[name="housingType"]')?.value || "APARTMENT";
            const typeLabelMap = { APARTMENT: "아파트", VILLA: "빌라/다세대", OFFICETEL: "오피스텔" };
            const price = snapshot.estimatedCost > 0 ? Number(snapshot.estimatedCost) : 600000000;
            const tax = Math.round(price * 0.011);
            const loanAmount = snapshot.newLoanAmount ? Number(snapshot.newLoanAmount) : 0;
            const userRequired = snapshot.userRequiredAmount > 0 ? Number(snapshot.userRequiredAmount) : Math.max(0, price + tax - loanAmount);

            return `
                <div class="result-calc-breakdown">
                    <div class="result-calc-meta-row">
                        <span class="calc-meta-item"><strong>주택 형태</strong> ${typeLabelMap[housingType] || "아파트"} (84㎡ 기준)</span>
                    </div>
                    <div class="result-calc-list">
                        <div class="calc-item-row is-featured">
                            <span class="calc-name">예상 주택 매매가</span>
                            <strong class="calc-price">${formatCompactMoney(price)}</strong>
                        </div>
                        <div class="calc-item-row">
                            <span class="calc-name">취득세 및 등기비용 (약 1.1%)</span>
                            <strong class="calc-price">${formatCompactMoney(tax)}</strong>
                        </div>
                        ${loanAmount > 0 ? `
                            <div class="calc-item-row">
                                <span class="calc-name">주택담보대출 실행액</span>
                                <strong class="calc-price">${formatCompactMoney(loanAmount)}</strong>
                            </div>
                        ` : ''}
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">필요 자기자본 (순 현금 준비금)</span>
                            <strong class="calc-total-val">${formatCompactMoney(userRequired)}</strong>
                        </div>
                    </div>
                </div>
            `;
        }

        if (eventType === "JEONSE") {
            const form = document.getElementById("jeonseSurveyForm");
            const housingType = form?.querySelector('[name="housingType"]')?.value || "APARTMENT";
            const typeLabelMap = { APARTMENT: "아파트", VILLA: "빌라/다세대", OFFICETEL: "오피스텔" };
            const deposit = snapshot.estimatedCost > 0 ? Number(snapshot.estimatedCost) : 300000000;
            const loanAmount = snapshot.newLoanAmount ? Number(snapshot.newLoanAmount) : 0;
            const userRequired = snapshot.userRequiredAmount > 0 ? Number(snapshot.userRequiredAmount) : Math.max(0, deposit - loanAmount);

            return `
                <div class="result-calc-breakdown">
                    <div class="result-calc-meta-row">
                        <span class="calc-meta-item"><strong>주택 형태</strong> ${typeLabelMap[housingType] || "아파트"} (59㎡ 기준)</span>
                    </div>
                    <div class="result-calc-list">
                        <div class="calc-item-row is-featured">
                            <span class="calc-name">전세 보증금</span>
                            <strong class="calc-price">${formatCompactMoney(deposit)}</strong>
                        </div>
                        ${loanAmount > 0 ? `
                            <div class="calc-item-row">
                                <span class="calc-name">전세자금대출</span>
                                <strong class="calc-price">${formatCompactMoney(loanAmount)}</strong>
                            </div>
                        ` : ''}
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">필요 보증금 현금 (자기자본)</span>
                            <strong class="calc-total-val">${formatCompactMoney(userRequired)}</strong>
                        </div>
                    </div>
                </div>
            `;
        }

        if (eventType === "MONTHLY_RENT") {
            const form = document.getElementById("monthlyRentSurveyForm");
            const inputDeposit = form?.querySelector('[name="desiredDeposit"]')?.value;
            const inputRent = form?.querySelector('[name="desiredMonthlyRent"]')?.value;
            const deposit = snapshot.estimatedCost > 0 
                ? Number(snapshot.estimatedCost) 
                : (inputDeposit !== undefined && inputDeposit !== null && String(inputDeposit).trim() !== "" ? parseMoneyValue(inputDeposit) : 20000000);
            const monthlyRent = (snapshot.additionalMonthlyExpense !== undefined && snapshot.additionalMonthlyExpense !== null && Number(snapshot.additionalMonthlyExpense) > 0)
                ? Number(snapshot.additionalMonthlyExpense)
                : (inputRent !== undefined && inputRent !== null && String(inputRent).trim() !== "" ? parseMoneyValue(inputRent) : 700000);
            const userRequired = snapshot.userRequiredAmount > 0 ? Number(snapshot.userRequiredAmount) : deposit;
            return `
                <div class="result-calc-breakdown">
                    <div class="result-calc-meta-row">
                        <span class="calc-meta-item"><strong>주거 형태</strong> 월세</span>
                    </div>
                    <div class="result-calc-list">
                        <div class="calc-item-row is-featured">
                            <span class="calc-name">월세 보증금</span>
                            <strong class="calc-price">${formatCompactMoney(deposit)}</strong>
                        </div>
                        <div class="calc-item-row">
                            <span class="calc-name">월 임대료</span>
                            <strong class="calc-price">월 ${formatCompactMoney(monthlyRent)}</strong>
                        </div>
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">초기 필요 보증금</span>
                            <strong class="calc-total-val">${formatCompactMoney(userRequired)}</strong>
                        </div>
                    </div>
                </div>
            `;
        }

        if (eventType === "REPAYMENT") {
            const repayAmount = snapshot.eventCost ? Number(snapshot.eventCost) : 30000000;
            const fee = Math.round(repayAmount * 0.0065);
            return `
                <div class="result-calc-breakdown">
                    <div class="result-calc-list">
                        <div class="calc-item-row is-featured">
                            <span class="calc-name">원금 상환액</span>
                            <strong class="calc-price">${formatCompactMoney(repayAmount)}</strong>
                        </div>
                        <div class="calc-item-row">
                            <span class="calc-name">예상 중도상환수수료 (약 0.65%)</span>
                            <strong class="calc-price">${formatCompactMoney(fee)}</strong>
                        </div>
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">총 소요 자금</span>
                            <strong class="calc-total-val">${formatCompactMoney(repayAmount + fee)}</strong>
                        </div>
                    </div>
                </div>
            `;
        }

        return `
            <div class="result-calc-breakdown">
                <div class="result-calc-list">
                    <div class="calc-item-row is-total-row">
                        <span class="calc-total-label">예상 소요 비용</span>
                        <strong class="calc-total-val">${formatCompactMoney(snapshot.eventCost)}</strong>
                    </div>
                </div>
            </div>
        `;
    }

    function buildPointReportCard(point, index) {
        const isCurrent = index === 0;
        const stepBadge = isCurrent ? "시작" : `STEP ${index}`;
        const snapshot = point.snapshot;
        const eventCost = Number(point.eventCost ?? 0);
        const reqAmount = Number(snapshot?.userRequiredAmount ?? eventCost);
        const netAssetStr = formatCompactMoney(point.netAsset);
        const changeAmount = point.netAssetChange ?? 0;
        const changeClass = changeAmount >= 0 ? "is-positive" : "is-negative";
        const changeSign = changeAmount > 0 ? "+" : "";
        const changeStr = isCurrent ? "초기 자산" : `${changeSign}${formatCompactMoney(changeAmount)}`;

        let supportCount = 0;
        let productCount = 0;
        let deficitWarning = false;

        if (snapshot) {
            supportCount = (snapshot.supports ?? []).length;
            productCount = (snapshot.recommendedProducts ?? []).length;
            const feasibility = snapshot.feasibility ?? {};
            deficitWarning = feasibility.status === "DEFICIT" || feasibility.deficitRisk || snapshot.deficit;
        }

        let detailHighlight = "";
        if (snapshot?.eventType === "MARRIAGE") {
            const form = document.getElementById("marriageSurveyForm");
            const sido = form?.querySelector('[name="regionSido"]')?.value || "서울특별시";
            const guestCount = Number(form?.querySelector('[name="guestCount"]')?.value || 200);
            const mealPrice = REGIONAL_MEAL_PRICES[sido] || REGIONAL_MEAL_PRICES["DEFAULT"];
            const mealPriceMan = (mealPrice / 10000).toFixed(1).replace(".0", "");
            const totalMealCost = guestCount * mealPrice;
            detailHighlight = `식대 ${formatCompactMoney(totalMealCost)} (${guestCount}명 × ${mealPriceMan}만)`;
        } else if (snapshot?.eventType === "CHILDBIRTH") {
            detailHighlight = "산후조리 및 보육 지원 반영";
        } else if (snapshot?.eventType === "VEHICLE_PURCHASE") {
            detailHighlight = "차량가 및 취등록세(7%) 반영";
        } else if (snapshot?.eventType === "HOME_PURCHASE") {
            detailHighlight = "주택매매 및 취득세(1.1%) 반영";
        }

        return `
            <div class="journey-hover-popover" role="tooltip">
                <div class="popover-arrow"></div>
                <div class="popover-header">
                    <span class="popover-step-badge">${escapeHtml(stepBadge)}</span>
                    <strong class="popover-title">${escapeHtml(point.label)}</strong>
                    ${!isCurrent ? (deficitWarning
                        ? `<span class="popover-status-badge is-warning">부족 주의</span>`
                        : `<span class="popover-status-badge is-stable">안정</span>`) : ""}
                </div>

                ${!isCurrent ? `
                    <div class="popover-cost-highlight">
                        <span>예상 소요 지출</span>
                        <strong>${escapeHtml(formatCompactMoney(eventCost))}</strong>
                    </div>
                ` : `
                    <div class="popover-cost-highlight" style="background: #f1f5f9; border-color: #cbd5e1;">
                        <span style="color: #475569;">시뮬레이션 시작</span>
                        <strong style="color: #1e3a5f;">초기 단계</strong>
                    </div>
                `}

                <div class="popover-stats-grid">
                    ${(!isCurrent && reqAmount > 0) ? `
                        <div class="popover-stat-item">
                            <span>본인 필요 자금</span>
                            <b style="color: #2563eb;">${escapeHtml(formatCompactMoney(reqAmount))}</b>
                        </div>
                    ` : ''}
                    ${(!isCurrent && point.newLoanAmount > 0) ? `
                        <div class="popover-stat-item">
                            <span>신규 대출</span>
                            <b>${escapeHtml(formatCompactMoney(point.newLoanAmount))}</b>
                        </div>
                    ` : ''}
                    ${detailHighlight ? `
                        <div class="popover-stat-item is-highlight-stat" style="grid-column: 1 / -1;">
                            <span>산출 기준</span>
                            <b style="font-size: 11.5px; color: #2563eb;">${escapeHtml(detailHighlight)}</b>
                        </div>
                    ` : ''}
                </div>

                ${(supportCount > 0 || productCount > 0) ? `
                    <div class="popover-benefits-tag">
                        <span class="material-symbols-outlined">verified</span>
                        <span>정부지원 ${supportCount}건 · 추천상품 ${productCount}건</span>
                    </div>
                ` : ''}

                ${!isCurrent ? `
                    <button type="button"
                            class="popover-detail-action-btn"
                            data-open-snapshot-modal="${index - 1}">
                        <span>상세 분석 보고서 보기</span>
                        <span class="material-symbols-outlined">arrow_forward</span>
                    </button>
                ` : ''}
            </div>
        `;
    }

    function renderAssetJourney(result, snapshots) {
        if (!assetJourney || !assetTimeline || !eventCostChart) {
            return;
        }

        assetJourney.hidden = snapshots.length === 0;
        if (snapshots.length === 0) {
            assetTimeline.innerHTML = "";
            eventCostChart.innerHTML = "";
            return;
        }

        const baseDate = result.initialState?.baseDate
            ?? new Date().toISOString().substring(0, 10);
        const points = [{
            date: baseDate,
            label: "현재 (시작)",
            eventType: "CURRENT",
            stepIndex: 0,
            eventCost: 0,
            newLoanAmount: 0,
            snapshot: null
        }, ...snapshots.map((snapshot, idx) => ({
            date: snapshot.eventDate,
            label: eventTypeLabel(snapshot.eventType),
            eventType: snapshot.eventType,
            stepIndex: idx + 1,
            eventCost: Number(snapshot.eventCost ?? 0),
            newLoanAmount: Number(snapshot.newLoanAmount ?? 0),
            snapshot: snapshot
        }))];

        assetTimeline.innerHTML = points.map((point, index) => {
            const isCurrent = index === 0;
            const stepLabel = isCurrent ? "시작" : `STEP ${index}`;
            const costDisplay = isCurrent
                ? "시작"
                : formatCompactMoney(point.eventCost);
            const costLabel = isCurrent ? "시점" : "소요 지출";
            const detail = isCurrent
                ? "시뮬레이션 시작"
                : buildJourneyDetail(point);

            const alignClass = index <= 1
                ? "popover-align-left"
                : (index >= points.length - 2 && points.length > 2 ? "popover-align-right" : "popover-align-center");

            return `
                <article class="lifecycle-journey-point ${isCurrent ? "is-current" : ""} ${alignClass}"
                         data-point-index="${index}"
                         tabindex="0"
                         role="button"
                         aria-label="${escapeHtml(point.label)} 상세 결과 (클릭 시 상세 모달 열림)">
                    <time>${escapeHtml(stepLabel)}</time>
                    <div class="lifecycle-journey-dot" aria-hidden="true"></div>
                    <strong>${escapeHtml(point.label)}</strong>
                    <div class="timeline-cost-badge">
                        <span>${escapeHtml(costLabel)}</span>
                        <b class="point-cost-highlight">${escapeHtml(costDisplay)}</b>
                    </div>
                    <small>${escapeHtml(detail)}</small>
                    ${buildPointReportCard(point, index)}
                </article>
            `;
        }).join("");

        eventCostChart.innerHTML = buildEventCostChart(snapshots);

        // Bind interactive click events on timeline points
        const timelinePoints = assetTimeline.querySelectorAll(".lifecycle-journey-point");

        timelinePoints.forEach(pointEl => {
            const idx = Number(pointEl.dataset.pointIndex);
            const pointData = points[idx];

            if (idx > 0 && pointData?.snapshot) {
                pointEl.addEventListener("click", e => {
                    if (e.target.closest("[data-open-snapshot-modal]")) return;
                    openSnapshotModal(pointData.snapshot, idx - 1);
                });

                pointEl.addEventListener("keydown", e => {
                    if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        openSnapshotModal(pointData.snapshot, idx - 1);
                    }
                });
            }
        });

        assetTimeline.querySelectorAll("[data-open-snapshot-modal]").forEach(btn => {
            btn.addEventListener("click", e => {
                e.stopPropagation();
                const snapshotIdx = Number(btn.dataset.openSnapshotModal);
                if (snapshots[snapshotIdx]) {
                    openSnapshotModal(snapshots[snapshotIdx], snapshotIdx);
                }
            });
        });
    }

    function buildJourneyDetail(point) {
        const details = [];
        if (point.eventCost > 0) {
            details.push(`총비용 ${formatCompactMoney(point.eventCost)}`);
        }
        if (point.snapshot?.userRequiredAmount > 0) {
            details.push(`본인부담 ${formatCompactMoney(point.snapshot.userRequiredAmount)}`);
        } else if (point.newLoanAmount > 0) {
            details.push(`대출 ${formatCompactMoney(point.newLoanAmount)}`);
        }
        return details.join(" · ") || "지출 계획 반영";
    }

    function buildEventCostChart(snapshots) {
        const costs = snapshots.map(snapshot => Math.max(0, Number(snapshot.eventCost ?? 0)));
        const maxCost = Math.max(...costs, 1);

        return snapshots.map((snapshot, index) => {
            const cost = costs[index];
            const width = cost === 0 ? 0 : Math.max(4, cost / maxCost * 100);
            const reqAmount = Number(snapshot.userRequiredAmount ?? 0);
            const subLabel = reqAmount > 0
                ? `본인 부담 ${formatCompactMoney(reqAmount)}`
                : (snapshot.newLoanAmount > 0 ? `대출 ${formatCompactMoney(snapshot.newLoanAmount)}` : "");

            return `
                <article class="lifecycle-cost-row">
                    <div class="lifecycle-cost-label">
                        <strong>${escapeHtml(eventTypeLabel(snapshot.eventType))}</strong>
                        <time>${escapeHtml(formatEventDate(snapshot.eventDate))}</time>
                    </div>
                    <div class="lifecycle-cost-visual">
                        <div class="lifecycle-cost-track" aria-hidden="true">
                            <span style="width: ${width.toFixed(1)}%"></span>
                        </div>
                        <div class="lifecycle-cost-values">
                            <b>지출 ${escapeHtml(formatApproxMoney(cost))}</b>
                            ${subLabel ? `<em class="is-highlight-stat">${escapeHtml(subLabel)}</em>` : ""}
                        </div>
                    </div>
                </article>
            `;
        }).join("");
    }

    function buildAssetChart(points) {
        const width = 920;
        const height = 280;
        const padding = {top: 28, right: 28, bottom: 48, left: 72};
        const values = points.map(point => point.netAsset);
        const rawMin = Math.min(0, ...values);
        const rawMax = Math.max(0, ...values);
        const range = Math.max(rawMax - rawMin, 10000000);
        const min = rawMin - range * .12;
        const max = rawMax + range * .12;
        const chartWidth = width - padding.left - padding.right;
        const chartHeight = height - padding.top - padding.bottom;
        const x = index => padding.left + (
            points.length === 1 ? chartWidth / 2 : chartWidth * index / (points.length - 1)
        );
        const y = value => padding.top + (max - value) / (max - min) * chartHeight;
        const path = points.map((point, index) =>
            `${index === 0 ? "M" : "L"} ${x(index).toFixed(1)} ${y(point.netAsset).toFixed(1)}`
        ).join(" ");
        const area = `${path} L ${x(points.length - 1).toFixed(1)} ${(padding.top + chartHeight).toFixed(1)} L ${x(0).toFixed(1)} ${(padding.top + chartHeight).toFixed(1)} Z`;
        const grid = [0, 1, 2, 3].map(index => {
            const value = max - (max - min) * index / 3;
            const py = y(value);
            return `
                <line x1="${padding.left}" y1="${py}" x2="${width - padding.right}" y2="${py}" />
                <text x="${padding.left - 12}" y="${py + 4}" text-anchor="end">${escapeHtml(formatAxisMoney(value))}</text>
            `;
        }).join("");
        const markers = points.map((point, index) => `
            <g class="lifecycle-chart-marker" data-point-index="${index}">
                <circle cx="${x(index)}" cy="${y(point.netAsset)}" r="6" />
                <text class="lifecycle-chart-value" x="${x(index)}" y="${y(point.netAsset) - 14}" text-anchor="middle">${escapeHtml(formatCompactMoney(point.netAsset))}</text>
                <text class="lifecycle-chart-year" x="${x(index)}" y="${height - 17}" text-anchor="middle">${escapeHtml(journeyYear(point.date))}</text>
            </g>
        `).join("");

        return `
            <svg viewBox="0 0 ${width} ${height}" role="img" aria-label="연도별 순자산 변화 차트">
                <g class="lifecycle-chart-grid">${grid}</g>
                <path class="lifecycle-chart-area" d="${area}" />
                <path class="lifecycle-chart-line" d="${path}" />
                ${markers}
            </svg>
        `;
    }

    function openSnapshotModal(snapshot, snapshotIndex = null) {
        if (!snapshotModal || !snapshot) {
            return;
        }

        const typeKey = lifecycleEventTypes[snapshot.eventType] || String(snapshot.eventType).toLowerCase();
        const icon = getEventIcon(typeKey);
        const title = lifecycleEventNames[typeKey] || eventTypeLabel(snapshot.eventType);
        const idx = snapshotIndex !== null ? snapshotIndex : (latestSimulationResult?.eventSnapshots?.findIndex(s => s === snapshot) ?? 0);
        const stepNum = idx >= 0 ? idx + 1 : 1;

        if (snapshotModalTitle) {
            snapshotModalTitle.textContent = `${title} 상세 분석 보고서`;
        }
        if (snapshotModalEventDate) {
            snapshotModalEventDate.textContent = `STEP ${stepNum} · ${formatEventDate(snapshot.eventDate)}`;
        }

        const calcDetailsHtml = buildEventCalculationDetails(snapshot, idx);

        const contributionMetrics = snapshot.eventType === "MARRIAGE"
            ? `
                ${modalMetric("본인 비용 분담액", snapshot.userContributionAmount)}
                ${modalMetric("가족 지원금", snapshot.familySupportAmount)}
            `
            : "";

        snapshotModalBody.innerHTML = `
            <div class="lifecycle-modal-result-wrapper">
                <div class="modal-event-hero">
                    <span class="modal-event-icon material-symbols-outlined">${icon}</span>
                    <div class="modal-event-hero-info">
                        <div class="modal-event-hero-badges">
                            <span class="snapshot-step-badge">STEP ${stepNum}</span>
                            <span class="modal-date-badge">${formatEventDate(snapshot.eventDate)}</span>
                        </div>
                        <h3>${escapeHtml(title)}</h3>
                    </div>
                    <div class="modal-hero-cost">
                        <span>예상 소요 비용</span>
                        <strong>${formatCompactMoney(snapshot.eventCost || snapshot.estimatedCost)}</strong>
                    </div>
                </div>

                ${calcDetailsHtml ? `
                    <section class="lifecycle-modal-block">
                        <h4>📋 세부 산출 내역</h4>
                        ${calcDetailsHtml}
                    </section>
                ` : ''}

                ${renderFeasibility(snapshot.feasibility)}

                <section class="lifecycle-modal-block">
                    <h4>💰 비용 및 자금 조달 요약</h4>
                    <div class="lifecycle-modal-grid">
                        ${modalMetric("총 예상비용", snapshot.estimatedCost || snapshot.eventCost)}
                        ${contributionMetrics}
                        ${modalMetric("확정 공공지원금", snapshot.supportBenefit)}
                        ${modalMetric("최종 본인 필요 자금", snapshot.userRequiredAmount)}
                        ${snapshot.newLoanAmount > 0 ? modalMetric("필요 대출금액", snapshot.newLoanAmount) : ""}
                        ${snapshot.additionalMonthlyExpense > 0 ? modalMetric("월 추가지출", snapshot.additionalMonthlyExpense) : ""}
                    </div>
                </section>

                <section class="lifecycle-modal-block">
                    <h4>🏛️ 맞춤 복지 혜택</h4>
                    ${renderSupportList(snapshot.supports)}
                </section>

                <section class="lifecycle-modal-block">
                    <h4>🏦 추천 금융상품</h4>
                    ${renderProductList(snapshot.recommendedProducts)}
                </section>

                <div class="lifecycle-modal-actions">
                    <button type="button"
                            class="lifecycle-secondary-button"
                            data-edit-lifecycle-survey>
                        설문 내역 수정하기
                    </button>
                    <button type="button"
                            class="lifecycle-primary-button"
                            data-close-snapshot-modal>
                        확인 완료
                    </button>
                </div>
            </div>
        `;

        snapshotModalBody
            .querySelector("[data-edit-lifecycle-survey]")
            ?.addEventListener("click", () => {
                closeSnapshotModal();
                showStep("events");
                openEventForm(typeKey);
            });

        snapshotModalBody
            .querySelectorAll("[data-close-snapshot-modal]")
            .forEach(btn => btn.addEventListener("click", closeSnapshotModal));

        snapshotModal.hidden = false;
        document.body.classList.add("lifecycle-modal-open");
    }

    function renderFeasibility(feasibility) {
        if (!feasibility) {
            return "";
        }

        const status = String(feasibility.status ?? "READY").toLowerCase();
        const delayText = feasibility.recommendedDelayMonths
            ? `<strong>권장 준비기간: 약 ${Number(feasibility.recommendedDelayMonths).toLocaleString("ko-KR")}개월</strong>`
            : "";

        return `
            <section class="lifecycle-feasibility ${escapeHtml(status)}">
                <span>PLAN CHECK</span>
                <h4>${escapeHtml(feasibility.title ?? "계획 분석")}</h4>
                <p>${escapeHtml(feasibility.message ?? "")}</p>
                ${delayText}
            </section>
        `;
    }

    function closeSnapshotModal() {
        if (!snapshotModal) {
            return;
        }

        snapshotModal.hidden = true;
        document.body.classList.remove("lifecycle-modal-open");
    }

    document
        .querySelectorAll("[data-close-snapshot-modal]")
        .forEach(button => {
            button.addEventListener("click", closeSnapshotModal);
        });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeSnapshotModal();
        }
    });

    function modalMetric(label, value) {
        return modalPlainMetric(label, `${formatMoney(value)}원`);
    }

    function modalPlainMetric(label, value) {
        return `
            <article>
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value)}</strong>
            </article>
        `;
    }

    function renderSupportList(supports) {
        if (!supports || supports.length === 0) {
            return `<p class="lifecycle-modal-empty">추천 복지 정보가 없습니다.</p>`;
        }

        return `
            <div class="lifecycle-recommendation-list">
                ${supports.map(support => {
                    const status = support.recommendationStatus ?? "NEEDS_CONFIRMATION";
                    const statusClass = status.toLowerCase().replace(/_/g, "-");
                    const statusLabel = {
                        ELIGIBLE: "신청 가능",
                        NEEDS_CONFIRMATION: "확인 필요",
                        NOT_ELIGIBLE: "대상 아님"
                    }[status] ?? status;

                    const dateText = support.sourceUpdatedAt ? `기준일: ${escapeHtml(support.sourceUpdatedAt)}` : "";
                    const reasonText = support.eligibilityReason ? `<div class="lifecycle-eligibility-reason">${escapeHtml(support.eligibilityReason)}</div>` : "";

                    return `
                        <article class="lifecycle-recommendation-item">
                            <div class="lifecycle-recommendation-header">
                                <strong>${escapeHtml(support.supportName ?? "복지 지원")}</strong>
                                <span class="lifecycle-status-badge ${escapeHtml(statusClass)}">${escapeHtml(statusLabel)}</span>
                            </div>
                            <p class="lifecycle-source-meta">
                                <span>${escapeHtml(support.sourceName ?? "복지로 / 서민금융진흥원")}</span>
                                ${dateText ? `<span>· ${dateText}</span>` : ""}
                            </p>
                            ${reasonText}
                            <span class="lifecycle-benefit-tag">${escapeHtml(support.effectType ?? "-")}${support.amount ? ` · ${formatMoney(support.amount)}원` : ""}</span>
                            ${support.sourceUrl
                                ? `<a href="${escapeHtml(support.sourceUrl)}" target="_blank" rel="noopener" class="lifecycle-link-btn">출처 보기</a>`
                                : ""}
                        </article>
                    `;
                }).join("")}
            </div>
        `;
    }

    function renderProductList(products) {
        if (!products || products.length === 0) {
            return `<p class="lifecycle-modal-empty">추천 금융상품이 없습니다.</p>`;
        }

        return `
            <div class="lifecycle-recommendation-list">
                ${products.map(product => {
                    const status = product.recommendationStatus ?? "ELIGIBLE";
                    const statusClass = status.toLowerCase().replace(/_/g, "-");
                    const statusLabel = {
                        ELIGIBLE: "신청 가능",
                        NEEDS_CONFIRMATION: "확인 필요",
                        NOT_ELIGIBLE: "대상 아님"
                    }[status] ?? status;

                    const dateText = product.sourceUpdatedAt ? `기준일: ${escapeHtml(product.sourceUpdatedAt)}` : "";
                    const reasonText = product.eligibilityReason ? `<div class="lifecycle-eligibility-reason">${escapeHtml(product.eligibilityReason)}</div>` : "";

                    return `
                        <article class="lifecycle-recommendation-item">
                            <div class="lifecycle-recommendation-header">
                                <strong>${escapeHtml(product.productName ?? "금융상품")}</strong>
                                <span class="lifecycle-status-badge ${escapeHtml(statusClass)}">${escapeHtml(statusLabel)}</span>
                            </div>
                            <p class="lifecycle-source-meta">
                                <span>${escapeHtml(product.institutionName ?? product.sourceName ?? "금융기관")}</span>
                                <span>· ${escapeHtml(product.productType ?? "-")}</span>
                                ${dateText ? `<span>· ${dateText}</span>` : ""}
                            </p>
                            ${reasonText}
                            <div class="lifecycle-product-terms">
                                <span>금리: <strong>${escapeHtml(product.interestRate ?? "상담 후 결정")}</strong></span>
                                <span>한도: <strong>${escapeHtml(product.loanLimit ?? "-")}</strong></span>
                            </div>
                            ${product.relatedUrl
                                ? `<a href="${escapeHtml(product.relatedUrl)}" target="_blank" rel="noopener" class="lifecycle-link-btn">상품 보기</a>`
                                : ""}
                        </article>
                    `;
                }).join("")}
            </div>
        `;
    }

    function eventTypeLabel(eventType) {
        return {
            MARRIAGE: "결혼",
            CHILDBIRTH: "출산",
            VEHICLE_PURCHASE: "차량 구매",
            MONTHLY_RENT: "월세",
            JEONSE: "전세",
            HOME_PURCHASE: "주택 구매",
            REPAYMENT: "대출 상환"
        }[eventType] ?? eventType ?? "-";
    }

    function formatEventDate(value) {
        if (!value) {
            return "일자 미정";
        }

        const [year, month, day] = String(value).split("-");
        return `${year}.${month}.${day}`;
    }

    function formatPercent(value) {
        if (value === null || value === undefined || value === "") {
            return "0%";
        }

        return `${Number(value).toLocaleString("ko-KR", {
            maximumFractionDigits: 2
        })}%`;
    }

    runSimulationBtn?.addEventListener(
        "click",
        runLifecycleSimulation
    );

    /*
     * =========================================================
     * 8. 공통 Utility
     * =========================================================
     */

    /**
     * 금액에 천 단위 콤마 표시
     *
     * 1500000
     * -> 1,500,000
     */
    function formatMoney(value) {

        if (
            value === null
            || value === undefined
            || value === ""
        ) {

            return "0";
        }

        const amount = Number(value);

        if (!Number.isFinite(amount)) {
            return "0";
        }

        return Math.round(amount).toLocaleString("ko-KR", {
            maximumFractionDigits: 0
        });
    }


    /*
     * =========================================================
     * 9. 페이지 최초 실행
     * =========================================================
     */

    async function ensureBaseSurvey() {
        try {
            const res = await fetch("/api/lifecycle/survey/base");
            if (res.status === 404) {
                await fetch("/api/lifecycle/survey/base", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        monthlyLivingExpense: 1500000,
                        currentHousingType: "MONTHLY_RENT",
                        monthlyHousingExpense: 500000,
                        industryCode: "SERVICE",
                        salaryGrowthScenario: "BASE"
                    })
                });
            }
            baseSurveyReady = true;
        } catch (error) {
            console.error("Base survey verification error", error);
            baseSurveyReady = true;
        }
    }

    async function initializeSurvey() {
        initDragAndDrop();

        document.getElementById("closeScenarioGateBtn")?.addEventListener("click", () => {
            if (scenarioGate) {
                scenarioGate.hidden = true;
            }
        });

        await ensureBaseSurvey();

        if (requestedScenarioId) {
            try {
                await selectScenario(requestedScenarioId);
                return;
            } catch (error) {
                console.error("요청한 시나리오 조회 오류", error);
                updateScenarioUrl(null);
            }
        }

        try {
            const response = await fetch("/api/lifecycle/scenarios");
            if (response.ok) {
                const scenarios = await response.json();
                renderScenarioList(scenarios);
                if (scenarios && scenarios.length > 0) {
                    const firstScenarioId = getResponseField(scenarios[0], "scenarioId");
                    await selectScenario(firstScenarioId);
                    return;
                }
            }
        } catch (error) {
            console.error("시나리오 목록 조회 실패", error);
        }

        try {
            const createRes = await fetch("/api/lifecycle/scenarios", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ scenarioName: "나의 미래 라이프 플랜" })
            });
            if (createRes.ok) {
                const scenario = await createRes.json();
                await selectScenario(getResponseField(scenario, "scenarioId"));
                return;
            }
        } catch (e) {
            console.error("기본 시나리오 생성 오류", e);
        }

        showStep("events");
    }

    initializeSurvey();

});
function formatKoreanMoney(value) {
    if (
        value === null
        || value === undefined
        || value === ""
    ) {
        return "0원";
    }

    const numberValue = Number(
        String(value).replace(/,/g, "")
    );

    if (!Number.isFinite(numberValue)) {
        return "0원";
    }

    const sign = numberValue < 0 ? "-" : "";
    const amount = Math.abs(Math.trunc(numberValue));

    if (amount < 10000) {
        return `${sign}${amount.toLocaleString("ko-KR")}원`;
    }

    const eok = Math.floor(amount / 100000000);
    const man = Math.floor((amount % 100000000) / 10000);

    if (eok > 0 && man > 0) {
        return `${sign}${eok.toLocaleString("ko-KR")}억 ${man.toLocaleString("ko-KR")}만원`;
    }

    if (eok > 0) {
        return `${sign}${eok.toLocaleString("ko-KR")}억원`;
    }

    return `${sign}${man.toLocaleString("ko-KR")}만원`;
}

function formatMoneyDisplay(value) {
    const wonText = `${formatMoney(value)}원`;
    const koreanText = formatKoreanMoney(value);

    return wonText === koreanText
        ? wonText
        : `${wonText} (${koreanText})`;
}

function formatCompactMoney(value) {
    if (value === null || value === undefined || value === "") return "0원";
    const amount = Number(String(value).replace(/,/g, ""));
    if (!Number.isFinite(amount)) return "0원";
    const sign = amount < 0 ? "-" : "";
    const absolute = Math.abs(amount);
    const eok = Math.floor(absolute / 100000000);
    const remainder = absolute % 100000000;
    const man = Math.round(remainder / 10000);
    if (eok > 0 && man > 0) return `${sign}${eok}억 ${man.toLocaleString("ko-KR")}만원`;
    if (eok > 0) return `${sign}${eok}억원`;
    if (man > 0) return `${sign}${man.toLocaleString("ko-KR")}만원`;
    if (absolute > 0) return `${sign}${Math.round(absolute).toLocaleString("ko-KR")}원`;
    return "0원";
}
