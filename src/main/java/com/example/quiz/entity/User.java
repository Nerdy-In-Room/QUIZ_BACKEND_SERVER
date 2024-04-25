package com.example.quiz.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String userRole;
    private Integer readyStatus;

    public User(Long userId, String userRole, Integer readyStatus) {
        this.userId = userId;
        this.userRole = userRole;
        this.readyStatus = readyStatus;
    }
}
