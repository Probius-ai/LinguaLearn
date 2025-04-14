document.addEventListener('DOMContentLoaded', () => {
    const loginButton = document.getElementById('login-btn');
    const profileLink = document.querySelector('.profile-link'); // Assuming this exists when logged in
    const loginModal = document.getElementById('login-modal'); // Get modal reference
    // Ensure these elements are selected *after* the modal exists in the DOM
    const authContainer = document.getElementById('firebaseui-auth-container');
    const authErrorDiv = document.getElementById('auth-error');

    // Check if essential elements exist (especially for logged-in users where modal might not be needed initially)
    if (!loginModal || !authContainer || !authErrorDiv) {
        // console.warn("Login modal, auth container, or error div not found. FirebaseUI might not work correctly if login button is clicked.");
        // If login button exists, it means we are logged out, so these elements *should* exist.
        if (loginButton) {
            console.error("Required elements for FirebaseUI (modal, container, error div) not found!");
        }
        // If profileLink exists, we are logged in, proceed with logout setup only.
    }


    let ui = null; // FirebaseUI instance
    let firebaseApp = null; // Firebase App instance

    // Function to fetch Firebase config from the backend
    async function fetchFirebaseConfig() {
        try {
            const response = await fetch('/api/firebase/config'); // Adjust endpoint if needed
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const config = await response.json();
            console.log("Firebase config fetched successfully:", config);
            return config;
        } catch (error) {
            console.error("Could not fetch Firebase config:", error);
            if (authErrorDiv) authErrorDiv.textContent = 'Error: Could not load login configuration.';
            // Optionally disable login button or show a persistent error
            if(loginButton) loginButton.disabled = true;
            return null;
        }
    }

    // Function to call the backend sync endpoint
    async function syncBackendProfile(idToken) {
        console.log("Calling backend sync endpoint...");
        try {
            const res = await fetch('/api/secure/users/sync', { // Adjust endpoint if needed
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + idToken,
                    'Content-Type': 'application/json'
                }
            });

            if (res.ok) {
                const userData = await res.json();
                console.log("Backend sync successful:", userData);
                // Reload page to reflect server-side session changes (Thymeleaf)
                window.location.reload();
            } else {
                const errorText = await res.text();
                console.error(`Backend sync failed: ${res.status} ${res.statusText}`, errorText);
                if (authErrorDiv) authErrorDiv.textContent = `Error syncing profile: ${res.status}. Please try again.`;
                // Handle sync failure - log out the user from Firebase
                if (firebase && firebase.auth) {
                    firebase.auth().signOut();
                }
                // Keep the modal open to show the error
            }
        } catch (error) {
            console.error('Error calling sync API:', error);
            if (authErrorDiv) authErrorDiv.textContent = 'Error syncing profile: ' + error.message;
             // Handle sync failure - log out the user from Firebase
            if (firebase && firebase.auth) {
                firebase.auth().signOut();
            }
             // Keep the modal open to show the error
        }
    }


    // Initialize Firebase and FirebaseUI
    async function initializeApp() {
        const firebaseConfig = await fetchFirebaseConfig();

        if (!firebaseConfig) {
            console.error("Firebase initialization failed: No config.");
            return; // Stop initialization if config fetch failed
        }

        // Initialize Firebase only once
        if (!firebaseApp) {
             try {
                // Use compat version initialization
                firebaseApp = firebase.initializeApp(firebaseConfig);
                console.log("Firebase App Initialized.");
             } catch (e) {
                 console.error("Error initializing Firebase App:", e);
                 if (authErrorDiv) authErrorDiv.textContent = 'Error initializing login service.';
                 if(loginButton) loginButton.disabled = true;
                 return;
             }
        }


        // Initialize FirebaseUI only once
        if (!ui && firebase && firebase.auth) {
            // Use compat version
            ui = new firebaseui.auth.AuthUI(firebase.auth());
            console.log("FirebaseUI Initialized.");
        } else if (!firebase || !firebase.auth) {
             console.error("Firebase Auth is not available for FirebaseUI initialization.");
             if (authErrorDiv) authErrorDiv.textContent = 'Error initializing login components.';
             if(loginButton) loginButton.disabled = true;
             return;
        }

        // FirebaseUI config
        const uiConfig = {
            signInSuccessUrl: null, // We handle success manually with callbacks
            signInOptions: [
                // Configure providers as needed (use compat version)
                firebase.auth.GoogleAuthProvider.PROVIDER_ID,
                firebase.auth.EmailAuthProvider.PROVIDER_ID,
                // firebase.auth.FacebookAuthProvider.PROVIDER_ID, // Add if needed
            ],
            callbacks: {
                signInSuccessWithAuthResult: function(authResult, redirectUrl) {
                    // User successfully signed in.
                    console.log("Sign-in successful:", authResult);

                    // Close the modal first
                    if (window.closeLoginModal) {
                        window.closeLoginModal();
                    } else {
                        console.warn("closeLoginModal function not found.");
                        // Fallback: Hide modal manually if function is not available
                        if(loginModal) loginModal.classList.remove('active');
                        const overlay = document.getElementById('modal-overlay');
                        if(overlay) overlay.classList.remove('active');
                        document.body.style.overflow = '';
                    }


                    // Get the ID token
                    authResult.user.getIdToken().then(async (idToken) => {
                        console.log("ID Token obtained:", idToken ? 'Yes' : 'No'); // Check if token is obtained
                        // Sync profile with backend
                        await syncBackendProfile(idToken);
                        // Reload is now handled within syncBackendProfile on success
                    }).catch(error => {
                         console.error("Error getting ID token:", error);
                         if (authErrorDiv) authErrorDiv.textContent = 'Error completing login: ' + error.message;
                         if (firebase && firebase.auth) {
                            firebase.auth().signOut(); // Sign out if token fails
                         }
                         // Re-open modal to show error? Or rely on authErrorDiv being visible.
                         // For now, just log out. The modal was already closed.
                    });
                    // Return false to prevent redirect.
                    return false;
                },
                signInFailure: function(error) {
                    // Handle sign-in errors (e.g., account exists with different credential)
                    console.error('FirebaseUI sign-in error:', error);
                    if (authErrorDiv) {
                        if (error.code === 'firebaseui/anonymous-upgrade-merge-conflict') {
                             authErrorDiv.textContent = 'Error: Account already exists with different login method.';
                        } else if (error.code === 'firebaseui/auth-ui-cancelled') {
                             console.log('User cancelled login.');
                             authErrorDiv.textContent = ''; // Clear error if user just closed UI
                        }
                        else {
                             authErrorDiv.textContent = `Login failed: ${error.message} (Code: ${error.code})`;
                        }
                    }
                    // Keep the modal open to show the error
                },
                uiShown: function() {
                    // Optional: hide a loader if you have one
                    console.log("FirebaseUI widget shown.");
                    if (authErrorDiv) authErrorDiv.textContent = ''; // Clear previous errors
                }
            },
            // Other UI config options (e.g., tosUrl, privacyPolicyUrl)
            // tosUrl: '/terms-of-service',
            // privacyPolicyUrl: '/privacy-policy'
        };

        // --- Auth State Listener ---
        if (firebase && firebase.auth) {
            firebase.auth().onAuthStateChanged((user) => {
                if (user) {
                    // User is signed in. Handled by signInSuccessWithAuthResult callback
                    // and subsequent page reload after backend sync.
                    console.log("Auth state changed: User is signed in.", user.uid);
                    // UI updates (like hiding login button) should ideally be handled
                    // by the server-side rendering (Thymeleaf) after reload.
                    // Ensure modal is closed if somehow left open
                     if (window.closeLoginModal) window.closeLoginModal();

                } else {
                    // User is signed out.
                    console.log("Auth state changed: User is signed out.");
                    // Ensure login button is visible and profile link is hidden
                    // This might conflict with Thymeleaf's initial rendering,
                    // but can be useful for client-side updates if needed.
                    // if (loginButton) loginButton.style.display = 'block'; // Let Thymeleaf handle this
                    // if (profileLink) profileLink.style.display = 'none'; // Let Thymeleaf handle this
                }
            });
        } else {
             console.error("Firebase Auth not available for setting up Auth State Listener.");
        }


        // --- Event Listeners ---

        // Listener for the main login button to open the modal AND start FirebaseUI
        if (loginButton) {
            loginButton.addEventListener('click', () => {
                // Ensure Firebase is initialized before starting UI
                // Also check if the necessary DOM elements for UI are present
                if (firebaseApp && ui && authContainer) {
                     console.log("Login button clicked, starting FirebaseUI.");
                     // Start the FirebaseUI Widget.
                     // The login-modal.js handles showing the modal itself.
                     // Ensure the container is visible *inside* the modal before starting.
                     authContainer.style.display = 'block'; // Make sure container is visible
                     ui.start('#firebaseui-auth-container', uiConfig);
                } else {
                    console.error("Firebase not ready or auth container not found, cannot start UI.");
                    if (authErrorDiv) authErrorDiv.textContent = 'Login service is not ready. Please try again later.';
                    // Attempt re-initialization or show error
                    if (!firebaseApp || !ui) {
                        initializeApp(); // Try to initialize again if Firebase/UI failed
                    }
                }
            });
        } else {
             // console.warn("Login button (#login-btn) not found. Assuming user is logged in.");
             // This is expected if the user is logged in.
        }

        // Add logout functionality (e.g., clicking the profile link/avatar)
        if (profileLink) {
            profileLink.addEventListener('click', (event) => {
                event.preventDefault(); // Prevent navigation if it's just for logout trigger

                // Check if it's part of a dropdown, if so, let dropdown handle it.
                // This assumes the profile link itself isn't the only logout trigger.
                // If the dropdown menu link '/logout' is clicked, this listener might not be needed,
                // OR it should only handle clicks on the avatar itself if that's the intended trigger.
                // For simplicity, let's assume clicking the avatar/link *can* trigger logout confirmation.

                // Find if the click is within a dropdown menu link
                 const dropdownLink = event.target.closest('.dropdown-menu a[href="/logout"]');
                 if (dropdownLink) {
                     // Let the default link behavior handle the /logout redirect
                     return;
                 }

                // If the click was on the avatar or profile link itself (not the dropdown logout)
                const confirmLogout = confirm("로그아웃 하시겠습니까?"); // Ask for confirmation
                if (confirmLogout) {
                    console.log("Logging out via profile link click...");
                    if (firebase && firebase.auth) {
                        firebase.auth().signOut().then(() => {
                            console.log('Signed out successfully from Firebase.');
                            // Redirect to a logout endpoint on your server
                            // This endpoint should clear the server-side session/cookie
                            window.location.href = '/logout'; // Adjust endpoint if needed
                        }).catch((error) => {
                            console.error('Sign out error', error);
                            alert('로그아웃 중 오류가 발생했습니다.');
                        });
                    } else {
                         console.error("Firebase auth not available for sign out.");
                         // Fallback: Just redirect to server logout
                         window.location.href = '/logout';
                    }
                }
            });
        } else {
            // console.warn("Profile link (.profile-link) not found. Logout trigger might be missing.");
            // If using Thymeleaf, this element might not exist when logged out, which is expected.
        }

    } // End of initializeApp

    // Start the initialization process only if not logged in (login button exists)
    // or if logged in (profile link exists - for logout functionality)
    if (loginButton || profileLink) {
        initializeApp();
    } else {
        console.log("Neither login button nor profile link found. Skipping Firebase auth initialization.");
    }


}); // End of DOMContentLoaded