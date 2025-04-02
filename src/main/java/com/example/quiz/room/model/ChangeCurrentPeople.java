package com.example.quiz.room.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeCurrentPeople {
    long roomId;
    int currentPeople;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        ChangeCurrentPeople that = (ChangeCurrentPeople) object;
        return roomId == that.roomId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roomId);
    }
}
