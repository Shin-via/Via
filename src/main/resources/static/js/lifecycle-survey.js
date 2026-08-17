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
    let baseSurveyReady = false;

    async function ensureScenario() {
        if (!baseSurveyReady) {
            throw new Error("기본 생활정보를 먼저 저장해주세요.");
        }
        if (!scenarioId) {
            throw new Error("진행할 시나리오를 먼저 선택해주세요.");
        }
        return scenarioId;
    }

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
        return yearMonth ? `${yearMonth}-01` : null;
    }

    function toYearMonth(serverDate) {
        return serverDate ? serverDate.substring(0, 7) : "";
    }

    function formatYearMonth(yearMonth) {
        if (!yearMonth) {
            return "시기 미정";
        }

        const [year, month] = yearMonth.split("-");
        return `${year}년 ${Number(month)}월`;
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
            control.disabled
            || control.dataset.optional === "true"
            || control.closest("[hidden]")
            || ["hidden", "button", "submit", "reset", "checkbox"]
                .includes(control.type)
        ) {
            return false;
        }

        if (control.type === "radio") {
            return !form.querySelector(
                `input[type="radio"][name="${control.name}"]:checked`
            );
        }

        return control.value.trim() === ""
            || !control.checkValidity();
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
                const group = control.closest(
                    ".lifecycle-form-group"
                );

                if (!group?.classList.contains("has-error")) {
                    return;
                }

                const form = control.closest("form");
                const stillInvalid = Array.from(
                    group.querySelectorAll(
                        "input, select, textarea"
                    )
                ).some(item =>
                    isSurveyControlIncomplete(item, form)
                );

                if (!stillInvalid) {
                    clearSurveyFieldError(group);
                }
            });
        });
    });


    function setBaseSurveyReady(ready) {
        baseSurveyReady = ready;
        if (scenarioGate) {
            scenarioGate.hidden = !ready;
        }
        stepButtons.forEach(button => {
            if (button.dataset.step !== "base") {
                button.disabled = !ready || !scenarioId;
            }
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
        renderTimeline();
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
        if (!baseSurveyReady) {
            alert("기본 생활정보를 먼저 저장해주세요.");
            return;
        }

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
        showStep("base");
        setScenarioListExpanded(true);
        try {
            await loadScenarioList();
        } catch (error) {
            console.error(error);
        }
        scenarioGate.scrollIntoView({ behavior: "smooth", block: "start" });
    });

    /*
     * =========================================================
     * 2. STEP 화면 이동
     * =========================================================
     */

    /**
     * 원하는 STEP 화면을 표시한다.
     *
     * base   -> 기본정보
     * events -> 생활 이벤트
     * review -> 입력 확인
     * result -> 결과 보기
     */
    function showStep(stepName) {

        if (stepName !== "base" && !baseSurveyReady) {
            alert("기본 생활정보를 먼저 저장해주세요.");
            stepName = "base";
        } else if (stepName !== "base" && !scenarioId) {
            alert("진행할 시나리오를 먼저 선택해주세요.");
            stepName = "base";
        }

        // 모든 STEP 패널 숨김
        stepPanels.forEach(panel => {

            panel.hidden =
                panel.dataset.stepPanel !== stepName;
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

        if (stepName === "result" && latestSimulationResult) {
            renderSimulationResult(latestSimulationResult);
        }

        const activePanel =
            document.querySelector(
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

                const targetRadio =
                    document.querySelector(
                        `input[name="salaryGrowthScenario"]
                        [value="${data.salaryGrowthScenario}"]`
                    );

                /*
                 * 위 querySelector가 줄바꿈 때문에
                 * 정상 선택되지 않을 수 있으므로
                 * 실제 선택은 아래 방식으로 처리
                 */
                salaryScenarioRadios.forEach(radio => {

                    radio.checked =
                        radio.value ===
                        data.salaryGrowthScenario;
                });
            }


            /*
             * CUSTOM 저장값이 있다면
             * 소수 -> % 변환해서 화면 표시
             */
            if (
                data.salaryGrowthScenario === "CUSTOM"
            ) {

                customSalaryGrowthArea.hidden = false;

                if (
                    data.customSalaryGrowthRate != null
                ) {

                    customSalaryGrowthRate.value =
                        Number(
                            data.customSalaryGrowthRate
                        ) * 100;
                }

            } else {

                customSalaryGrowthArea.hidden = true;
            }

            moneyInputs.forEach(formatMoneyInput);
            const complete = checkBaseSurveyComplete();
            setBaseSurveyReady(complete);

            return complete;


        } catch (error) {

            /*
             * 기본설문 조회 실패가
             * 전체 페이지 이용을 막으면 안 되므로
             * console에만 기록한다.
             */
            console.error(
                "기본 생활정보 조회 오류",
                error
            );
            setBaseSurveyReady(false);
            return false;
        }
    }


    /*
     * =========================================================
     * 4. 이벤트 선택
     * =========================================================
     */

    function openEventForm(eventType) {
        selectedEventType = eventType;
        selectedEventTypes.add(eventType);

        eventForms.forEach(form => {
            form.hidden = true;
        });

        const targetForm = document.querySelector(
            `[data-event-form="${eventType}"]`
        );

        if (targetForm) {
            targetForm.hidden = false;
            targetForm.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }

        updateEventSelectionUi();
    }

    function updateEventSelectionUi() {
        eventButtons.forEach(button => {
            const selected = selectedEventTypes.has(
                button.dataset.eventType
            );
            button.classList.toggle("selected", selected);
            button.setAttribute("aria-pressed", String(selected));
        });
    }

    async function removeEvent(eventType) {
        const eventId = savedEventIds[eventType];

        if (eventId) {
            const confirmed = window.confirm(
                `${lifecycleEventNames[eventType]} 이벤트를 삭제하시겠습니까?\n저장된 설문 내용도 함께 삭제됩니다.`
            );

            if (!confirmed) {
                openEventForm(eventType);
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
        renderTimeline();
    }

    eventButtons.forEach(button => {
        button.addEventListener("click", async () => {
            const eventType = button.dataset.eventType;

            if (!selectedEventTypes.has(eventType)) {
                openEventForm(eventType);
                return;
            }

            button.disabled = true;
            try {
                await removeEvent(eventType);
            } catch (error) {
                console.error(error);
                alert("이벤트 삭제 중 오류가 발생했습니다.");
            } finally {
                button.disabled = false;
            }
        });
    });

    function addOrUpdateTimelineEvent(type, targetDate, eventId = null) {
        if (!targetDate) {
            return;
        }

        const index = lifecycleEvents.findIndex(event => event.type === type);
        const timelineEvent = {
            type,
            targetDate,
            title: lifecycleEventNames[type] ?? type,
            eventId
        };

        if (index >= 0) {
            lifecycleEvents[index] = timelineEvent;
        } else {
            lifecycleEvents.push(timelineEvent);
        }

        lifecycleEvents.sort((a, b) =>
            a.targetDate.localeCompare(b.targetDate)
        );
        renderTimeline();
    }

    function renderTimeline() {
        const timeline = document.getElementById("lifecycleTimeline");
        const empty = document.getElementById("lifecycleTimelineEmpty");

        if (!timeline || !empty) {
            return;
        }

        if (lifecycleEvents.length === 0) {
            empty.hidden = false;
            timeline.hidden = true;
            timeline.replaceChildren();
            return;
        }

        empty.hidden = true;
        timeline.hidden = false;
        timeline.innerHTML = lifecycleEvents.map((event, index) => `
            <div class="lifecycle-timeline-item">
                <button type="button"
                        class="lifecycle-timeline-node"
                        data-timeline-event="${event.type}">
                    <span class="timeline-date">${formatYearMonth(event.targetDate)}</span>
                    <span class="timeline-dot"></span>
                    <strong>${event.title}</strong>
                    <span class="timeline-edit">상세설정</span>
                </button>
                ${index < lifecycleEvents.length - 1
                    ? '<div class="timeline-line"></div>'
                    : ''}
            </div>
        `).join("");

        timeline.querySelectorAll("[data-timeline-event]")
            .forEach(button => {
                button.addEventListener("click", () => {
                    openEventForm(button.dataset.timelineEvent);
                });
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
        renderTimeline();
        await loadEventDetails(events);
    }

    const eventApiPaths = {
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

    async function saveEventSurvey(eventType, form, button) {
        const apiPath = eventApiPaths[eventType];

        if (!apiPath) {
            return;
        }

        const currentScenarioId = await ensureScenario();
        const eventId = savedEventIds[eventType];
        const url = eventId
            ? `/api/lifecycle/survey/${apiPath}/${eventId}`
            : `/api/lifecycle/survey/scenario/${currentScenarioId}/${apiPath}`;

        button.disabled = true;

        try {
            const response = await fetch(url, {
                method: eventId ? "PUT" : "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(buildEventRequest(form))
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
            button.textContent = "저장 완료";
        } catch (error) {
            console.error(error);
            alert("이벤트 저장 중 오류가 발생했습니다.");
        } finally {
            button.disabled = false;
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
                saveEventSurvey(eventType, form, saveButton);
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
    async function saveMarriageSurvey() {

        const targetDate =
            document.getElementById(
                "marriageTargetDate"
            ).value;

        const lifestyle =
            document.querySelector(
                'input[name="marriageLifestyleLevel"]:checked'
            );

        const guestCount =
            document.getElementById(
                "marriageGuestCount"
            ).value;

        const furnitureIncluded =
            document.getElementById(
                "marriageFurnitureIncluded"
            ).checked;

        const honeymoonIncluded =
            document.getElementById(
                "marriageHoneymoonIncluded"
            ).checked;

        const contributionRate =
            document.getElementById(
                "marriageUserContributionRate"
            ).value;

        const familySupportAmount =
            document.getElementById(
                "marriageFamilySupportAmount"
            ).value;


        /*
         * 필수값 검사
         */
        if (!targetDate) {

            alert("결혼 예정일을 입력해주세요.");
            return;
        }

        if (!lifestyle) {

            alert("결혼 준비 수준을 선택해주세요.");
            return;
        }

        if (!guestCount) {

            alert("예상 하객 수를 입력해주세요.");
            return;
        }


        /*
         * CUSTOM이면 예상 결혼비용 필수
         */
        if (
            lifestyle.value === "CUSTOM"
            && !marriageCustomEstimatedCost.value
        ) {

            alert(
                "예상 결혼 총비용을 입력해주세요."
            );

            return;
        }


        /*
         * MarriageSurveyRequest와 동일한 구조
         */
        const requestData = {

            targetDate:
            toServerDate(targetDate),

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
                        marriageCustomEstimatedCost.value
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


                alert("결혼 계획이 저장되었습니다.");


                /*
                 * 현재 POST 응답이 eventId를 반환하지 않으므로
                 * 이후 eventId 반환 방식으로 변경하면
                 * savedEventIds.marriage에 저장하면 된다.
                 */

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


                alert("결혼 계획이 수정되었습니다.");
            }

            addOrUpdateTimelineEvent(
                "marriage",
                targetDate,
                savedEventIds.marriage
            );


        } catch (error) {

            console.error(error);

            alert(
                "결혼 계획 저장 중 오류가 발생했습니다."
            );
        }
    }


    /*
     * 결혼 저장 버튼
     */
    if (saveMarriageSurveyBtn) {

        saveMarriageSurveyBtn.addEventListener(
            "click",
            saveMarriageSurvey
        );
    }


    /*
     * =========================================================
     * 6. 입력 내용 확인
     * =========================================================
     */

    /**
     * STEP 3에서 기본정보와 이벤트를 표시한다.
     */
    async function loadReview() {

        await loadBaseSurveySummary();

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

        const summaryArea =
            document.getElementById(
                "eventSurveySummary"
            );


        if (!summaryArea) {
            return;
        }


        if (selectedEventTypes.size === 0) {

            summaryArea.innerHTML =
                "<p>선택한 생활 이벤트가 없습니다.</p>";

            return;
        }


        const eventNames = {

            marriage: "결혼",

            childbirth: "출산",

            vehicle: "차량 구매",

            "monthly-rent": "월세",

            jeonse: "전세",

            "home-purchase": "주택 구매",

            repayment: "대출 상환"
        };


        summaryArea.innerHTML = Array.from(
            selectedEventTypes
        ).map(eventType => {

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
                    <h4>${escapeHtml(
                        eventNames[eventType] ?? eventType
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
        const annualIncome =
            document.getElementById("simulationAnnualIncome");

        const liquidAssetAmount =
            document.getElementById("simulationLiquidAssetAmount");

        const salaryGrowthRate =
            document.getElementById("simulationSalaryGrowthRate");

        const monthlyLivingExpense =
            document.getElementById("monthlyLivingExpense");

        const monthlyHousingExpense =
            document.getElementById("monthlyHousingExpense");

        return {
            baseDate: new Date().toISOString().substring(0, 10),
            annualIncome: parseMoneyValue(annualIncome?.value),
            liquidAssetAmount: parseMoneyValue(liquidAssetAmount?.value),
            monthlyLivingExpense: parseMoneyValue(monthlyLivingExpense?.value),
            monthlyHousingExpense: parseMoneyValue(monthlyHousingExpense?.value),
            annualSalaryGrowthRate: Number(salaryGrowthRate?.value || 0) / 100,
            loans: []
        };
    }

    async function runLifecycleSimulation() {
        if (!scenarioId) {
            alert("시뮬레이션할 시나리오를 먼저 선택해주세요.");
            return;
        }

        runSimulationBtn.disabled = true;
        runSimulationBtn.textContent = "계산 중...";

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
                throw new Error(`시뮬레이션 실패: ${response.status}`);
            }

            latestSimulationResult = await response.json();
            renderSimulationResult(latestSimulationResult);

        } catch (error) {
            console.error(error);
            alert("시뮬레이션 실행 중 오류가 발생했습니다.");
        } finally {
            runSimulationBtn.disabled = false;
            runSimulationBtn.textContent = "시뮬레이션 실행";
        }
    }

    function renderSimulationResult(result) {
        const snapshots = result.eventSnapshots ?? [];

        if (simulationEmpty) {
            simulationEmpty.hidden = snapshots.length > 0;
        }

        if (simulationSummary) {
            simulationSummary.hidden = false;
            simulationSummary.innerHTML = `
                <article>
                    <span>최종 순자산</span>
                    <strong>${formatMoney(result.finalNetAsset)}원</strong>
                </article>
                <article>
                    <span>순자산 변화</span>
                    <strong>${formatMoney(result.netAssetChange)}원</strong>
                </article>
                <article>
                    <span>총 이벤트 비용</span>
                    <strong>${formatMoney(result.totalEventCost)}원</strong>
                </article>
                <article>
                    <span>총 지원 혜택</span>
                    <strong>${formatMoney(result.totalSupportBenefit)}원</strong>
                </article>
                <article>
                    <span>최종 DSR</span>
                    <strong>${formatPercent(result.finalDsr)}</strong>
                </article>
            `;
        }

        if (simulationSnapshots) {
            simulationSnapshots.hidden = snapshots.length === 0;
            simulationSnapshots.innerHTML = snapshots.map((snapshot, index) =>
                renderSnapshotCard(snapshot, index)
            ).join("");

            simulationSnapshots
                .querySelectorAll("[data-snapshot-index]")
                .forEach(button => {
                    button.addEventListener("click", () => {
                        openSnapshotModal(
                            snapshots[Number(button.dataset.snapshotIndex)]
                        );
                    });
                });
        }
    }

    function renderSnapshotCard(snapshot, index) {
        const supportCount = (snapshot.supports ?? []).length;
        const productCount = (snapshot.recommendedProducts ?? []).length;
        const feasibility = snapshot.feasibility ?? {};
        const feasibilityStatus = feasibility.status ?? "READY";

        return `
            <button type="button"
                    class="lifecycle-snapshot-card"
                    data-snapshot-index="${index}">
                <span>${escapeHtml(formatEventDate(snapshot.eventDate))}</span>
                <strong>${escapeHtml(eventTypeLabel(snapshot.eventType))}</strong>
                <small>${escapeHtml(snapshot.summary ?? "")}</small>
                <span class="lifecycle-feasibility-badge ${escapeHtml(feasibilityStatus.toLowerCase())}">
                    ${escapeHtml(feasibility.title ?? "계획 분석")}
                </span>

                <div class="lifecycle-snapshot-metrics">
                    <em>비용 ${formatMoney(snapshot.eventCost)}원</em>
                    <em>부족 ${formatMoney(snapshot.fundingShortage)}원</em>
                    <em>복지 ${supportCount}개</em>
                    <em>상품 ${productCount}개</em>
                </div>
            </button>
        `;
    }

    function openSnapshotModal(snapshot) {
        if (!snapshotModal || !snapshot) {
            return;
        }

        snapshotModalTitle.textContent =
            eventTypeLabel(snapshot.eventType);

        snapshotModalEventDate.textContent =
            formatEventDate(snapshot.eventDate);

        const contributionMetrics = snapshot.eventType === "MARRIAGE"
            ? `
                ${modalMetric("본인 비용 분담액", snapshot.userContributionAmount)}
                ${modalMetric("가족 지원금", snapshot.familySupportAmount)}
            `
            : "";

        snapshotModalBody.innerHTML = `
            ${renderFeasibility(snapshot.feasibility)}

            <section class="lifecycle-modal-block">
                <h4>비용 요약</h4>
                <div class="lifecycle-modal-grid">
                    ${modalMetric("예상비용", snapshot.estimatedCost)}
                    ${contributionMetrics}
                    ${modalMetric("확정 공공지원금", snapshot.supportBenefit)}
                    ${modalMetric("최종 사용자 부담금", snapshot.userRequiredAmount)}
                    ${modalMetric("부족 금액", snapshot.fundingShortage)}
                    ${modalMetric("신규 대출", snapshot.newLoanAmount)}
                    ${modalMetric("월 추가지출", snapshot.additionalMonthlyExpense)}
                </div>
            </section>

            <section class="lifecycle-modal-block">
                <h4>재무상태 변화</h4>
                <div class="lifecycle-modal-grid">
                    ${modalMetric("현금 변화", snapshot.cashAssetChange)}
                    ${modalMetric("부채 변화", snapshot.totalDebtChange)}
                    ${modalMetric("순자산 변화", snapshot.netAssetChange)}
                    ${modalMetric("월 저축여력 변화", snapshot.monthlySavingCapacityChange)}
                    ${modalPlainMetric("DSR 변화", formatPercent(snapshot.dsrChange))}
                </div>
            </section>

            <section class="lifecycle-modal-block">
                <h4>복지 추천</h4>
                ${renderSupportList(snapshot.supports)}
            </section>

            <section class="lifecycle-modal-block">
                <h4>금융상품 추천</h4>
                ${renderProductList(snapshot.recommendedProducts)}
            </section>

            <div class="lifecycle-modal-actions">
                <button type="button"
                        class="lifecycle-secondary-button"
                        data-edit-lifecycle-survey>
                    설문 내역 수정하기
                </button>
            </div>
        `;

        snapshotModalBody
            .querySelector("[data-edit-lifecycle-survey]")
            ?.addEventListener("click", () => {
                closeSnapshotModal();
                showStep("review");
            });

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
                ${supports.map(support => `
                    <article class="lifecycle-recommendation-item">
                        <strong>${escapeHtml(support.supportName ?? "복지 지원")}</strong>
                        <p>
                            ${escapeHtml(support.sourceName ?? "출처 확인 필요")}
                            · ${escapeHtml(support.recommendationStatus ?? "확인 필요")}
                        </p>
                        <span>${escapeHtml(support.effectType ?? "-")} · ${formatMoney(support.amount)}원</span>
                        ${support.sourceUrl
            ? `<a href="${escapeHtml(support.sourceUrl)}" target="_blank" rel="noopener">출처 보기</a>`
            : ""}
                    </article>
                `).join("")}
            </div>
        `;
    }

    function renderProductList(products) {
        if (!products || products.length === 0) {
            return `<p class="lifecycle-modal-empty">추천 금융상품이 없습니다.</p>`;
        }

        return `
            <div class="lifecycle-recommendation-list">
                ${products.map(product => `
                    <article class="lifecycle-recommendation-item">
                        <strong>${escapeHtml(product.productName ?? "금융상품")}</strong>
                        <p>
                            ${escapeHtml(product.institutionName ?? "기관 정보 없음")}
                            · ${escapeHtml(product.productType ?? "-")}
                        </p>
                        <span>
                            ${escapeHtml(product.interestRate ?? "금리 정보 없음")}
                            · ${escapeHtml(product.loanLimit ?? "한도 정보 없음")}
                        </span>
                        ${product.relatedUrl
            ? `<a href="${escapeHtml(product.relatedUrl)}" target="_blank" rel="noopener">상품 보기</a>`
            : ""}
                    </article>
                `).join("")}
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

    async function initializeSurvey() {
        setBaseSurveyReady(false);
        checkBaseSurveyComplete();

        const hasBaseSurvey = await loadBaseSurvey();
        if (!hasBaseSurvey) {
            showStep("base");
            return;
        }

        if (requestedScenarioId) {
            try {
                await selectScenario(requestedScenarioId);
                return;
            } catch (error) {
                console.error("요청한 시나리오 조회 오류", error);
                updateScenarioUrl(null);
                alert("선택한 시나리오를 불러올 수 없습니다.");
            }
        }

        showStep("base");
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