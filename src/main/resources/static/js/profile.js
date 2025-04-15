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
            
            // 탭 변경 이벤트 서버에 기록 (선택적)
            logTabChange(tabId);
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
    
    // 즐겨찾기 제거 기능
    const removeFavoriteButtons = document.querySelectorAll('.remove-favorite');
    removeFavoriteButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const wordChip = this.closest('.word-chip');
            const wordText = wordChip.querySelector('span:first-child').textContent;
            
            // 확인 대화상자
            if (confirm(`'${wordText}' 단어를 즐겨찾기에서 제거하시겠습니까?`)) {
                // 시각적으로 먼저 제거 (실제 제거는 API 호출 후)
                wordChip.style.opacity = '0.5';
                
                // 서버에 제거 요청 보내기
                removeFavoriteWord(wordText)
                    .then(response => {
                        if (response.success) {
                            // 성공 시 요소 제거
                            wordChip.remove();
                        } else {
                            // 실패 시 원래 상태로 복원
                            wordChip.style.opacity = '1';
                            alert('즐겨찾기 제거에 실패했습니다. 다시 시도해주세요.');
                        }
                    })
                    .catch(error => {
                        console.error('즐겨찾기 제거 오류:', error);
                        wordChip.style.opacity = '1';
                        alert('서버 오류가 발생했습니다. 다시 시도해주세요.');
                    });
            }
        });
    });
    
    // 서버에 즐겨찾기 제거 요청 보내는 함수
    function removeFavoriteWord(word) {
        return fetch('/api/favorites/word/remove', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCSRFToken()
            },
            body: JSON.stringify({ word: word })
        })
        .then(response => response.json());
    }
    
    // 탭 변경 로깅 함수 (분석을 위한 선택적 기능)
    function logTabChange(tabId) {
        fetch('/api/log/tab-view', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': getCSRFToken()
            },
            body: JSON.stringify({ 
                tab: tabId,
                page: 'profile',
                timestamp: new Date().toISOString()
            })
        })
        .catch(error => {
            console.error('탭 로깅 오류:', error);
            // 로깅은 중요도가 낮으므로 실패해도 사용자에게 알리지 않음
        });
    }
    
    // CSRF 토큰 가져오기 (스프링 시큐리티)
    function getCSRFToken() {
        return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    }
});
