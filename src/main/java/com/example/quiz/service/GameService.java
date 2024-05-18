package com.example.quiz.service;

import com.example.quiz.dto.response.ResponseMessage;
import com.example.quiz.game.domain.Game;
import com.example.quiz.game.domain.GameUser;
import com.example.quiz.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {

    private final GameRepository gameRepository;

    public ResponseMessage gameStatusService(String roomId, Long userId){

        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setUserId(userId);

        // 개발자의 역활에 따른 로직을 나누어야 한다.
        // 유저의 역할이 admin이면 방에 들어온 유저의 수와 게임에 status가 true인 사람의 갯수 -1를 비교하여
        // 똑같으면 게임의 상태를 true로 변경하고 admin의 status 도 변경한다.
        log.info("id의 궁금:{}",roomId);

        // 해당 게임의 데이터 베이스를 찾아오는 코드
        Optional<Game> game1 = gameRepository.findById(roomId);
        log.info("gmae1의 값은 {}", game1);
        Game game = gameRepository.findById(roomId).get();
        // 게임 데이터에서 유저들의 데이터만 받아오는 코드
        List<GameUser> gameUser = game.getGameUser();

        // requestUserInfo 에서 userid를 가지고 와서 user 정보를 찾음
        GameUser findUser = gameUser.stream().filter(user -> user.getUserId().equals(userId)).findFirst().get();

        // 해당 유저의 역할이 유저일 경우
        if (findUser.getUserRole().equals("USER")){
            // 유저의 정보를 수정함
            findUser.setReadyStatus(!findUser.getReadyStatus());

            gameRepository.save(game);
            responseMessage.setCheckedAnswer(findUser.getReadyStatus() ? true:false);
            // 해당 유저의 역할이 방장일 경우
        }else if (findUser.getUserRole().equals("ADMIN")){
            // TODO: 고도화 쿼리로 일 넘김
            long count = gameUser.stream().filter(user -> user.getReadyStatus().equals(true)).count();
            if (count == game.getCurrentParticipantsNo()-1){
                findUser.setReadyStatus(!findUser.getReadyStatus());
                // 유저의 정보를 수정함
                game.setIsGaming(true);
                gameRepository.save(game);
                responseMessage.setCheckedAnswer(true);
            }
        }
        return responseMessage;
    }
}
