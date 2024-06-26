// let socket = new SockJS('/game');
// let stompClient = Stomp.over(socket);
//
// stompClient.connect({}, function(frame) {
//     let roomId = window.location.pathname.split('/')[2]
//     stompClient.subscribe('/room/' + roomId, function(response){
//
//         console.log("구독 잘 됨");
//     });
// });

window.onload = connect;
window.onbeforeunload = disconnect;

let stompClient;
let subscription;

function connect() {
    let socket = new SockJS("/game");
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log(frame);

        let roomId = window.location.pathname.split('/')[2];
        subscription = stompClient.subscribe("/room/" + roomId, function (res) {
            console.log(res);
        });
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