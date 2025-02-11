let timeLeft = 30;
let remainQuizValue = 0;
let timeIntervalId = null;
let stompClient;
let ans;
let des;
let curQuiz;

window.onload = function () {
    initPage();
    connectToQuizUpdates();
};

function initPage() {
    const createQuizBtn = document.getElementById("createQuizBtn");
    if (createQuizBtn) {
        createQuizBtn.addEventListener("click", () => {
            sendCreateQuizEvent();
        });
    }

    const answerBtn = document.getElementById("answerBtn");
    if (answerBtn) {
        answerBtn.disabled = true;
        answerBtn.addEventListener("click", () => {
            checkQuizEvent()
        })
    }
}

function connectToQuizUpdates() {
    const roomId = window.location.pathname.split("/")[2];
    const socket = new SockJS("/ingame" + "?roomId=" + roomId);
    stompClient = Stomp.over(socket);

    stompClient.connect({"heart-beat": "10000,10000"}, function (frame) {
        stompClient.debug = function (str) {
        };

        stompClient.subscribe(`/pub/quiz/${roomId}`, function (res) {
            const quizData = JSON.parse(res.body);
            ans = quizData.correctAnswer;
            des = quizData.description;

            if (quizData.hasOwnProperty("problem")) {
                updateQuizStatus(quizData);
                hideAnswerAndDescription();
            } else if (quizData.finalResult && remainQuizValue === 1) {
                handleFinalWinners(quizData);
            } else {
                handleWinner(quizData);
            }
        });
    });
}

function sendCreateQuizEvent() {
    const roomId = window.location.pathname.split("/")[2];
    stompClient.send(`/room/${roomId}/send`, {}, JSON.stringify({}));

    const createQuizBtn = document.getElementById("createQuizBtn");
    const answerBtn = document.getElementById("answerBtn");
    if (createQuizBtn) {
        createQuizBtn.disabled = true;
    }
    if (answerBtn) {
        answerBtn.disabled = false;
    }
}

function checkQuizEvent() {
    const userId = document.getElementById("userId").textContent;
    const roomId = window.location.pathname.split("/")[2];
    const userAnswer = document.getElementById("answerInput").value.trim();
    if(remainQuizValue === 1) {
        stompClient.send(`/room/${roomId}/check`, {}, JSON.stringify({
            userId: userId,
            answer: userAnswer,
            finalQuiz : true
        }));
    }

    stompClient.send(`/room/${roomId}/check`, {}, JSON.stringify({
        userId: userId,
        answer: userAnswer,
        finalQuiz : false
    }));

    document.getElementById("answerInput").value = "";
}

function handleWinner(quizData) {
    const winnerSpan = document.getElementById("winner");
    if (!quizData.currentResult) {
        showToast("틀렸습니다.")
        return;
    }

    winnerSpan.textContent = quizData.email;

    if (timeIntervalId) {
        clearInterval(timeIntervalId);
    }

    timeLeft = 0;
    const timeLeftElem = document.getElementById("timeLeft");
    if (timeLeftElem) timeLeftElem.textContent = 0;

    document.getElementById("correctAnswer").style.display = "block";
    document.getElementById("correctAnswerText").textContent = quizData.correctAnswer;
    document.getElementById("description").style.display = "block";
    document.getElementById("descriptionText").textContent = quizData.description;

    // 마지막 문제 제외한 토스트 메시지
    if(remainQuizValue !== 1) {
        showToast(`이번 문제의 승리자는 \n ${quizData.email} 님입니다!`, 3000);
    }

    const createQuizBtn = document.getElementById("createQuizBtn");
    if (createQuizBtn) {
        createQuizBtn.disabled = false;
    }
    const answerBtn = document.getElementById("answerBtn");
    if (answerBtn) answerBtn.disabled = true;

    curQuiz = quizData;
}

function handleFinalWinners(quizData) {
    if (timeIntervalId) {
        clearInterval(timeIntervalId);
    }
    timeLeft = 0;
    const timeLeftElem = document.getElementById("timeLeft");
    if (timeLeftElem) timeLeftElem.textContent = 0;

    const finalWinnersList = document.getElementById("finalWinnersList");
    if (!finalWinnersList) return;

    const createQuizBtn = document.getElementById("createQuizBtn");
    if (createQuizBtn) createQuizBtn.disabled = true;

    const answerBtnFinal = document.getElementById("answerBtn");
    if (answerBtnFinal) answerBtnFinal.disabled = true;

    const winner = document.getElementById("winner");
    winner.textContent = quizData.email;

    // 정답 & 설명 표시
    document.getElementById("correctAnswer").style.display = "block";
    document.getElementById("correctAnswerText").textContent = ans;
    document.getElementById("description").style.display = "block";
    document.getElementById("descriptionText").textContent = des;

    finalWinnersList.innerHTML = "";
    finalWinnersList.style.display = "block";

    if (remainQuizValue === 1 && !quizData.finalWinners) {
        showToast("전원 탈락! \n 최종 우승자는 없습니다. \n 5초 뒤에 로비로 이동합니다.");
        setTimeout(() => {
            const roomId = window.location.pathname.split("/")[2];
            window.location.href = `/room/${roomId}`;
        }, 5000);
    }

    quizData.finalWinners.forEach(winner => {
        const li = document.createElement("li");
        li.textContent = winner;
        finalWinnersList.appendChild(li);
    });

    const finalWinnersArray = quizData.finalWinners;
    let finalWinnersText = "알 수 없음";
    if (Array.isArray(finalWinnersArray) && finalWinnersArray.length > 0) {
        finalWinnersText = finalWinnersArray.join(", ");
    }

    showToast(`모든 퀴즈가 끝났습니다! \n 최종 우승자는 ${finalWinnersText} 입니다! \n 축하합니다! 5초뒤에 로비로 이동합니다.`);
    const roomId = window.location.pathname.split("/")[2];
    setTimeout(() => {
        window.location.href = `/room/${roomId}`;
    }, 5000);
}

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
            if (remainQuizValue !== 1) {
                showToast("시간 종료!");

                const createQuizBtn = document.getElementById("createQuizBtn");
                const answerBtn = document.getElementById("answerBtn");
                if (createQuizBtn) {
                    createQuizBtn.disabled = false;
                }
                if (answerBtn) {
                    answerBtn.disabled = true;
                }

                document.getElementById("correctAnswer").style.display = "block";
                document.getElementById("correctAnswerText").textContent = ans;
                document.getElementById("description").style.display = "block";
                document.getElementById("descriptionText").textContent = des;
            } else {
                handleFinalWinners(curQuiz)
            }
        }
    }, 1000);
}

function updateQuizStatus(quizData) {
    const remainQuizElem = document.getElementById("remainQuiz");
    if (remainQuizElem) {
        remainQuizValue = remainQuizElem.textContent--;
    }

    const problemElem = document.getElementById("problem");
    if (problemElem) {
        problemElem.textContent = quizData.problem || "문제가 없습니다.";
    }

    const answerBtn = document.getElementById("answerBtn");
    if (answerBtn) {
        answerBtn.disabled = false;
    }
    startTimer();
}

function showToast(message, duration = 3000) {
    const toastContainer = document.getElementById("toast-container");

    const toast = document.createElement("div");
    toast.className = "toast";
    toast.innerText = message;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, duration);
}

function hideAnswerAndDescription() {
    const correctAnswerElem = document.getElementById("correctAnswerText");
    const descriptionElem = document.getElementById("descriptionText");
    const currentWinnerElem = document.getElementById("winner");

    if (correctAnswerElem) {
        correctAnswerElem.textContent = "";
    }

    if (descriptionElem) {
        descriptionElem.textContent = "";
    }

    if (currentWinnerElem) {
        currentWinnerElem.textContent = "";
    }
}
