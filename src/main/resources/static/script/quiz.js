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

    stompClient.connect({}, function (frame) {
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

    timeLeft = 30;
    const timeLeftElem = document.getElementById("timeLeft");

    timeIntervalId = setInterval(() => {
        timeLeft--;
        if (timeLeftElem) timeLeftElem.textContent = timeLeft;

        if (timeLeft <= 0) {
            clearInterval(timeIntervalId);
            alert("시간 종료!");

            // Admin에서 createQuiz 버튼 활성화
            const createQuizBtn = document.getElementById("createQuizBtn");
            if (createQuizBtn) {
                createQuizBtn.disabled = false;
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