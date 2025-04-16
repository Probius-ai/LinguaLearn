document.addEventListener('DOMContentLoaded', function() {
    // 탭 전환 기능
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', function() {
            // 활성 탭 변경
            tabButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // 해당 콘텐츠 표시
            const tabId = this.getAttribute('data-tab');
            tabContents.forEach(content => {
                content.classList.remove('active');
                if (content.id === tabId + '-tab') {
                    content.classList.add('active');
                }
            });

            // URL 해시 업데이트 (필요시)
            window.location.hash = tabId;
        });
    });

    // URL 해시에 따른 탭 초기화
    function initTabFromHash() {
        const hash = window.location.hash.substring(1);
        if (hash) {
            const tabButton = document.querySelector(`.tab-btn[data-tab="${hash}"]`);
            if (tabButton) {
                tabButton.click();
            }
        }
    }

    // 페이지 로드 시 해시에 따라 탭 초기화
    initTabFromHash();

    // 설정 저장 성공/실패 메시지 처리
    function showMessage() {
        const urlParams = new URLSearchParams(window.location.search);
        const successParam = urlParams.get('success');
        const errorParam = urlParams.get('error');

        if (successParam === 'settings-updated') {
            alert('설정이 성공적으로 저장되었습니다.');
        } else if (errorParam === 'settings-update-failed') {
            alert('설정 저장 중 오류가 발생했습니다. 다시 시도해주세요.');
        }
    }

    // 페이지 로드 시 메시지 확인
    showMessage();

    // CSRF 토큰 가져오기 (스프링 시큐리티)
    function getCSRFToken() {
        return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    }
});