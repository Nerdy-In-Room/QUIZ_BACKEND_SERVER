window.addEventListener("load", () => {
    const [navigationEntry] = performance.getEntriesByType('navigation');

    if (navigationEntry) {
        if (navigationEntry.type === 'navigate' || navigationEntry.type === 'reload') {

            connect();
        }
    }
});

window.addEventListener("beforeunload", () => {
    disconnect()
});

let stompClient;
let subscription;
let allReady;

const RECONNECT_DELAY = 5000;
const MAX_RECONNECT_ATTEMPTS = 10;
let reconnectAttempts = 0;

function connect() {
    const socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    const roomId = window.location.pathname.split('/')[2];

    stompClient.connect({"heart-beat": "10000,10000"}, function (frame) {
        reconnectAttempts = 0;
        stompClient.debug = function (str) {
        };

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
            const parseData = JSON.parse(res.body);

            if (parseData.hasOwnProperty('roomId')) {
                updateParticipant(parseData);
            } else if (parseData.hasOwnProperty("userId") && parseData.hasOwnProperty("readyStatus")) {
                handleServerMessage(parseData);
            }
            else if (parseData.hasOwnProperty("gameStarted") && parseData.gameStarted === true) {
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

function setupReadyButton(roomId, ready) {
    const userId = ready.getAttribute("data-user-id");
    stompClient.send(`/room/${roomId}/ready`, {}, JSON.stringify({userId: userId}));
}

function setupStartButton(roomId) {
    if (!allReady) {
        alert("사용자들이 모두 준비가 완료되지 않았습니다.");
    } else {
        stompClient.send(`/room/${roomId}/start`, {}, JSON.stringify({}));
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
    console.log(`Attempting to reconnect in ${RECONNECT_DELAY / 1000} seconds...`);

    setTimeout(() => {
        connect();
    }, RECONNECT_DELAY);
}
