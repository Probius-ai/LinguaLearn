document.addEventListener('DOMContentLoaded', () => {
    // 필요한 요소 참조
    const loginButton = document.getElementById('login-btn');
    const logoutBtn = document.getElementById('logout-btn');
    const loginModal = document.getElementById('login-modal');
    const authContainer = document.getElementById('firebaseui-auth-container');
    const authErrorDiv = document.getElementById('auth-error');

    // 초기화 관련 변수
    let ui = null; // FirebaseUI 인스턴스
    let firebaseApp = null; // Firebase 앱 인스턴스

    // Firebase 설정 가져오기
    async function fetchFirebaseConfig() {
        try {
            const response = await fetch('/api/firebase/config');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const config = await response.json();
            console.log("Firebase config fetched successfully");
            return config;
        } catch (error) {
            console.error("Could not fetch Firebase config:", error);
            if (authErrorDiv) authErrorDiv.textContent = '로그인 설정을 불러올 수 없습니다.';
            if(loginButton) loginButton.disabled = true;
            return null;
        }
    }

    // 백엔드 동기화 함수
    async function syncBackendProfile(idToken) {
        console.log("백엔드 동기화 중...");
        try {
            const res = await fetch('/api/secure/users/sync', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + idToken,
                    'Content-Type': 'application/json'
                }
            });

            if (res.ok) {
                const userData = await res.json();
                console.log("백엔드 동기화 성공");
                // 페이지 새로고침하여 세션 변경사항 반영
                window.location.reload();
            } else {
                const errorText = await res.text();
                console.error(`백엔드 동기화 실패: ${res.status}`, errorText);
                if (authErrorDiv) authErrorDiv.textContent = `프로필 동기화 오류: ${res.status}. 다시 시도해주세요.`;
                // 동기화 실패 시 Firebase 로그아웃
                if (firebase && firebase.auth) {
                    firebase.auth().signOut();
                }
            }
        } catch (error) {
            console.error('동기화 API 호출 오류:', error);
            if (authErrorDiv) authErrorDiv.textContent = '프로필 동기화 오류: ' + error.message;
            if (firebase && firebase.auth) {
                firebase.auth().signOut();
            }
        }
    }

    // 앱 초기화
    async function initializeApp() {
        const firebaseConfig = await fetchFirebaseConfig();
        if (!firebaseConfig) {
            console.error("Firebase 초기화 실패: 설정 없음");
            return;
        }

        // Firebase 앱 초기화 (한 번만)
        if (!firebaseApp) {
            try {
                firebaseApp = firebase.initializeApp(firebaseConfig);
                console.log("Firebase 앱 초기화 완료");
            } catch (e) {
                console.error("Firebase 앱 초기화 오류:", e);
                if (authErrorDiv) authErrorDiv.textContent = '로그인 서비스 초기화 오류';
                if(loginButton) loginButton.disabled = true;
                return;
            }
        }

        // FirebaseUI 초기화 (한 번만)
        if (!ui && firebase && firebase.auth) {
            ui = new firebaseui.auth.AuthUI(firebase.auth());
            console.log("FirebaseUI 초기화 완료");
        } else if (!firebase || !firebase.auth) {
            console.error("FirebaseUI 초기화를 위한 Firebase Auth를 사용할 수 없습니다.");
            if (authErrorDiv) authErrorDiv.textContent = '로그인 컴포넌트 초기화 오류';
            if(loginButton) loginButton.disabled = true;
            return;
        }

        // FirebaseUI 설정
        const uiConfig = {
            signInSuccessUrl: null, // 콜백으로 성공 처리
            signInOptions: [
                firebase.auth.GoogleAuthProvider.PROVIDER_ID,
                firebase.auth.EmailAuthProvider.PROVIDER_ID,
                // 필요시 다른 인증 제공자 추가
            ],
            callbacks: {
                signInSuccessWithAuthResult: function(authResult, redirectUrl) {
                    console.log("로그인 성공");

                    // 모달 닫기
                    if (window.closeLoginModal) {
                        window.closeLoginModal();
                    } else {
                        console.warn("closeLoginModal 함수를 찾을 수 없습니다.");
                        if(loginModal) loginModal.classList.remove('active');
                        const overlay = document.getElementById('modal-overlay');
                        if(overlay) overlay.classList.remove('active');
                        document.body.style.overflow = '';
                    }

                    // ID 토큰 가져오기
                    authResult.user.getIdToken().then(async (idToken) => {
                        console.log("ID 토큰 획득 완료");
                        // 백엔드와 프로필 동기화
                        await syncBackendProfile(idToken);
                        // 성공 시 syncBackendProfile 내에서 페이지 새로고침 처리
                    }).catch(error => {
                        console.error("ID 토큰 가져오기 오류:", error);
                        if (authErrorDiv) authErrorDiv.textContent = '로그인 완료 오류: ' + error.message;
                        if (firebase && firebase.auth) {
                            firebase.auth().signOut();
                        }
                    });
                    return false; // 리디렉션 방지
                },
                signInFailure: function(error) {
                    console.error('FirebaseUI 로그인 오류:', error);
                    if (authErrorDiv) {
                        if (error.code === 'firebaseui/anonymous-upgrade-merge-conflict') {
                            authErrorDiv.textContent = '오류: 다른 로그인 방법으로 이미 계정이 존재합니다.';
                        } else if (error.code === 'firebaseui/auth-ui-cancelled') {
                            console.log('사용자가 로그인을 취소했습니다.');
                            authErrorDiv.textContent = '';
                        } else {
                            authErrorDiv.textContent = `로그인 실패: ${error.message} (코드: ${error.code})`;
                        }
                    }
                },
                uiShown: function() {
                    console.log("FirebaseUI 위젯 표시됨");
                    if (authErrorDiv) authErrorDiv.textContent = '';
                }
            }
        };

        // 인증 상태 리스너
        if (firebase && firebase.auth) {
            firebase.auth().onAuthStateChanged((user) => {
                if (user) {
                    console.log("인증 상태 변경: 로그인됨", user.uid);
                    if (window.closeLoginModal) window.closeLoginModal();
                    
                    // Get user information for UI update
                    const userData = {
                        name: user.displayName || '사용자',
                        email: user.email,
                        photoURL: user.photoURL,
                        uid: user.uid
                    };
                    
                    // Update UI without page refresh
                    updateAuthUI(true, userData);
                } else {
                    console.log("인증 상태 변경: 로그아웃됨");
                    // Update UI to show login button
                    updateAuthUI(false);
                    // 서버 세션 상태 확인 (백엔드 세션은 별도로 존재할 수 있음)
                    checkServerSession();
                }
            });
            
            // 토큰 갱신 리스너
            firebase.auth().onIdTokenChanged(async (user) => {
                if (user) {
                    // 토큰 갱신 시 백엔드에 알림
                    const token = await user.getIdToken();
                    // 필요시 백엔드에 토큰 갱신 요청
                    console.log("ID 토큰이 갱신되었습니다");
                }
            });
        } else {
            console.error("인증 상태 리스너 설정을 위한 Firebase Auth를 사용할 수 없습니다.");
        }

        // 서버 세션 상태 확인 함수
        async function checkServerSession() {
            try {
                const response = await fetch('/api/auth/status');
                if (response.ok) {
                    // 서버 세션이 있지만 Firebase 상태는 로그아웃 - 페이지 새로고침
                    console.log("서버 세션은 있으나 Firebase 상태가 로그아웃됨 - 새로고침");
                    window.location.reload();
                }
            } catch (error) {
                console.error("서버 세션 확인 오류:", error);
            }
        }

        // 로그인 버튼 이벤트 리스너
        if (loginButton) {
            loginButton.addEventListener('click', () => {
                if (firebaseApp && ui && authContainer) {
                    console.log("로그인 버튼 클릭, FirebaseUI 시작");
                    authContainer.style.display = 'block';
                    ui.start('#firebaseui-auth-container', uiConfig);
                } else {
                    console.error("Firebase가 준비되지 않았거나 인증 컨테이너를 찾을 수 없어 UI를 시작할 수 없습니다.");
                    if (authErrorDiv) authErrorDiv.textContent = '로그인 서비스가 준비되지 않았습니다. 나중에 다시 시도해주세요.';
                    if (!firebaseApp || !ui) {
                        initializeApp();
                    }
                }
            });
        }

        // 로그아웃 기능 추가
        if (logoutBtn) {
            logoutBtn.addEventListener('click', handleLogout);
        }
    }

    // 로그아웃 처리 함수
    function handleLogout() {
        const confirmLogout = confirm("로그아웃 하시겠습니까?");
        if (confirmLogout) {
            console.log("로그아웃 중...");
            if (firebase && firebase.auth) {
                firebase.auth().signOut().then(() => {
                    console.log('Firebase에서 로그아웃 성공');
                    window.location.href = '/logout'; // 서버 세션도 로그아웃
                }).catch((error) => {
                    console.error('로그아웃 오류', error);
                    alert('로그아웃 중 오류가 발생했습니다.');
                    // 오류가 발생해도 서버 세션은 로그아웃 시도
                    window.location.href = '/logout';
                });
            } else {
                console.error("로그아웃을 위한 Firebase auth를 사용할 수 없습니다.");
                window.location.href = '/logout'; // Firebase가 없어도 서버 세션은 로그아웃
            }
        }
    }

    /**
     * Updates the authentication UI based on user login state
     * @param {boolean} isLoggedIn - Whether user is logged in
     * @param {Object} userData - User data object with name, email, etc.
     */
    function updateAuthUI(isLoggedIn, userData = null) {
        const loginBtn = document.getElementById('login-btn');
        const userLoggedIn = document.querySelector('.user-logged-in');
        
        if (!loginBtn || !userLoggedIn) {
            console.warn('Auth UI elements not found in DOM');
            return;
        }
        
        if (isLoggedIn && userData) {
            // Hide login button, show user profile section
            loginBtn.style.display = 'none';
            userLoggedIn.style.display = 'flex';
            
            // Update avatar with first letter of user's name
            const avatar = userLoggedIn.querySelector('.avatar');
            if (avatar && userData.name) {
                avatar.textContent = userData.name.substring(0, 1).toUpperCase();
            }
            
            // Update profile link if needed
            const profileLink = userLoggedIn.querySelector('.profile-link');
            if (profileLink) {
                profileLink.title = `${userData.name}님의 프로필`;
            }
        } else {
            // Show login button, hide user profile section
            loginBtn.style.display = 'block';
            userLoggedIn.style.display = 'none';
        }
    }

    // 초기화 시작 (로그인 버튼이나 로그아웃 버튼이 있을 때만)
    if (loginButton || logoutBtn) {
        initializeApp();
    } else {
        console.log("로그인 버튼이나 로그아웃 버튼을 찾을 수 없습니다. Firebase 인증 초기화 건너뜀.");
    }
});