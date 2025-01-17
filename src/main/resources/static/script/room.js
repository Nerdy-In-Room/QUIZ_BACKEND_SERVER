window.addEventListener("load", function () {
    let roomsContainer = document.getElementById('roomsContainer');

    let sock = new SockJS('/updateOccupancy');
    let stompClient = Stomp.over(sock);

    stompClient.connect({}, function (frame) {
        stompClient.debug = function(str) {};

        stompClient.subscribe('/pub/occupancy', function (message) {
                let rooms = JSON.parse(message.body);
                let emptyMessageRow = document.getElementById("empty-message");

                if (Array.isArray(rooms)) {
                    rooms.forEach(room => {
                        if (room.currentPeople === 0) {
                            let roomRow = document.getElementById('room-' + room.roomId);
                            if (roomRow) {
                                roomRow.remove();

                                let tableBody = document.getElementById('room-table-body');

                                if (tableBody.children.length === 0 && !emptyMessageRow) {
                                    let newEmptyMessageRow = document.createElement('tr');
                                    newEmptyMessageRow.id = 'empty-message';
                                    newEmptyMessageRow.innerHTML = `
                            <td colspan="7" style="text-align: center;">현재 생성된 방이 없습니다.</td>
                        `;
                                    tableBody.appendChild(newEmptyMessageRow);
                                }
                            }
                        } else {
                            let roomRow = document.getElementById('room-' + room.roomId);

                            roomRow.querySelector('.current-people').textContent = room.currentPeople;

                        }
                    })
                } else {
                    let tableBody = document.getElementById('room-table-body');

                    if (emptyMessageRow) {
                        emptyMessageRow.remove();
                    }

                    roomRow = document.createElement('tr');
                    roomRow.id = 'room-' + rooms.roomId;
                    roomRow.innerHTML = `
                        <td>${rooms.roomId}</td>
                        <td>${rooms.roomName}</td>
                        <td>${rooms.topicId}</td>
                        <td>${rooms.maxPeople}</td>
                        <td>${rooms.quizCount}</td>
                        <td class="current-people">${rooms.currentPeople}</td>
                        <td><a data-roomid="${rooms.roomId}" href="/room/${rooms.roomId}">enter</a></td>
                    `;
                    tableBody.insertBefore(roomRow, tableBody.firstChild);
                }
            }
        );
    });
});
