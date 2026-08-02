const signupForm = document.querySelector('#signupForm');
const password = document.querySelector('#password');
const passwordConfirm = document.querySelector('#passwordConfirm');
const passwordMessage = document.querySelector('#passwordMessage');
const loginEmail = document.querySelector('#loginEmail');
const sendCodeButton = document.querySelector('#sendCodeButton');
const verificationArea = document.querySelector('#verificationArea');
const verificationCode = document.querySelector('#verificationCode');
const verifyCodeButton = document.querySelector('#verifyCodeButton');
const emailVerificationMessage = document.querySelector('#emailVerificationMessage');

function checkPasswordMatch() {
    if (!passwordConfirm.value) {
        passwordMessage.textContent = '';
        return false;
    }

    if (password.value === passwordConfirm.value) {
        passwordMessage.textContent =
            '비밀번호가 일치합니다.';
        passwordMessage.className = 'success-message';
        return true;
    }

    passwordMessage.textContent =
        '비밀번호가 일치하지 않습니다.';
    passwordMessage.className = 'field-error';
    return false;
}

password.addEventListener('input', checkPasswordMatch);
passwordConfirm.addEventListener(
    'input',
    checkPasswordMatch
);

signupForm.addEventListener('submit', event => {
    if (!checkPasswordMatch()) {
        event.preventDefault();
        passwordConfirm.focus();
    }
});

sendCodeButton.addEventListener('click', () => {
    if (!loginEmail.checkValidity()) {
        loginEmail.reportValidity();
        return;
    }

    // TODO: 이메일 인증번호 전송 API 호출
    verificationArea.hidden = false;
    verificationArea.classList.remove('hidden');
    verificationCode.focus();

    emailVerificationMessage.textContent =
        '인증번호를 입력해주세요.';
    emailVerificationMessage.className = 'success-message';
});

verifyCodeButton.addEventListener('click', () => {
    if (!verificationCode.value.trim()) {
        emailVerificationMessage.textContent =
            '인증번호를 입력해주세요.';
        emailVerificationMessage.className = 'field-error';
        verificationCode.focus();
        return;
    }

    // TODO: 인증번호 확인 API 호출
});