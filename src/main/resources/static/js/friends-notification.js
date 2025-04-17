// This script checks for pending friend requests and adds a notification badge
document.addEventListener('DOMContentLoaded', function() {
    // Only run if user is logged in
    const userLoggedInDiv = document.querySelector('.user-logged-in');
    if (!userLoggedInDiv || window.getComputedStyle(userLoggedInDiv).display === 'none') {
        return;
    }

    // Find the friends link in the navigation
    const friendsLink = document.querySelector('a[href="/friends"]');
    if (!friendsLink) {
        return;
    }

    // Check for pending friend requests
    checkFriendRequests();

    // Check every 2 minutes
    setInterval(checkFriendRequests, 120000);

    function checkFriendRequests() {
        fetch('/api/friends/requests')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Request failed');
                }
                return response.json();
            })
            .then(data => {
                if (data.requests && data.requests.length > 0) {
                    // Add or update notification badge
                    let badge = friendsLink.querySelector('.notification-badge');
                    if (!badge) {
                        badge = document.createElement('span');
                        badge.className = 'notification-badge';
                        friendsLink.appendChild(badge);
                    }
                    badge.textContent = data.requests.length;
                    badge.style.display = 'inline-flex';
                } else {
                    // Remove badge if no requests
                    const badge = friendsLink.querySelector('.notification-badge');
                    if (badge) {
                        badge.remove();
                    }
                }
            })
            .catch(error => {
                console.error('Error checking friend requests:', error);
            });
    }
});