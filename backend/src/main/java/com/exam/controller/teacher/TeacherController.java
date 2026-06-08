package com.exam.controller.teacher;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.dto.common.ClassRoomDTO;
import com.exam.dto.common.LeaderboardItemDTO;
import com.exam.dto.common.PaperTargetDTO;
import com.exam.dto.common.StudentDTO;
import com.exam.dto.teacher.*;
import com.exam.service.teacher.TeacherService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    // ==================== Question Management ====================

    @GetMapping("/questions")
    public Result<List<QuestionDTO>> getQuestions(@RequestParam(required = false) String type) {
        return Result.success(teacherService.getQuestions(type));
    }

    @GetMapping("/questions/paged")
    public Result<PageResult<QuestionDTO>> getQuestionsPaged(@RequestParam(required = false) String type,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return Result.success(teacherService.getQuestionsPaged(type, keyword, page, size));
    }

    @PostMapping("/questions")
    public Result<QuestionDTO> createQuestion(@RequestBody QuestionDTO dto) {
        return Result.success(teacherService.createQuestion(dto));
    }

    @PutMapping("/questions/{id}")
    public Result<QuestionDTO> updateQuestion(@PathVariable Integer id, @RequestBody QuestionDTO dto) {
        return Result.success(teacherService.updateQuestion(id, dto));
    }

    @DeleteMapping("/questions/{id}")
    public Result<?> deleteQuestion(@PathVariable Integer id) {
        teacherService.deleteQuestion(id);
        return Result.success();
    }

    @PostMapping("/questions/batch")
    public Result<List<QuestionDTO>> createQuestionsBatch(@RequestBody List<QuestionDTO> dtos) {
        return Result.success(teacherService.createQuestionsBatch(dtos));
    }

    // ==================== Paper Management ====================

    @GetMapping("/papers")
    public Result<List<PaperListDTO>> getPapers() {
        return Result.success(teacherService.getPapers());
    }

    @GetMapping("/papers/paged")
    public Result<PageResult<PaperListDTO>> getPapersPaged(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return Result.success(teacherService.getPapersPaged(page, size));
    }

    @GetMapping("/papers/{paperId}")
    public Result<PaperListDTO> getPaper(@PathVariable Integer paperId) {
        return Result.success(teacherService.getPaper(paperId));
    }

    @PostMapping("/papers")
    public Result<PaperListDTO> createPaper(@RequestBody PaperListDTO dto) {
        return Result.success(teacherService.createPaper(dto));
    }

    @PutMapping("/papers/{paperId}")
    public Result<PaperListDTO> updatePaper(@PathVariable Integer paperId, @RequestBody PaperListDTO dto) {
        return Result.success(teacherService.updatePaper(paperId, dto));
    }

    @DeleteMapping("/papers/{paperId}")
    public Result<?> deletePaper(@PathVariable Integer paperId) {
        teacherService.deletePaper(paperId);
        return Result.success();
    }

    @PostMapping("/papers/{paperId}/publish")
    public Result<PaperListDTO> publishPaper(@PathVariable Integer paperId,
                                             @RequestBody(required = false) List<PaperTargetDTO> targets) {
        return Result.success(teacherService.publishPaper(paperId, targets));
    }

    @PostMapping("/papers/{paperId}/unpublish")
    public Result<PaperListDTO> unpublishPaper(@PathVariable Integer paperId) {
        return Result.success(teacherService.unpublishPaper(paperId));
    }

    @PutMapping("/papers/{paperId}/leaderboard-visibility")
    public Result<PaperListDTO> updateLeaderboardVisibility(@PathVariable Integer paperId,
                                                            @RequestBody Map<String, Boolean> body) {
        return Result.success(teacherService.updateLeaderboardVisibility(paperId, body == null ? false : body.get("leaderboardPublic")));
    }

    @PostMapping("/papers/{paperId}/questions")
    public Result<?> assignQuestions(@PathVariable Integer paperId, @RequestBody List<PaperQuestionDTO> questions) {
        teacherService.assignQuestions(paperId, questions);
        return Result.success();
    }

    @GetMapping("/papers/{paperId}/questions")
    public Result<List<PaperQuestionDTO>> getPaperQuestions(@PathVariable Integer paperId) {
        return Result.success(teacherService.getPaperQuestions(paperId));
    }

    // ==================== Class Management ====================

    @GetMapping("/classes")
    public Result<List<ClassRoomDTO>> getClasses() {
        return Result.success(teacherService.getClasses());
    }

    @PostMapping("/classes")
    public Result<ClassRoomDTO> createClass(@RequestBody Map<String, String> body) {
        return Result.success(teacherService.createClass(body == null ? null : body.get("name")));
    }

    @GetMapping("/classes/{classId}")
    public Result<ClassRoomDTO> getClassDetail(@PathVariable Integer classId) {
        return Result.success(teacherService.getClassDetail(classId));
    }

    @PostMapping("/classes/{classId}/students")
    public Result<ClassRoomDTO> addClassStudent(@PathVariable Integer classId, @RequestBody Map<String, Integer> body) {
        return Result.success(teacherService.addClassStudent(classId, body == null ? null : body.get("studentId")));
    }

    @DeleteMapping("/classes/{classId}/students/{studentId}")
    public Result<?> removeClassStudent(@PathVariable Integer classId, @PathVariable Integer studentId) {
        teacherService.removeClassStudent(classId, studentId);
        return Result.success();
    }

    @GetMapping("/students")
    public Result<List<StudentDTO>> getStudents(@RequestParam(required = false) String keyword) {
        return Result.success(teacherService.getStudents(keyword));
    }

    // ==================== Exam Records ====================

    @GetMapping("/exam-records")
    public Result<List<ExamRecordDTO>> getExamRecords(@RequestParam Integer paperId) {
        return Result.success(teacherService.getExamRecords(paperId));
    }

    @GetMapping("/exam-records/paged")
    public Result<PageResult<ExamRecordDTO>> getExamRecordsPaged(@RequestParam Integer paperId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.success(teacherService.getExamRecordsPaged(paperId, page, size));
    }

    @GetMapping("/exam-records/{recordId}")
    public Result<ExamRecordDetailDTO> getExamRecordDetail(@PathVariable Integer recordId) {
        return Result.success(teacherService.getExamRecordDetail(recordId));
    }

    @GetMapping("/papers/{paperId}/statistics")
    public Result<PaperStatisticsDTO> getPaperStatistics(@PathVariable Integer paperId,
                                                         @RequestParam(required = false) Integer classId,
                                                         @RequestParam(required = false) List<Integer> studentIds) {
        return Result.success(teacherService.getPaperStatistics(
                paperId,
                classId,
                studentIds == null ? new ArrayList<>() : studentIds
        ));
    }

    @GetMapping("/papers/{paperId}/leaderboard")
    public Result<List<LeaderboardItemDTO>> getLeaderboard(@PathVariable Integer paperId) {
        return Result.success(teacherService.getLeaderboard(paperId));
    }

    @PostMapping("/exam-records/{recordId}/grade")
    public Result<?> gradeQuestion(@PathVariable Integer recordId, @RequestBody GradeRequest request) {
        teacherService.gradeQuestion(recordId, request);
        return Result.success();
    }

    @GetMapping("/papers/{paperId}/export")
    public void exportPaperRecords(@PathVariable Integer paperId, HttpServletResponse response) {
        teacherService.exportPaperRecords(paperId, response);
    }
}
