package com.example.quiz.room.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeCurrentPeopleResponse {
    long roomId;
    int currentPeople;
    long version;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChangeCurrentPeopleResponse that = (ChangeCurrentPeopleResponse) o;
        return getRoomId() == that.getRoomId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getRoomId());
    }
}
