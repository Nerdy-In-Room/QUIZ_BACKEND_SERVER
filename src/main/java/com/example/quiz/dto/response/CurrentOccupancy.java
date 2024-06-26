package com.example.quiz.dto.response;

public class CurrentOccupancy {
    public Long roomId;
    public int currentPeople;

    public CurrentOccupancy(Long roomId, int currentPeople) {
        this.roomId = roomId;
        this.currentPeople = currentPeople;
    }
}
