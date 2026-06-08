package com.exam.repository;

import com.exam.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("SELECT q FROM Question q WHERE q.createdBy IS NULL OR q.createdBy = :teacherId")
    List<Question> findAccessibleQuestions(@Param("teacherId") Integer teacherId);

    @Query("SELECT q FROM Question q WHERE (q.createdBy IS NULL OR q.createdBy = :teacherId) AND q.type = :type")
    List<Question> findAccessibleByType(@Param("teacherId") Integer teacherId, @Param("type") String type);

    // 分页版本
    @Query("SELECT q FROM Question q WHERE q.createdBy IS NULL OR q.createdBy = :teacherId")
    Page<Question> findAccessibleQuestionsPaged(@Param("teacherId") Integer teacherId, Pageable pageable);

    @Query("SELECT q FROM Question q WHERE (q.createdBy IS NULL OR q.createdBy = :teacherId) AND q.type = :type")
    Page<Question> findAccessibleByTypePaged(@Param("teacherId") Integer teacherId, @Param("type") String type, Pageable pageable);

    // 关键词搜索（分页）
    @Query("SELECT q FROM Question q WHERE (q.createdBy IS NULL OR q.createdBy = :teacherId) AND (LOWER(q.content) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    Page<Question> searchAccessibleQuestionsPaged(@Param("teacherId") Integer teacherId, @Param("keyword") String keyword, Pageable pageable);
}
