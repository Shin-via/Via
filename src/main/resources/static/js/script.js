document.addEventListener('DOMContentLoaded', () => {
    // 1. 사이드바 메누 클릭 시 활성화 변경 처리
    const sidebarLinks = document.querySelectorAll('.sidebar-link');

    sidebarLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();

            // 기존 active 제거 후 클릭된 요소에 추가
            sidebarLinks.forEach(item => item.classList.remove('active'));
            link.classList.add('active');
        });
    });

    // 2. 상단 네비게이션 메가 메뉴 마우스 인터랙션 보완 (선택사항)
    const megaContainers = document.querySelectorAll('.mega-menu-container');

    megaContainers.forEach(container => {
        const navLink = container.querySelector('.nav-link');

        container.addEventListener('mouseenter', () => {
            // 메뉴 오픈시 필요한 추가 동작 필요시 구현
        });

        container.addEventListener('mouseleave', () => {
            // 메뉴 닫힐 시 필요한 추가 동작 필요시 구현
        });
    });

    // 3. 검색 버튼 및 로그인 버튼 클릭 이벤트 핸들러 샘플
    const searchBtn = document.querySelector('.icon-button');
    const signInBtn = document.querySelector('.btn-signin');

    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            console.log('Search button clicked');
        });
    }

    if (signInBtn) {
        signInBtn.addEventListener('click', () => {
            console.log('Sign In button clicked');
        });
    }
});