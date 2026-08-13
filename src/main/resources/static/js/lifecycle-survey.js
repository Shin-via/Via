document.addEventListener("DOMContentLoaded", () => {
    const stepButtons = document.querySelectorAll("[data-step]");
    const stepPanels = document.querySelectorAll("[data-step-panel]");

    stepButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const selectedStep = button.dataset.step;
            stepButtons.forEach((item) => item.classList.toggle("active", item === button));
            stepPanels.forEach((panel) => {
                panel.hidden = panel.dataset.stepPanel !== selectedStep;
            });
        });
    });
});
