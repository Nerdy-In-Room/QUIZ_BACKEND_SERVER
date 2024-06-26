document.querySelectorAll('a').forEach(anchor =>
    anchor.addEventListener('click', (event) => {
        let roomId = event.target.dataset.roomid;
        event.preventDefault();

        fetch(`/room/${roomId}`)
            .then(response => {

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                window.location.href = `/room/${roomId}`;
            })
            .catch(error => {
                if (error.message.includes('403')) {
                    alert('권한이 없습니다.');
                } else if (error.message.includes('401')) {
                    alert('중복 입장은 불가능합니다.')
                } else {
                    alert('알 수 없는 오류가 발생했습니다.');
                }
            });
    })
);

window.addEventListener("load", function () {
    // HTML 요소에서 방 ID 목록을 읽어오기
    let roomsContainer = document.getElementById('roomsContainer');
    let roomsData = roomsContainer.getAttribute('data-rooms');
    let currentPageRooms = roomsData.split(',').map(Number);

    let sock = new SockJS('/updateOccupancy');

    sock.onopen = function() {
        setInterval(function() {
            sock.send(JSON.stringify(currentPageRooms));
        }, 5000);
    };

    sock.onmessage = function (event) {
        let occupancyList = JSON.parse(event.data);

        occupancyList.forEach(function (occupancy) {
            $('#room-' + occupancy.roomId + ' .current-people').text(occupancy.currentPeople);
        });
    };
    // let stompClient = Stomp.over(socket);
    //
    // stompClient.connect({}, function(frame) {
    //     console.log('Connected: ' + frame);
    //
    //     // 주기적으로 현재 페이지의 방 ID 목록을 서버에 전송
    //     setInterval(function() {
    //         stompClient.send('/room-people/updateOccupancy', {}, JSON.stringify(currentPageRooms));
    //     }, 5000);
    //
    //     // 서버로부터 업데이트된 데이터를 수신하여 페이지를 갱신
    //     stompClient.subscribe('/pub/occupancy', function(message) {
    //         let rooms = JSON.parse(message.body);
    //
    //         rooms.forEach(function(room) {
    //             $('#room-' + room.roomId + ' .current-people').text(room.currentPeople);
    //         });
    //     });
    // });
});
