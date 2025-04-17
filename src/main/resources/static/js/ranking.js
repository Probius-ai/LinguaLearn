document.addEventListener('DOMContentLoaded', function() {
    // Confetti animation for top 3 users
    if (document.querySelector('.rank-1') || document.querySelector('.rank-2') || document.querySelector('.rank-3')) {
        createConfetti();
    }

    // Function to create confetti effect (for top rankings)
    function createConfetti() {
        const confettiContainer = document.createElement('div');
        confettiContainer.className = 'confetti-container';
        confettiContainer.style.position = 'fixed';
        confettiContainer.style.top = '0';
        confettiContainer.style.left = '0';
        confettiContainer.style.width = '100%';
        confettiContainer.style.height = '100%';
        confettiContainer.style.pointerEvents = 'none';
        confettiContainer.style.zIndex = '9999';
        confettiContainer.style.overflow = 'hidden';
        document.body.appendChild(confettiContainer);

        const colors = ['#4361ee', '#3a0ca3', '#f72585', '#4cc9f0', '#4caf50', '#ff9800'];

        // Create 50 confetti pieces
        for (let i = 0; i < 50; i++) {
            const confetti = document.createElement('div');
            confetti.className = 'confetti';

            // Random properties
            const color = colors[Math.floor(Math.random() * colors.length)];
            const size = Math.random() * 10 + 5; // 5px ~ 15px
            const left = Math.random() * 100; // 0% ~ 100%
            const delay = Math.random() * 5; // 0s ~ 5s
            const rotation = Math.random() * 360; // 0deg ~ 360deg

            // Apply styles
            confetti.style.backgroundColor = color;
            confetti.style.width = `${size}px`;
            confetti.style.height = `${size}px`;
            confetti.style.position = 'absolute';
            confetti.style.top = '0';
            confetti.style.left = `${left}%`;
            confetti.style.opacity = '0.7';
            confetti.style.animation = `confetti-fall 5s linear ${delay}s 1`;
            confetti.style.transform = `rotate(${rotation}deg)`;

            confettiContainer.appendChild(confetti);
        }

        // Add animation keyframes
        if (!document.getElementById('confetti-style')) {
            const style = document.createElement('style');
            style.id = 'confetti-style';
            style.textContent = `
                @keyframes confetti-fall {
                    0% {
                        transform: translateY(0) rotate(0deg);
                        opacity: 0.7;
                    }
                    100% {
                        transform: translateY(100vh) rotate(360deg);
                        opacity: 0;
                    }
                }
            `;
            document.head.appendChild(style);
        }

        // Remove after animation completes
        setTimeout(() => {
            confettiContainer.remove();
        }, 10000);
    }

    // Animate rank cards when they come into view
    const rankCards = document.querySelectorAll('.rank-card');

    // Check if IntersectionObserver is supported
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.style.opacity = '1';
                    entry.target.style.transform = 'translateY(0)';
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1 });

        rankCards.forEach(card => {
            observer.observe(card);
        });
    } else {
        // Fallback for browsers that don't support IntersectionObserver
        rankCards.forEach(card => {
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        });
    }
});