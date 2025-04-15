document.addEventListener('DOMContentLoaded', function() {
    // 요소 가져오기
    const loginBtn = document.getElementById('login-btn');
    const modal = document.getElementById('login-modal');
    const overlay = document.getElementById('modal-overlay');
    const closeBtn = modal ? modal.querySelector('.close-modal') : null; // Check if modal exists

    // 모달 닫기 함수 (전역에서 접근 가능하도록 수정)
    window.closeLoginModal = function() {
        if (modal && overlay) {
            modal.classList.remove('active');
            overlay.classList.remove('active');
            document.body.style.overflow = ''; // 배경 스크롤 다시 활성화
        }
    }

    // 로그인 버튼이 없으면 (이미 로그인된 상태) 이후 로직 실행 안 함
    if (!loginBtn || !modal || !overlay || !closeBtn) {
        // console.log("Login button or modal elements not found, skipping modal setup.");
        return;
    }

    // 모달 열기 함수
    function openModal() {
        modal.classList.add('active');
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden'; // 배경 스크롤 방지
        // Optional: Clear previous errors when opening
        const authErrorDiv = document.getElementById('auth-error');
        if (authErrorDiv) authErrorDiv.textContent = '';
    }

    // 이벤트 리스너 등록
    loginBtn.addEventListener('click', openModal);
    closeBtn.addEventListener('click', window.closeLoginModal);
    overlay.addEventListener('click', window.closeLoginModal);

    // ESC 키로 모달 닫기
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && modal.classList.contains('active')) {
            window.closeLoginModal();
        }
    });

    // 모달 내부 클릭시 이벤트 버블링 방지
    modal.addEventListener('click', function(event) {
        // Close button 클릭은 버블링되어야 하므로 제외
        if (event.target !== closeBtn && !closeBtn.contains(event.target)) {
             event.stopPropagation();
        }
    });

    // Removed the original form submission handler
});
