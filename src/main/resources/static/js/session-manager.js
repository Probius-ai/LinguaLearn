/**
 * 세션 관리를 위한 JavaScript
 * - 사용자 비활성 감지
 * - 세션 만료 알림
 * - 자동 로그아웃
 */
document.addEventListener('DOMContentLoaded', function() {
    // 설정
    const INACTIVE_TIMEOUT = 1800000; // 30분(밀리초)
    const WARNING_BEFORE_TIMEOUT = 60000; // 만료 1분 전 경고(밀리초)
    let inactivityTimer; // 비활성 타이머
    let warningTimer; // 경고 타이머
    let lastActivity = Date.now(); // 마지막 활동 시간
    let isAuthenticated = false; // 인증 상태
    
    // 인증 상태 확인
    checkAuthStatus();
    
    /**
     * 인증 상태 확인 함수
     */
    function checkAuthStatus() {
        fetch('/api/auth/status')
            .then(response => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error('Not authenticated');
                }
            })
            .then(data => {
                isAuthenticated = true;
                console.log('User is authenticated:', data);
                startSessionMonitoring();
            })
            .catch(err => {
                isAuthenticated = false;
                console.log('User is not authenticated');
                // 인증되지 않은 경우 세션 모니터링 없음
            });
    }
    
    /**
     * 세션 모니터링 시작
     */
    function startSessionMonitoring() {
        if (!isAuthenticated) return;
        
        // 이전 타이머가 있으면 초기화
        resetTimers();
        
        // 활동 이벤트 리스너 등록
        registerActivityListeners();
        
        // 비활성 타이머 시작
        inactivityTimer = setTimeout(handleInactiveUser, INACTIVE_TIMEOUT);
        
        // 경고 타이머 시작
        warningTimer = setTimeout(showTimeoutWarning, INACTIVE_TIMEOUT - WARNING_BEFORE_TIMEOUT);
        
        // 주기적으로 세션 갱신
        startSessionHeartbeat();
    }
    
    /**
     * 세션 하트비트 시작 (주기적으로 서버에 활성 상태 알림)
     */
    function startSessionHeartbeat() {
        // 10분마다 세션 갱신
        setInterval(() => {
            if (isAuthenticated && (Date.now() - lastActivity) < INACTIVE_TIMEOUT) {
                fetch('/api/auth/heartbeat', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                }).then(response => {
                    if (!response.ok) {
                        console.warn('Session heartbeat failed');
                    }
                }).catch(err => {
                    console.error('Error sending heartbeat:', err);
                });
            }
        }, 600000); // 10분(밀리초)
    }
    
    /**
     * 활동 이벤트 리스너 등록
     */
    function registerActivityListeners() {
        // 사용자 활동 이벤트
        const activityEvents = [
            'mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'
        ];
        
        // 각 이벤트에 대한 리스너 등록
        activityEvents.forEach(eventName => {
            document.addEventListener(eventName, resetUserActivity, true);
        });
    }
    
    /**
     * 사용자 활동 재설정
     */
    function resetUserActivity() {
        lastActivity = Date.now();
        resetTimers();
        
        // 경고창이 표시된 경우 닫기
        const warningModal = document.getElementById('session-timeout-warning');
        if (warningModal && warningModal.classList.contains('active')) {
            warningModal.classList.remove('active');
            document.getElementById('modal-overlay').classList.remove('active');
        }
        
        // 비활성 타이머 재시작
        inactivityTimer = setTimeout(handleInactiveUser, INACTIVE_TIMEOUT);
        
        // 경고 타이머 재시작
        warningTimer = setTimeout(showTimeoutWarning, INACTIVE_TIMEOUT - WARNING_BEFORE_TIMEOUT);
    }
    
    /**
     * 타이머 재설정
     */
    function resetTimers() {
        if (inactivityTimer) clearTimeout(inactivityTimer);
        if (warningTimer) clearTimeout(warningTimer);
    }
    
    /**
     * 세션 만료 경고 표시
     */
    function showTimeoutWarning() {
        console.log('Session timeout warning');
        
        // 이미 경고창 요소가 있는지 확인
        let warningModal = document.getElementById('session-timeout-warning');
        let modalOverlay = document.getElementById('modal-overlay');
        
        // 없으면 생성
        if (!warningModal) {
            warningModal = document.createElement('div');
            warningModal.id = 'session-timeout-warning';
            warningModal.className = 'modal';
            warningModal.innerHTML = `
                <div class="modal-content">
                    <div class="modal-header">
                        <h2>세션 만료 경고</h2>
                    </div>
                    <div class="modal-body">
                        <p>장시간 활동이 없어 1분 후 자동으로 로그아웃됩니다.</p>
                        <p>계속 이용하시려면 '세션 유지' 버튼을 클릭하세요.</p>
                        <div class="form-actions">
                            <button id="extend-session-btn" class="btn btn-primary">세션 유지</button>
                            <button id="logout-now-btn" class="btn btn-secondary">로그아웃</button>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(warningModal);
            
            // 세션 유지 버튼 이벤트
            document.getElementById('extend-session-btn').addEventListener('click', function() {
                resetUserActivity();
            });
            
            // 로그아웃 버튼 이벤트
            document.getElementById('logout-now-btn').addEventListener('click', function() {
                logoutUser();
            });
        }
        
        // 모달 오버레이가 없으면 생성
        if (!modalOverlay) {
            modalOverlay = document.createElement('div');
            modalOverlay.id = 'modal-overlay';
            modalOverlay.className = 'modal-overlay';
            document.body.appendChild(modalOverlay);
        }
        
        // 경고창 표시
        warningModal.classList.add('active');
        modalOverlay.classList.add('active');
    }
    
    /**
     * 비활성 사용자 처리
     */
    function handleInactiveUser() {
        console.log('User inactive, logging out');
        logoutUser();
    }
    
    /**
     * 사용자 로그아웃
     */
    function logoutUser() {
        // 세션 타이머 중지
        resetTimers();
        
        // 경고창이 있으면 닫기
        const warningModal = document.getElementById('session-timeout-warning');
        if (warningModal) {
            warningModal.classList.remove('active');
        }
        
        const modalOverlay = document.getElementById('modal-overlay');
        if (modalOverlay) {
            modalOverlay.classList.remove('active');
        }
        
        // Firebase 로그아웃 (필요한 경우)
        if (typeof firebase !== 'undefined' && firebase.auth) {
            firebase.auth().signOut().then(() => {
                // 서버 로그아웃 페이지로 리디렉션
                window.location.href = '/logout';
            }).catch((error) => {
                console.error('Logout error:', error);
                // 오류가 발생해도 서버 로그아웃으로 리디렉션
                window.location.href = '/logout';
            });
        } else {
            // Firebase를 사용할 수 없으면 서버 로그아웃으로 직접 리디렉션
            window.location.href = '/logout';
        }
    }
    
    // 페이지 언로드 시 (페이지 이동, 탭 닫기 등)
    window.addEventListener('beforeunload', function() {
        resetTimers();
    });
});