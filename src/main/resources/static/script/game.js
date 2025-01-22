window.addEventListener("load", () => {
    const [navigationEntry] = performance.getEntriesByType('navigation');

    if (navigationEntry) {
        if (navigationEntry.type === 'navigate' || navigationEntry.type === 'reload') {
            connect();
        } else if (navigationEntry.type === 'back_forward') {
            window.location.href = '/room-list';
        }
    }
});

window.addEventListener("beforeunload", () => {
    if (nextURL !== "quiz") {
        disconnect()
    }
});

let stompClient;
let subscription;
let allReady;
let nextURL = null;
let remainQuizInitialized = false; // 초기화 여부
let remainQuizValue = 0;

let reconnectAttempts = 0;
const BASE_DELAY = 1000;
const MAX_DELAY = 30000;
const MAX_RECONNECT_ATTEMPTS = 10;

function connect() {
    const socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    reconnectAttempts = 0;
    const roomId = window.location.pathname.split('/')[2];

    stompClient.connect({"heart-beat": "10000,10000"}, function (frame) {
        const startButton = document.getElementById("start-btn");
        const readyButton = document.getElementById("ready-btn");
        const remainQuizElem = document.getElementById("remainQuiz");
        const remainQuizGameElem = document.getElementById("count");
        const storedValue = sessionStorage.getItem("remainQuizValue");

        if (storedValue !== null) {
            remainQuizValue = Number(storedValue);
            console.log("Loaded remainQuizValue from session:", remainQuizValue);
        } else {
            // sessionStorage에 없으면 DOM에서 읽고 저장
            remainQuizValue = Number(remainQuizElem.textContent.trim()) || 0;
            sessionStorage.setItem("remainQuizValue", remainQuizValue);
        }

        remainQuizElem.textContent = remainQuizValue;
        remainQuizGameElem.textContent = remainQuizValue;

        if (startButton === null) {
            readyButton.addEventListener("click", () => {
                setupReadyButton(roomId, readyButton)
                if (nextURL === null) {
                    nextURL = "quiz";
                } else {
                    nextURL = null;
                }
            });
        } else {
            startButton.addEventListener("click", () => {
                setupStartButton(roomId);
                nextURL = "quiz";
            });
        }

        subscription = stompClient.subscribe("/pub/room/" + roomId, function (res) {
            const parseData = JSON.parse(res.body);

            if (parseData.hasOwnProperty('roomId')) {
                updateParticipant(parseData);
            } else if (parseData.hasOwnProperty("userId") && parseData.hasOwnProperty("readyStatus")) {
                handleServerMessage(parseData);
            }
            else if (parseData.hasOwnProperty("remainQuiz")) {
                window.location.href = `/quiz/${roomId}`;
            }
        });

    }, function (error) {
        console.log("WebSocket connection error or disconnected: ", error);

        scheduleReconnect();
    });
}

function disconnect() {
    if (stompClient !== null) {
        if (subscription !== null) {
            subscription.unsubscribe();
        }
        stompClient.disconnect();
    }
}

// remainQuizValue가 변경될 때마다 sessionStorage에 저장
function updateRemainQuizValue(newValue) {
    remainQuizValue = newValue;
    sessionStorage.setItem("remainQuizValue", newValue);
}

function setupReadyButton(roomId, ready) {
    const userId = ready.getAttribute("data-user-id");
    stompClient.send(`/room/${roomId}/ready`, {}, JSON.stringify({
        userId: userId
    }));
}

function setupStartButton(roomId) {
    if (!allReady) {
        showToast(`사용자들이 모두 준비가 완료되지 않았습니다.`);
    } else {
        stompClient.send(`/room/${roomId}/start`, {}, JSON.stringify({
            remainQuiz : remainQuizValue
        }));
    }
}

function handleServerMessage(message) {
    updateParticipantStatus(message.userId, message.readyStatus);

    if (message.hasOwnProperty("allReadyStatus")) {
        updateGameStatus(message.allReadyStatus);
    } else {
        console.warn("Unknown message type:", message);
    }
}

function updateGameStatus(isAllReady) {
    const gameStatusElem = document.getElementById("gameStatus");
    if (!gameStatusElem) return;
    allReady = isAllReady;

    if (isAllReady) {
        gameStatusElem.textContent = "Ready";
    } else {
        gameStatusElem.textContent = "Not Ready";
    }
}

function updateParticipantStatus(userId, status) {
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);

    if (!row) {
        console.warn("No row found for userId:", userId);

        return;
    }

    const statusCell = row.querySelector("td:nth-child(3)");

    if (!statusCell) {
        console.warn("No status cell found in row for userId:", userId);

        return;
    }

    statusCell.innerText = status ? "Ready" : "Not Ready";
}

function updateParticipant(participant) {
    const participantsTable = document.getElementById("participants");
    const existingRow = participantsTable.querySelector(`tr[data-user-id="${participant.id}"]`);

    if (existingRow) {
        const readyStatusCell = existingRow.querySelector("td:nth-child(3)");
        readyStatusCell.textContent = participant.readyStatus ? "Ready" : "Not Ready";
        readyStatusCell.classList.toggle("ready-true", participant.readyStatus);
        readyStatusCell.classList.toggle("ready-false", !participant.readyStatus);
    } else {
        const row = document.createElement("tr");
        row.setAttribute("data-user-id", participant.id);

        const idCell = document.createElement("td");
        idCell.textContent = participant.id;
        row.appendChild(idCell);

        const usernameCell = document.createElement("td");
        usernameCell.textContent = participant.username;
        row.appendChild(usernameCell);

        const readyStatusCell = document.createElement("td");
        readyStatusCell.textContent = participant.readyStatus ? "Ready" : "Not Ready";
        readyStatusCell.classList.add(participant.readyStatus ? "ready-true" : "ready-false");
        row.appendChild(readyStatusCell);

        participantsTable.appendChild(row);
    }
}

function scheduleReconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        console.error(`Max reconnect attempts reached (${MAX_RECONNECT_ATTEMPTS}). Giving up.`);

        return;
    }

    reconnectAttempts++;
    const delay = Math.min(BASE_DELAY * Math.pow(2, reconnectAttempts), MAX_DELAY);

    setTimeout(() => {
        connect();
    }, delay);
}

function showToast(message, duration = 2000) {
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