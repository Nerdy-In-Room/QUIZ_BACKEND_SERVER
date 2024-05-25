package com.example.quiz.repository;


import com.example.quiz.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Page<Room> findAllByRemoveStatus(boolean removeStatus, Pageable pageable);
}
