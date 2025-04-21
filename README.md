# QUIZ_BACKEND_SERVER

## 설명
 - CS를 좀 더 쉽게 익히기 위한 퀴즈 게임
---
## 배경
 - 암기 방식으로 외우기만 하면 쉽게 까먹을 수 있습니다.
 - 누구나 방을 만들고 입장하여 퀴즈를 풀며 재밌게 CS 공부를 할 수 있도록하는 데에 목적을 두고 있습니다.
---
## 기술 스택
 - 서버 : Spring boot, Java, Spring data jpa
 - DB : MySQL, MongoDB, Redis
 - 웹 서버 : Nginx
 - 프론트 : Thymeleaf
 - 배포 : Github actions, AWS EC2
---
## room list , 입/퇴장 아키텍처
![image](https://github.com/user-attachments/assets/9292802e-84e7-4b73-a281-c97891d7db56)
### flow
1. room list 진입 시 서버로 구독 요청
2. room list 에서 방 생성 또는 방 입장 또는 게임방에서 퇴장
   1. 방 생성 시 라운드로빈 정책으로 방 생성 요청, redis 로 변동사항 발행
   2. 방 입장 시 roomId 를 기준으로 로드밸런싱, redis 로 변동사항 발행
   3. 방 퇴장 시 이벤트 기반으로 redis 로 변동사항 발행
3. redis 를 구독 중인 모든 서버에 변경사항 브로드캐스팅
4. 각 서버는 BlockingQueue 에 변동사항 삽입
5. Queue 내부에 제한만큼 변동사항이 쌓이거나 1초가 지나면 구독중인 클라이언트에 브로드캐스팅
---
## room(대기방) , 준비/시작 아키텍처
![image](https://github.com/user-attachments/assets/c8f763d5-07a4-4e2b-a842-f61a2a851303)
## flow
1. room 진입시 서버로 구독 요청
   - 'room/' 경로로 WebSocket pub/sub 요청
2. room 에서 게임 시작 또는 준비 대기방에서 퇴장
   - 준비버튼 클릭시 서버의 Ready 상태가 true 갱신
   - 시작버튼 클릭시 서버에서 모든 사용자가 Ready 인지 확인
   - 방 퇴장시 리스트에서 사용자 삭제
3. 모든 유저 준비시 인게임 화면으로 리다이렉트
4. 게임방이 시작되면 불필요한 정보들 갱신
   - room list 삭제 
   - 인게임 이동시 대기방 정보 삭제
   - roomDB에 remove Status 1로 갱신
5. 모든 게임 종료 후 앞서 삭제됐던 정보 롤백
---
## quiz(인게임) , 문제 출제/확인 아키텍처
![image](https://github.com/user-attachments/assets/d8aaefe0-43de-454b-bcab-69489bae84d9)
## flow
1. quiz 진입시 서버로 구독 요청
   - 'quiz/' 경로로 WebSocket pub/sub 요청
2. quiz 에서 문제 출제 또는 확인 인게임에서 퇴장
   - Admin 문제 출제 버튼 클릭시 중복되지 않는 새로운 문제 갱신
   - User 문제 확인 버튼 클릭시 정답이면 라운드 종료, 오답이면 토스트 메시지 출력
   - 방 퇴장시 바로 room(대기방) 이 아닌 room list 이동
3. 모든 라운드 종료시 room(대기방) 으로 리다이렉트
4. room(대기방) 으로 리다이렉트 시 사용자 정보들 갱신
   - removeStatus 0 으로 정상화
   - 다중 사용자 정보 정상화
   - room list 정상화
---
## 배포
 - Rolling 방식의 배포 자동화를 구축했습니다.
 - dev 브랜치에 머지가 발생할 경우 배포를 진행합니다.
---
## 배포 정보
 - 배포 서버 : AWS EC2
 - 배포 툴 : Github Actions

