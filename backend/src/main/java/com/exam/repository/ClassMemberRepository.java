package com.exam.repository;

import com.exam.entity.ClassMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, Integer> {
    List<ClassMember> findByClassId(Integer classId);
    List<ClassMember> findByStudentId(Integer studentId);
    List<ClassMember> findByClassIdIn(Collection<Integer> classIds);
    Optional<ClassMember> findByClassIdAndStudentId(Integer classId, Integer studentId);
    boolean existsByClassIdAndStudentId(Integer classId, Integer studentId);
    void deleteByClassIdAndStudentId(Integer classId, Integer studentId);
}
