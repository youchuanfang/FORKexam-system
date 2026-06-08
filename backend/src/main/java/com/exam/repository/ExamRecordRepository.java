package com.exam.repository;

import com.exam.entity.ExamRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRecordRepository extends JpaRepository<ExamRecord, Integer> {
    List<ExamRecord> findByStudentId(Integer studentId);
    List<ExamRecord> findByPaperId(Integer paperId);
    List<ExamRecord> findByPaperIdAndSubmitTimeIsNotNull(Integer paperId);
    Optional<ExamRecord> findByStudentIdAndPaperId(Integer studentId, Integer paperId);
}
