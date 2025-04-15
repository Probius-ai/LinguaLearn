document.addEventListener('DOMContentLoaded', function() {
    // 상태 변수
    let currentPage = 1;
    let itemsPerPage = 10;
    let totalItems = 0;
    let totalPages = 0;
    let currentWords = [];
    let wordToDelete = null;

    // DOM 요소
    const wordsTableBody = document.getElementById('wordsTableBody');
    const pagination = document.getElementById('pagination');
    const filterLanguage = document.getElementById('filterLanguage');
    const filterLevel = document.getElementById('filterLevel');
    const searchWord = document.getElementById('searchWord');
    const searchBtn = document.getElementById('searchBtn');
    const resetFilterBtn = document.getElementById('resetFilterBtn');
    const addWordBtn = document.getElementById('addWordBtn');
    const wordModal = document.getElementById('wordModal');
    const modalOverlay = document.getElementById('modal-overlay');
    const closeModalBtns = document.querySelectorAll('.close-modal');
    const modalTitle = document.getElementById('modalTitle');
    const wordForm = document.getElementById('wordForm');
    const wordId = document.getElementById('wordId');
    const wordText = document.getElementById('wordText');
    const wordTranslation = document.getElementById('wordTranslation');
    const wordLanguage = document.getElementById('wordLanguage');
    const wordLevel = document.getElementById('wordLevel');
    const wordPronunciation = document.getElementById('wordPronunciation');
    const wordExample = document.getElementById('wordExample');
    const cancelWordBtn = document.getElementById('cancelWordBtn');
    const saveWordBtn = document.getElementById('saveWordBtn');
    const deleteConfirmModal = document.getElementById('deleteConfirmModal');
    const cancelDeleteBtn = document.getElementById('cancelDeleteBtn');
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const alertBox = document.getElementById('alertBox');

    // 초기 로드
    loadWords();

    // 이벤트 리스너 등록
    searchBtn.addEventListener('click', function() {
        currentPage = 1;
        loadWords();
    });

    resetFilterBtn.addEventListener('click', function() {
        filterLanguage.value = '';
        filterLevel.value = '';
        searchWord.value = '';
        currentPage = 1;
        loadWords();
    });

    addWordBtn.addEventListener('click', function() {
        openAddWordModal();
    });

    closeModalBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            closeModals();
        });
    });

    modalOverlay.addEventListener('click', function() {
        closeModals();
    });

    cancelWordBtn.addEventListener('click', function() {
        closeModals();
    });

    wordForm.addEventListener('submit', function(e) {
        e.preventDefault();
        saveWord();
    });

    cancelDeleteBtn.addEventListener('click', function() {
        closeModals();
    });

    confirmDeleteBtn.addEventListener('click', function() {
        deleteWord();
    });

    // 단어 목록 로드
    function loadWords() {
        showLoading();
        
        // 필터 파라미터 구성
        const params = new URLSearchParams();
        if (filterLanguage.value) params.append('language', filterLanguage.value);
        if (filterLevel.value) params.append('level', filterLevel.value);
        
        // API 호출
        fetch(`/api/admin/words?${params.toString()}`)
            .then(response => {
                if (!response.ok) {
                    return response.json().then(err => { throw err; });
                }
                return response.json();
            })
            .then(data => {
                hideLoading();
                
                // 검색어 필터링 (클라이언트 측에서)
                if (searchWord.value.trim()) {
                    const searchTerm = searchWord.value.trim().toLowerCase();
                    currentWords = data.filter(word => 
                        word.text?.toLowerCase().includes(searchTerm) || 
                        word.translation?.toLowerCase().includes(searchTerm)
                    );
                } else {
                    currentWords = data;
                }
                
                totalItems = currentWords.length;
                totalPages = Math.ceil(totalItems / itemsPerPage);
                
                renderTable();
                renderPagination();
            })
            .catch(error => {
                hideLoading();
                console.error('단어 목록 로드 중 오류 발생:', error);
                showAlert('단어 목록을 불러오는 데 실패했습니다: ' + (error.error || error.message || '알 수 없는 오류'), 'error');
            });
    }

    // 테이블 렌더링
    function renderTable() {
        wordsTableBody.innerHTML = '';
        
        if (currentWords.length === 0) {
            wordsTableBody.innerHTML = `
                <tr>
                    <td colspan="5" style="text-align: center;">단어가 없습니다.</td>
                </tr>
            `;
            return;
        }
        
        // 현재 페이지 데이터 계산
        const startIndex = (currentPage - 1) * itemsPerPage;
        const endIndex = Math.min(startIndex + itemsPerPage, currentWords.length);
        const pageWords = currentWords.slice(startIndex, endIndex);
        
        // 행 생성
        pageWords.forEach(word => {
            const row = document.createElement('tr');
            
            // 언어 및 레벨 태그 클래스 설정
            const languageClass = 'language-tag';
            const levelClass = `level-tag ${word.level || 'beginner'}`;
            
            // 행 내용 설정
            row.innerHTML = `
                <td>${word.text || ''}</td>
                <td>${word.translation || ''}</td>
                <td><span class="${languageClass}">${getLanguageDisplay(word.language)}</span></td>
                <td><span class="${levelClass}">${getLevelDisplay(word.level)}</span></td>
                <td>
                    <div class="action-buttons">
                        <button class="btn-icon btn-edit" data-id="${word.id}" title="수정">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn-icon btn-delete" data-id="${word.id}" data-word="${word.text}" title="삭제">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </td>
            `;
            
            wordsTableBody.appendChild(row);
        });
        
        // 편집 버튼 이벤트 리스너
        document.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', function() {
                const id = this.getAttribute('data-id');
                openEditWordModal(id);
            });
        });
        
        // 삭제 버튼 이벤트 리스너
        document.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', function() {
                const id = this.getAttribute('data-id');
                const word = this.getAttribute('data-word');
                openDeleteConfirmModal(id, word);
            });
        });
    }

    // 페이지네이션 렌더링
    function renderPagination() {
        pagination.innerHTML = '';
        
        if (totalPages <= 1) {
            return;
        }
        
        // 이전 페이지 버튼
        if (currentPage > 1) {
            const prevBtn = createPageItem('<i class="fas fa-chevron-left"></i>', () => {
                currentPage--;
                renderTable();
                renderPagination();
            });
            pagination.appendChild(prevBtn);
        }
        
        // 페이지 번호 버튼들
        let startPage = Math.max(1, currentPage - 2);
        let endPage = Math.min(totalPages, startPage + 4);
        
        if (endPage - startPage < 4) {
            startPage = Math.max(1, endPage - 4);
        }
        
        for (let i = startPage; i <= endPage; i++) {
            const pageBtn = createPageItem(i, () => {
                currentPage = i;
                renderTable();
                renderPagination();
            }, i === currentPage);
            pagination.appendChild(pageBtn);
        }
        
        // 다음 페이지 버튼
        if (currentPage < totalPages) {
            const nextBtn = createPageItem('<i class="fas fa-chevron-right"></i>', () => {
                currentPage++;
                renderTable();
                renderPagination();
            });
            pagination.appendChild(nextBtn);
        }
    }

    // 페이지 항목 생성 함수
    function createPageItem(content, clickHandler, isActive = false) {
        const pageItem = document.createElement('div');
        pageItem.className = 'page-item' + (isActive ? ' active' : '');
        pageItem.innerHTML = content;
        pageItem.addEventListener('click', clickHandler);
        return pageItem;
    }

    // 단어 추가 모달 열기
    function openAddWordModal() {
        modalTitle.textContent = '단어 추가';
        wordId.value = '';
        wordText.value = '';
        wordTranslation.value = '';
        wordLanguage.value = 'english';
        wordLevel.value = 'beginner';
        wordPronunciation.value = '';
        wordExample.value = '';
        
        openModal(wordModal);
    }

    // 단어 수정 모달 열기
    function openEditWordModal(id) {
        const word = currentWords.find(w => w.id === id);
        if (!word) {
            showAlert('단어를 찾을 수 없습니다.', 'error');
            return;
        }
        
        modalTitle.textContent = '단어 수정';
        wordId.value = word.id;
        wordText.value = word.text || '';
        wordTranslation.value = word.translation || '';
        wordLanguage.value = word.language || 'english';
        wordLevel.value = word.level || 'beginner';
        wordPronunciation.value = word.pronunciation || '';
        wordExample.value = word.exampleSentence || '';
        
        openModal(wordModal);
    }

    // 삭제 확인 모달 열기
    function openDeleteConfirmModal(id, text) {
        wordToDelete = id;
        const modalMessage = document.querySelector('#deleteConfirmModal p');
        modalMessage.textContent = `정말로 "${text}" 단어를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`;
        
        openModal(deleteConfirmModal);
    }

    // 모달 열기 공통 함수
    function openModal(modal) {
        modal.classList.add('active');
        modalOverlay.classList.add('active');
        document.body.style.overflow = 'hidden'; // 배경 스크롤 방지
    }

    // 모달 닫기
    function closeModals() {
        wordModal.classList.remove('active');
        deleteConfirmModal.classList.remove('active');
        modalOverlay.classList.remove('active');
        document.body.style.overflow = '';
    }

    // 단어 저장 (추가 또는 수정)
    function saveWord() {
        // 필수 필드 검증
        if (!wordText.value.trim()) {
            showAlert('단어 텍스트는 필수 항목입니다.', 'error');
            return;
        }
        
        if (!wordTranslation.value.trim()) {
            showAlert('번역은 필수 항목입니다.', 'error');
            return;
        }
        
        showLoading();
        
        const wordData = {
            text: wordText.value.trim(),
            translation: wordTranslation.value.trim(),
            language: wordLanguage.value,
            level: wordLevel.value,
            pronunciation: wordPronunciation.value.trim(),
            exampleSentence: wordExample.value.trim()
        };
        
        let url = '/api/admin/words';
        let method = 'POST';
        
        // 수정 모드인 경우
        if (wordId.value) {
            url = `/api/admin/words/${wordId.value}`;
            method = 'PUT';
        }
        
        fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(wordData)
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => { throw err; });
            }
            return response.json();
        })
        .then(data => {
            hideLoading();
            closeModals();
            
            const action = wordId.value ? '수정' : '추가';
            showAlert(`단어가 성공적으로 ${action}되었습니다.`, 'success');
            
            // 목록 다시 로드
            loadWords();
        })
        .catch(error => {
            hideLoading();
            console.error('단어 저장 중 오류 발생:', error);
            showAlert('단어 저장에 실패했습니다: ' + (error.error || error.message || '알 수 없는 오류'), 'error');
        });
    }

    // 단어 삭제
    function deleteWord() {
        if (!wordToDelete) {
            return;
        }
        
        showLoading();
        
        fetch(`/api/admin/words/${wordToDelete}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => { throw err; });
            }
            return response.json();
        })
        .then(data => {
            hideLoading();
            closeModals();
            
            showAlert('단어가 성공적으로 삭제되었습니다.', 'success');
            
            // 목록 다시 로드
            loadWords();
        })
        .catch(error => {
            hideLoading();
            console.error('단어 삭제 중 오류 발생:', error);
            showAlert('단어 삭제에 실패했습니다: ' + (error.error || error.message || '알 수 없는 오류'), 'error');
        });
    }

    // 알림 표시
    function showAlert(message, type) {
        alertBox.className = `alert alert-${type}`;
        alertBox.textContent = message;
        alertBox.style.display = 'block';
        
        // 3초 후 자동으로 숨김
        setTimeout(() => {
            alertBox.style.display = 'none';
        }, 3000);
    }

    // 로딩 표시/숨김
    function showLoading() {
        loadingOverlay.style.display = 'flex';
    }
    
    function hideLoading() {
        loadingOverlay.style.display = 'none';
    }

    // 언어 표시명 변환
    function getLanguageDisplay(language) {
        switch (language) {
            case 'english': return '영어';
            case 'japanese': return '일본어';
            case 'chinese': return '중국어';
            default: return language || '';
        }
    }
    
    // 레벨 표시명 변환
    function getLevelDisplay(level) {
        switch (level) {
            case 'beginner': return '초급';
            case 'intermediate': return '중급';
            case 'advanced': return '고급';
            default: return level || '';
        }
    }
});