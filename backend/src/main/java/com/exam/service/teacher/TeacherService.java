package com.exam.service.teacher;

import com.exam.dto.teacher.*;

import java.util.List;

public interface TeacherService {

    // Question management
    List<QuestionDTO> getQuestions(String type);
    QuestionDTO createQuestion(QuestionDTO dto);
    QuestionDTO updateQuestion(Integer id, QuestionDTO dto);
    void deleteQuestion(Integer id);

    // Paper management
    List<PaperListDTO> getPapers();
    PaperListDTO getPaper(Integer paperId);
    PaperListDTO createPaper(PaperListDTO dto);
    PaperListDTO updatePaper(Integer paperId, PaperListDTO dto);
    void deletePaper(Integer paperId);

    // Paper questions
    void assignQuestions(Integer paperId, List<PaperQuestionDTO> questions);

    // Exam records
    List<ExamRecordDTO> getExamRecords(Integer paperId);
    ExamRecordDetailDTO getExamRecordDetail(Integer recordId);

    // Grading
    void gradeQuestion(Integer recordId, GradeRequest request);
}
