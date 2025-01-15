window.onload = () => {
    const [navigationEntry] = performance.getEntriesByType('navigation');

    if (navigationEntry) {
        if (navigationEntry.type === 'navigate' || navigationEntry.type === 'reload') {
            console.log('connect');
            connect();
        }
    }
};

window.onbeforeunload = () => {
    disconnect();
};

let stompClient;
let subscription;
let allReady;

const RECONNECT_DELAY = 5000;
const MAX_RECONNECT_ATTEMPTS = 10;
let reconnectAttempts = 0;

// WebSocket 연결
function connect() {
    const socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    const roomId = window.location.pathname.split('/')[2];

    stompClient.connect({"heart-beat": "10000,10000"}, function (frame) {
        reconnectAttempts = 0;

        const startButton = document.getElementById("start-btn");
        const readyButton = document.getElementById("ready-btn");

        if (startButton === null) {
            readyButton.addEventListener("click", () => {
                setupReadyButton(roomId, readyButton)
            });
        } else {
            startButton.addEventListener("click", () => {
                setupStartButton(roomId);
            });
        }

        subscription = stompClient.subscribe("/pub/room/" + roomId, function (res) {
            console.log("res: ", res);
            const parseData = JSON.parse(res.body);

            if (parseData.hasOwnProperty('roomId')) {
                updateParticipant(parseData);
            } else if (parseData.hasOwnProperty("userId") && parseData.hasOwnProperty("readyStatus")) {
                handleServerMessage(parseData);
            } // 3) 전체 게임 시작 신호
            else if (parseData.hasOwnProperty("gameStarted") && parseData.gameStarted === true) {
                // 모든 클라이언트 동시에 퀴즈 페이지로 이동
                window.location.href = `/quiz/${roomId}`;
            }
        });

    }, function (error) {
        console.log("WebSocket connection error or disconnected: ", error);

        scheduleReconnect();
    });
}

function setupReadyButton(roomId, ready) {
    const userId = ready.getAttribute("data-user-id");
    stompClient.send(`/room/${roomId}/ready`, {}, JSON.stringify({userId: userId}));
}

function setupStartButton(roomId) {
    if(!allReady) {
        alert("사용자들이 모두 준비가 완료되지 않았습니다.");
    }
    else {
        // 게임 시작 메시지 전송 → 서버가 "gameStarted" 브로드캐스트
        stompClient.send(`/room/${roomId}/start`, {}, JSON.stringify({}));
        console.log("Sent start game request.");
    }
}

function handleServerMessage(message) {
    updateParticipantStatus(message.userId, message.readyStatus);
    // 게임 상태 업데이트
    if (message.hasOwnProperty("allReadyStatus")) {
        updateGameStatus(message.allReadyStatus);
    } else {
        console.warn("Unknown message type:", message);
    }
}

function updateGameStatus(isAllReady) {
    const gameStatusElem = document.getElementById("gameStatus");
    if(!gameStatusElem) return;
    allReady = isAllReady;

    if(isAllReady) {
        gameStatusElem.textContent = "Ready";
    }
    else {
        gameStatusElem.textContent = "Not Ready";
    }
}

function updateParticipantStatus(userId, status) {
    console.log("updateParticipantStatus called!", userId, status);
    const row = document.querySelector(`tr[data-user-id="${userId}"]`);
    console.log("row: ", row)

    if (!row) {
        console.warn("No row found for userId:", userId);

        return;
    }

    const statusCell = row.querySelector("td:nth-child(3)");

    console.log("statusCell: ", statusCell);
    if (!statusCell) {
        console.warn("No status cell found in row for userId:", userId);

        return;
    }

    statusCell.innerText = status ? "Ready" : "Not Ready";
}

function updateParticipant(participant) {
    const participantsTable = document.getElementById("participants");

    console.log("part table: ", participantsTable);
    const existingRow = participantsTable.querySelector(`tr[data-user-id="${participant.id}"]`);
    console.log("existing row: ", existingRow);

    if (existingRow) {
        // 기존 사용자가 있으면 Ready Status만 업데이트
        const readyStatusCell = existingRow.querySelector("td:nth-child(3)");
        readyStatusCell.textContent = participant.readyStatus ? "Ready" : "Not Ready";
        readyStatusCell.classList.toggle("ready-true", participant.readyStatus);
        readyStatusCell.classList.toggle("ready-false", !participant.readyStatus);
    } else {
        // 새 사용자 추가
        const row = document.createElement("tr");
        row.setAttribute("data-user-id", participant.id);

        // User ID
        const idCell = document.createElement("td");
        idCell.textContent = participant.id;
        row.appendChild(idCell);

        // Username
        const usernameCell = document.createElement("td");
        usernameCell.textContent = participant.username;
        row.appendChild(usernameCell);

        // Ready Status
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
    console.log(`Attempting to reconnect in ${RECONNECT_DELAY / 1000} seconds...`);

    setTimeout(() => {
        connect();
    }, RECONNECT_DELAY);
}

function disconnect() {
    if (stompClient !== null) {
        if (subscription !== null) {
            subscription.unsubscribe();
        }
        stompClient.disconnect();
    }
}