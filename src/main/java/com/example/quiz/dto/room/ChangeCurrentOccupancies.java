package com.example.quiz.dto.room;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeCurrentOccupancies{
    long roomId;
    int currentPeople;

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
