document.addEventListener('DOMContentLoaded', function() {
    // Handle retry button click events
    const retryButtons = document.querySelectorAll('.retry-btn');

    retryButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            // Add loading effect when clicked
            this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 로딩 중...';
            this.disabled = true;

            // Let the default link navigation happen
            // The href attribute will navigate to the retry page
        });
    });

    // Handle speech synthesis for each sentence
    const sentenceElements = document.querySelectorAll('.sentence-text');

    sentenceElements.forEach(sentence => {
        // Add a listen button next to each sentence
        const listenBtn = document.createElement('button');
        listenBtn.className = 'listen-btn';
        listenBtn.innerHTML = '<i class="fas fa-volume-up"></i>';
        listenBtn.title = '문장 듣기';
        listenBtn.style.marginLeft = '0.5rem';
        listenBtn.style.background = 'none';
        listenBtn.style.border = 'none';
        listenBtn.style.color = 'var(--accent)';
        listenBtn.style.cursor = 'pointer';

        sentence.appendChild(listenBtn);

        // Add click event to play the sentence
        listenBtn.addEventListener('click', function(e) {
            e.stopPropagation(); // Prevent triggering parent events

            const text = sentence.textContent.trim();
            const utterance = new SpeechSynthesisUtterance(text);
            utterance.lang = 'en-US'; // Set language to English

            // Add visual feedback when speaking
            listenBtn.innerHTML = '<i class="fas fa-volume-up fa-beat"></i>';

            utterance.onend = function() {
                listenBtn.innerHTML = '<i class="fas fa-volume-up"></i>';
            };

            speechSynthesis.speak(utterance);
        });
    });

    // Track analytics for page visits
    function trackWrongSentencesVisit() {
        if (typeof firebase !== 'undefined' && firebase.analytics) {
            firebase.analytics().logEvent('wrong_sentences_view', {
                timestamp: new Date().toISOString(),
                sentence_count: document.querySelectorAll('.wrong-item').length
            });
        }
    }

    // Try to track the visit, but don't fail if analytics is not available
    try {
        trackWrongSentencesVisit();
    } catch (error) {
        console.log('Analytics tracking not available');
    }
});