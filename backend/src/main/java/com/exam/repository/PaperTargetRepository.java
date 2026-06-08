package com.exam.repository;

import com.exam.entity.PaperTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperTargetRepository extends JpaRepository<PaperTarget, Integer> {
    List<PaperTarget> findByPaperId(Integer paperId);
    void deleteByPaperId(Integer paperId);
}
