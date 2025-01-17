// 전역 변수
let timeLeft = 30;
let remainQuizValue = 0; // 나중에 DOM에서 초기화
let timeIntervalId = null; // 타이머 ID
let stompClient;

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
            console.log("Received quiz data:", quizData);

            // 받은 데이터를 바탕으로 퀴즈 상태 업데이트
            updateQuizStatus(quizData);
        });
    });
}

// createQuiz 이벤트 WebSocket으로 전송
function sendCreateQuizEvent() {
    const roomId = window.location.pathname.split("/")[2];
    stompClient.send(`/room/${roomId}/send`, {}, JSON.stringify({}));

    // Admin에서 버튼을 비활성화
    const createQuizBtn = document.getElementById("createQuizBtn");
    if (createQuizBtn) {
        createQuizBtn.disabled = true;
    }
}

// 타이머 시작 함수
function startTimer() {
    if (timeIntervalId) {
        clearInterval(timeIntervalId);
    }

    timeLeft = 5;
    const timeLeftElem = document.getElementById("timeLeft");

    timeIntervalId = setInterval(() => {
        timeLeft--;
        if (timeLeftElem) timeLeftElem.textContent = timeLeft;

        if (timeLeft <= 0) {
            clearInterval(timeIntervalId);
            showToast("시간 종료");

            // Admin에서 createQuiz 버튼 활성화
            const createQuizBtn = document.getElementById("createQuizBtn");
            if (createQuizBtn) {
                createQuizBtn.disabled = false;
            }

            if (remainQuizValue === 0) {
                // 최종 우승자 표시
                const finalWinnerElem = document.getElementById("finalWinner");
                const finalWinner = finalWinnerElem ? finalWinnerElem.textContent : "알 수 없음";

                // 축하 메시지와 리다이렉트
                alert(`모든 퀴즈가 끝났습니다! 최종 우승자는 ${finalWinner}입니다! 축하합니다!`);
                const roomId = window.location.pathname.split("/")[2];
                window.location.href = `/room/${roomId}`;
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