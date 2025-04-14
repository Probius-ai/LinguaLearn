document.addEventListener('DOMContentLoaded', function() {
    // 테마 토글 버튼
    const themeToggle = document.getElementById('theme-toggle');
    // 테마 드롭다운 버튼들
    const themeButtons = document.querySelectorAll('.theme-dropdown button');
    
    // 로컬 스토리지에서 테마 가져오기
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    }
    
    // 테마 토글 버튼 클릭 처리
    themeToggle.addEventListener('click', function() {
        // 현재 테마 확인
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        
        // 라이트/다크 모드 토글
        const newTheme = currentTheme === 'light' ? 'dark' : 'light';
        
        // 테마 적용
        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        
        // 서버에 테마 저장 (실제 서버 통신이 필요한 경우)
        saveThemeToServer(newTheme);
    });
    
    // 테마 드롭다운 버튼 클릭 처리
    themeButtons.forEach(button => {
        button.addEventListener('click', function() {
            const theme = this.getAttribute('data-theme');
            
            // 테마 적용
            document.documentElement.setAttribute('data-theme', theme);
            localStorage.setItem('theme', theme);
            
            // 서버에 테마 저장 (실제 서버 통신이 필요한 경우)
            saveThemeToServer(theme);
        });
    });
    
    // 테마를 서버에 저장하는 함수 (타임리프 + 스프링부트 연동)
    function saveThemeToServer(theme) {
        fetch('/api/user/theme', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCSRFToken()
            },
            body: JSON.stringify({ theme: theme })
        })
        .then(response => {
            // 응답 처리 (필요시)
            if (!response.ok) {
                console.log('테마 저장 중 오류 발생');
            }
        })
        .catch(error => {
            console.error('테마 저장 요청 실패:', error);
        });
    }
    
    // CSRF 토큰 가져오기 (스프링 시큐리티)
    function getCSRFToken() {
        return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    }
});
