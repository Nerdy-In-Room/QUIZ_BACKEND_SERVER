package com.example.quiz.dto.room;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ChangeCurrentOccupancies{
    long roomId;
    int currentPeople;

    public ChangeCurrentOccupancies(long roomId, int currentPeople) {
        this.roomId = roomId;
        this.currentPeople = currentPeople;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        ChangeCurrentOccupancies that = (ChangeCurrentOccupancies) object;
        return roomId == that.roomId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roomId);
    }
}
