const signupForm = document.querySelector('#signupForm');
const password = document.querySelector('#password');
const passwordConfirm = document.querySelector('#passwordConfirm');
const passwordMessage = document.querySelector('#passwordMessage');


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
