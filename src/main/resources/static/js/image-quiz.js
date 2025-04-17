document.addEventListener('DOMContentLoaded', function() {
    // 상태 변수
    let currentLanguage = "";
    let currentLevel = "";
    let correctAnswer = "";
    let quizCounter = 0;
    let score = 0;
    let isAnswered = false;
    let usedWords = new Set(); // 이미 사용된 단어 추적
    let retryCount = 0;        // 중복 단어 재시도 카운터
    const maxRetries = 5;      // 최대 재시도 횟수

    // 요소 참조
    const languageSelect = document.getElementById('language-select');
    const levelSelect = document.getElementById('level-select');
    const quizSection = document.getElementById('quiz-section');
    const loadingArea = document.getElementById('loading');
    const quizImage = document.getElementById('quiz-image');
    const optionsBox = document.getElementById('options-box');
    const quizCounterElement = document.getElementById('quiz-counter');
    const quizScoreElement = document.getElementById('quiz-score');
    const resultElement = document.getElementById('result');
    const scoreUpdateElement = document.getElementById('score-update');
    const nextButton = document.getElementById('next-btn');


    // 언어 선택 처리
    window.selectLanguage = function(language) {
        currentLanguage = language;
        languageSelect.style.display = 'none';
        levelSelect.style.display = 'block';
    };

    // 뒤로가기
    window.goBack = function() {
        levelSelect.style.display = 'none';
        languageSelect.style.display = 'block';
    };

    // 언어 선택으로 돌아가기
    window.goToLanguageSelect = function() {
        quizSection.style.display = 'none';
        languageSelect.style.display = 'block';
        resetQuiz();
    };

    // 퀴즈 시작
    window.startQuiz = function(level) {
        currentLevel = level;
        quizCounter = 0;
        score = 0;
        usedWords.clear(); // 중복방지 단어목록 초기화

        levelSelect.style.display = 'none';
        loadingArea.style.display = 'flex';

        // 초기화 및 첫 번째 문제 로드
        updateQuizCounter();
        updateScore();
        loadNextQuestion();
    };

    // 퀴즈 카운터 업데이트
    function updateQuizCounter() {
        quizCounterElement.textContent = `문제 ${quizCounter + 1}/10`;
    }

    // 점수 업데이트
    function updateScore() {
        quizScoreElement.textContent = `점수: ${score}`;
    }

    // 다음 문제 로드
    function loadNextQuestion() {
        // 퀴즈 섹션 초기화
        resetQuizState();
        loadingArea.style.display = 'flex';
        quizSection.style.display = 'none';
        retryCount = 0; // 재시도 카운터 리셋

        // API에서 문제 가져오기
        fetchNewQuestion();
    }

    function fetchNewQuestion() {
        fetch(`/api/quiz/image?language=${currentLanguage}&level=${currentLevel}`)
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => { throw err; });
                }
                return response.json();
            })
            .then(data => {
                // 이미 중복된 단어인지 확인
                if (usedWords.has(data.answer) && retryCount < maxRetries) {
                    // 중복된 단어면 다시 시도
                    retryCount++;
                    fetchNewQuestion();
                    return;
                }

                // 로딩 숨기기, 퀴즈 섹션 표시
                loadingArea.style.display = 'none';
                quizSection.style.display = 'block';

                // 단어를 중복 목록에 추가
                usedWords.add(data.answer);

                // 문제 데이터 설정
                displayQuestion(data);
            })
            .catch(error => {
                loadingArea.style.display = 'none';

                // 오류 메시지 표시
                if (error.error) {
                    resultElement.textContent = `오류: ${error.error}`;
                    resultElement.style.color = 'var(--error)';
                } else {
                    resultElement.textContent = '문제를 불러오는 중 오류가 발생했습니다.';
                    resultElement.style.color = 'var(--error)';
                }

                // 2초 후 언어 선택 화면으로 돌아가기
                setTimeout(() => {
                    goToLanguageSelect();
                }, 2000);
            });
    }

    // 문제 표시
    function displayQuestion(data) {
        // 이미지 설정
        quizImage.src = data.imageUrl;
        quizImage.alt = '이 사진의 단어는 무엇일까요?';

        // 정답 저장
        correctAnswer = data.answer;

        // 선택지 설정
        optionsBox.innerHTML = '';
        data.choices.forEach(choice => {
            const button = document.createElement('button');
            button.className = 'option-btn';
            button.textContent = choice;
            button.onclick = () => selectAnswer(choice, button);
            optionsBox.appendChild(button);
        });

        // 결과 영역 초기화
        resultElement.textContent = '';
        resultElement.style.color = '';
        scoreUpdateElement.style.display = 'none';
        nextButton.style.display = 'none';


        // 응답 상태 초기화
        isAnswered = false;
    }

    // 답변 선택 처리
    function selectAnswer(selected, button) {
        // 이미 대답했으면 무시
        if (isAnswered) return;
        isAnswered = true;

        // 정답 체크
        if (selected === correctAnswer) {
            // 정답일 경우
            button.classList.add('correct');
            resultElement.textContent = '정답입니다!';
            resultElement.style.color = 'var(--success)';

            // 점수 증가
            score++;
            updateScore();
            scoreUpdateElement.style.display = 'block';

            // 서버에 점수 업데이트 요청
            updateScoreOnServer();
        } else {
            // 오답일 경우
            button.classList.add('incorrect');

            // 정답 버튼 찾아서 표시
            const buttons = optionsBox.querySelectorAll('.option-btn');
            buttons.forEach(btn => {
                if (btn.textContent === correctAnswer) {
                    btn.classList.add('correct');
                }
            });

            resultElement.textContent = `틀렸습니다. 정답은 "${correctAnswer}"입니다.`;
            resultElement.style.color = 'var(--error)';
        }

        // 다음 버튼 표시
        nextButton.style.display = 'block';
        nextButton.onclick = handleNextQuestion;
    }

    // 다음 문제 처리
    function handleNextQuestion() {
        quizCounter++;

        if (quizCounter >= 10) {
            showFinalResult();
        } else {
            updateQuizCounter();
            loadNextQuestion();
        }
    }

    // 최종 결과 표시
    function showFinalResult() {
        // 모든 요소 숨기기
        optionsBox.innerHTML = '';

        // 이미지 요소 완전히 숨기기 (display: none 속성 추가)
        quizImage.style.display = 'none'; // 이 부분이 핵심입니다
        quizImage.src = '';
        quizImage.alt = '';

        nextButton.style.display = 'none';
        scoreUpdateElement.style.display = 'none';

        // 결과 출력
        resultElement.innerHTML = `
        <h2>퀴즈 완료!</h2>
        <p>당신의 최종 점수는 <strong>${score}/10</strong>입니다.</p>
        <p>${getFeedbackMessage()}</p>
    `;
        resultElement.style.color = 'var(--text-primary)';
        resultElement.style.display = 'block';

        // 화면 유지
        quizSection.style.display = 'block';
        loadingArea.style.display = 'none';

        // 자동 리셋 (필요에 따라 시간 조정 가능)
        setTimeout(() => {
            goToLanguageSelect();
        }, 6000);
    }

    // 점수에 따른 피드백 메시지
    function getFeedbackMessage() {
        if (score >= 9) {
            return '훌륭합니다! 당신은 진정한 언어 마스터입니다!';
        } else if (score >= 7) {
            return '잘했습니다! 조금만 더 연습하면 완벽해질 거예요!';
        } else if (score >= 5) {
            return '나쁘지 않습니다. 계속 연습해보세요!';
        } else {
            return '더 많은 연습이 필요합니다. 다시 도전해보세요!';
        }
    }

    // 서버에 점수 업데이트
    function updateScoreOnServer() {
        fetch('/api/quiz/score', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                language: currentLanguage,
                level: currentLevel
            })
        })
            .then(response => response.json())
            .then(data => {
                if (!data.success) {
                    console.error('점수 업데이트 실패:', data.message);
                }
            })
            .catch(error => {
                console.error('점수 업데이트 중 오류:', error);
            });
    }

    // 퀴즈 상태 초기화
    function resetQuizState() {
        optionsBox.innerHTML = '';
        resultElement.textContent = '';
        scoreUpdateElement.style.display = 'none';
        nextButton.style.display = 'none';
    }

    // 퀴즈 전체 초기화
    function resetQuiz() {
        quizCounter = 0;
        score = 0;
        currentLanguage = '';
        currentLevel = '';
        resetQuizState();
    }

    // 다음 버튼에 이벤트 리스너 추가

});