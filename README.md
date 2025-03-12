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
## 배포
 - Rolling 방식의 배포 자동화를 구축했습니다.
 - dev 브랜치에 머지가 발생할 경우 배포를 진행합니다.
---
## 배포 정보
 - 배포 서버 : AWS EC2
 - 배포 툴 : Github Actions

