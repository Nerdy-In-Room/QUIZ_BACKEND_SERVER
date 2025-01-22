// 전역 변수
let timeLeft = 30;
let remainQuizValue = 0; // 나중에 DOM에서 초기화
let timeIntervalId = null; // 타이머 ID
let stompClient;
let ans;
let des;
let curQuiz;

window.onload = function () {
    initPage();
    connectToQuizUpdates();
};

// 초기화 함수
function initPage() {
    const remainQuizElem = document.getElementById("remainQuiz");
    if (remainQuizElem) {
        remainQuizValue = Number(remainQuizElem.textContent.trim()) || 0;
    }

    // Admin 전용 createQuizBtn 이벤트 등록
    const createQuizBtn = document.getElementById("createQuizBtn");
    if (createQuizBtn) {
        createQuizBtn.addEventListener("click", () => {
            // WebSocket으로 createQuiz 이벤트를 전송
            sendCreateQuizEvent();
        });
    }

    const answerBtn = document.getElementById("answerBtn");
    if(answerBtn) {
        answerBtn.disabled = true;
        answerBtn.addEventListener("click", () => {
            checkQuizEvent()
        })
    }
}

// WebSocket 연결 및 구독
function connectToQuizUpdates() {
    const socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    const roomId = window.location.pathname.split("/")[2];

    stompClient.connect({"heart-beat": "10000,10000"}, function (frame) {
        console.log("Connected to WebSocket:", frame);
        console.log("roomId is {}", roomId);

        // /pub/quiz/{roomId} 경로 구독
        stompClient.subscribe(`/pub/quiz/${roomId}`, function (res) {
            const quizData = JSON.parse(res.body);
            ans = quizData.correctAnswer;
            des = quizData.description;
            // 받은 데이터를 바탕으로 퀴즈 상태 업데이트
            if(quizData.hasOwnProperty("problem")) {
                updateQuizStatus(quizData);
                hideAnswerAndDescription();
            }
            else if(!quizData.finalResult){
                handleWinner(quizData);
            }
            else {
                handleFinalWinners(quizData);
            }
        });
    });
}

// 새 문제 출제 (Admin 전용)
function sendCreateQuizEvent() {
    const roomId = window.location.pathname.split("/")[2];
    stompClient.send(`/room/${roomId}/send`, {}, JSON.stringify({}));

    // Admin에서 버튼을 비활성화
    const createQuizBtn = document.getElementById("createQuizBtn");
    const answerBtn = document.getElementById("answerBtn");
    if (createQuizBtn) {
        createQuizBtn.disabled = true;
    }
    if(answerBtn) {
        answerBtn.disabled = false;
    }
}

// 정답 제출 이벤트
function checkQuizEvent() {
    const userId = document.getElementById("userId").textContent;
    const roomId = window.location.pathname.split("/")[2];
    const userAnswer = document.getElementById("answerInput").value.trim();
    stompClient.send(`/room/${roomId}/check`, {}, JSON.stringify({
        userId : userId,
        answer : userAnswer
    }));
    // 답안 제출 후 입력창 비우기
    document.getElementById("answerInput").value = "";
}

// 정답자가 나왔을 때 처리 (winnerEmail, timeLeft=0, Toast 표시, CreateQuiz 버튼 활성화)
function handleWinner(quizData) {
    const winnerSpan = document.getElementById("winner");

    if(!quizData.currentResult) {
        showToast("틀렸습니다.")
        return;
    }

    winnerSpan.textContent = quizData.email;
    // 시간을 0초로 즉시 만듬
    if(timeIntervalId) {
        clearInterval(timeIntervalId);
    }
    timeLeft = 0;
    const timeLeftElem = document.getElementById("timeLeft");
    if(timeLeftElem) timeLeftElem.textContent = 0;

    document.getElementById("correctAnswer").style.display = "block";
    document.getElementById("correctAnswerText").textContent = quizData.correctAnswer;
    document.getElementById("description").style.display = "block";
    document.getElementById("descriptionText").textContent = quizData.description;

    // Toast로 승리자 알림
    showToast(`이번 문제의 승리자는 ${quizData.email} 님입니다!`, 3000);
    const createQuizBtn = document.getElementById("createQuizBtn");
    if (createQuizBtn) {
        createQuizBtn.disabled = false;
    }
    const answerBtn = document.getElementById("answerBtn");
    if(answerBtn) answerBtn.disabled = true;
    // 정답자 최신화
    curQuiz = quizData;
}
// 최종 우승자 표시
function handleFinalWinners(quizData) {
    // 타이머 종료
    if(timeIntervalId) {
        clearInterval(timeIntervalId);
    }
    timeLeft = 0;
    const timeLeftElem = document.getElementById("timeLeft");
    if(timeLeftElem) timeLeftElem.textContent = 0;

    const finalWinnersList = document.getElementById("finalWinnersList");
    if (!finalWinnersList) return;

    const answerBtnFinal = document.getElementById("answerBtn");
    if(answerBtnFinal) answerBtnFinal.disabled = true;

    const winner = document.getElementById("winner");
    winner.textContent = quizData.email;

    // 정답 & 설명 표시
    document.getElementById("correctAnswer").style.display = "block";
    document.getElementById("correctAnswerText").textContent = ans;
    document.getElementById("description").style.display = "block";
    document.getElementById("descriptionText").textContent = des;

    // 기존 리스트 초기화
    finalWinnersList.innerHTML = "";
    // 숨겨진 리스트를 표시
    finalWinnersList.style.display = "block";

    // 우승자가 없거나 배열이 비었을 때 처리
    if (remainQuizValue === 0) {
        showToast("전원 탈락! 최종 우승자는 없습니다. 5초 뒤에 로비로 이동합니다.");
        setTimeout(() => {
            const roomId = window.location.pathname.split("/")[2];
            window.location.href = `/room/${roomId}`;
        },5000);
    }

    // 배열에 있는 각 우승자를 <li>로 표시
    quizData.finalWinners.forEach(winner => {
        const li = document.createElement("li");
        li.textContent = winner;
        finalWinnersList.appendChild(li);
    });

    // finalWinners가 배열이라 가정
    const finalWinnersArray = quizData.finalWinners; // ["alice@example.com", "bob@example.com"]
    let finalWinnersText = "알 수 없음";
    if (Array.isArray(finalWinnersArray) && finalWinnersArray.length > 0) {
        // 쉼표로 구분된 문자열로 합치기
        finalWinnersText = finalWinnersArray.join(", ");
    }

    // 축하 토스트 메시지와 리다이렉트
    showToast(`모든 퀴즈가 끝났습니다! 최종 우승자는 ${finalWinnersText} 입니다! 축하합니다! 5초뒤에 로비로 이동합니다.`);
    const roomId = window.location.pathname.split("/")[2];
    setTimeout(() => {
        window.location.href = `/room/${roomId}`;
    }, 5000);
}

// 타이머 시작 함수
function startTimer() {
    if (timeIntervalId) {
        clearInterval(timeIntervalId);
    }

    timeLeft = 30;
    const timeLeftElem = document.getElementById("timeLeft");

    timeIntervalId = setInterval(() => {
        timeLeft--;
        if (timeLeftElem) timeLeftElem.textContent = timeLeft;
        // Case 1) 아직 남은 퀴즈 있음
        if (timeLeft <= 0) {
            clearInterval(timeIntervalId);
            if (remainQuizValue !== 0) {
                showToast("시간 종료!");
                // Admin에서 createQuiz 버튼 활성화
                const createQuizBtn = document.getElementById("createQuizBtn");
                const answerBtn = document.getElementById("answerBtn");
                if (createQuizBtn) {
                    createQuizBtn.disabled = false;
                }
                if(answerBtn) {
                    answerBtn.disabled = true;
                }
                // 현재 문제 정답 & 설명 표시 (ans, des)
                document.getElementById("correctAnswer").style.display = "block";
                document.getElementById("correctAnswerText").textContent = ans;
                document.getElementById("description").style.display = "block";
                document.getElementById("descriptionText").textContent = des;
            }
            // Case 2) 남은 퀴즈가 0 => 최종 우승자 처리
            else {
                handleFinalWinners(curQuiz)
            }
        }
    }, 1000);
}

// 퀴즈 상태 업데이트
function updateQuizStatus(quizData) {
    // 남은 문제 수 갱신
    const remainQuizElem = document.getElementById("remainQuiz");
    if (remainQuizElem) {
        remainQuizValue = quizData.quizCount;
        remainQuizElem.textContent = remainQuizValue;
    }

    // 문제 내용 갱신
    const problemElem = document.getElementById("problem");
    if (problemElem) {
        problemElem.textContent = quizData.problem || "문제가 없습니다.";
    }

    // 정답 제출 버튼 활성화
    const answerBtn = document.getElementById("answerBtn");
    if (answerBtn) {
        answerBtn.disabled = false;
    }
    // 제한 시간 갱신
    startTimer();
}

function showToast(message, duration = 3000) {
    const toastContainer = document.getElementById("toast-container");

    // Toast 메시지 생성
    const toast = document.createElement("div");
    toast.className = "toast";
    toast.innerText = message;

    // 컨테이너에 추가
    toastContainer.appendChild(toast);

    // 지정된 시간 후에 삭제
    setTimeout(() => {
        toast.remove();
    }, duration);
}

// 정답과 설명과 현재 우승자를 숨기는 함수
function hideAnswerAndDescription() {
    const correctAnswerElem = document.getElementById("correctAnswerText");
    const descriptionElem = document.getElementById("descriptionText");
    const currentWinnerElem = document.getElementById("winner");

    if (correctAnswerElem) {
        correctAnswerElem.textContent = ""; // 내용도 초기화
    }

    if (descriptionElem) {
        descriptionElem.textContent = ""; // 내용도 초기화
    }

    if (currentWinnerElem) {
        currentWinnerElem.textContent = ""; // 내용도 초기화
    }
}