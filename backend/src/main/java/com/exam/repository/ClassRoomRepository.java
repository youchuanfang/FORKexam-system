package com.exam.repository;

import com.exam.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Integer> {
    List<ClassRoom> findByTeacherId(Integer teacherId);
    Optional<ClassRoom> findByJoinCode(String joinCode);
}
