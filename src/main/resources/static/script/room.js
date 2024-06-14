// document.querySelector('a[data-roomid]').addEventListener('click', (event) => {
//     event.preventDefault();
//     let roomId = event.target.dataset.roomid;
//     console.log(roomId)
//
//     fetch(`/room/${roomId}`)
//         .then(response => {
//             if (!response.ok) {
//                 throw new Error(`HTTP error! status: ${response.status}`);
//             }
//             return response.json();
//         })
//         .catch(error => {
//             if (error.message.includes('403')) {
//                 alert('권한이 없습니다.');
//             } else {
//                 alert('알 수 없는 오류가 발생했습니다.');
//             }
//         });
// });

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
                } else {
                    alert('알 수 없는 오류가 발생했습니다.');
                }
            });
    })
);