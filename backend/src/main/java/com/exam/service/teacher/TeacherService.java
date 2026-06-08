package com.exam.service.teacher;

import com.exam.common.PageResult;
import com.exam.dto.teacher.*;
import com.exam.dto.common.ClassRoomDTO;
import com.exam.dto.common.LeaderboardItemDTO;
import com.exam.dto.common.PaperTargetDTO;
import com.exam.dto.common.StudentDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface TeacherService {

    // Question management
    List<QuestionDTO> getQuestions(String type);
    PageResult<QuestionDTO> getQuestionsPaged(String type, String keyword, int page, int size);
    QuestionDTO createQuestion(QuestionDTO dto);
    QuestionDTO updateQuestion(Integer id, QuestionDTO dto);
    void deleteQuestion(Integer id);
    List<QuestionDTO> createQuestionsBatch(List<QuestionDTO> dtos);

    // Paper management
    List<PaperListDTO> getPapers();
    PageResult<PaperListDTO> getPapersPaged(int page, int size);
    PaperListDTO getPaper(Integer paperId);
    PaperListDTO createPaper(PaperListDTO dto);
    PaperListDTO updatePaper(Integer paperId, PaperListDTO dto);
    void deletePaper(Integer paperId);
    PaperListDTO publishPaper(Integer paperId, List<PaperTargetDTO> targets);
    PaperListDTO unpublishPaper(Integer paperId);
    PaperListDTO updateLeaderboardVisibility(Integer paperId, Boolean visible);

    // Paper questions
    List<PaperQuestionDTO> getPaperQuestions(Integer paperId);
    void assignQuestions(Integer paperId, List<PaperQuestionDTO> questions);

    // Class management
    List<ClassRoomDTO> getClasses();
    ClassRoomDTO createClass(String name);
    ClassRoomDTO getClassDetail(Integer classId);
    ClassRoomDTO addClassStudent(Integer classId, Integer studentId);
    void removeClassStudent(Integer classId, Integer studentId);
    List<StudentDTO> getStudents(String keyword);

    // Exam records
    List<ExamRecordDTO> getExamRecords(Integer paperId);
    PageResult<ExamRecordDTO> getExamRecordsPaged(Integer paperId, int page, int size);
    ExamRecordDetailDTO getExamRecordDetail(Integer recordId);
    PaperStatisticsDTO getPaperStatistics(Integer paperId, Integer classId, List<Integer> studentIds);
    List<LeaderboardItemDTO> getLeaderboard(Integer paperId);

    // Grading & Export
    void gradeQuestion(Integer recordId, GradeRequest request);
    void exportPaperRecords(Integer paperId, HttpServletResponse response);
}
