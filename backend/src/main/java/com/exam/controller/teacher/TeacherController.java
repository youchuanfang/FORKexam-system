package com.exam.controller.teacher;

import com.exam.common.Result;
import com.exam.dto.teacher.*;
import com.exam.service.teacher.TeacherService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // ==================== Paper Management ====================

    @GetMapping("/papers")
    public Result<List<PaperListDTO>> getPapers() {
        return Result.success(teacherService.getPapers());
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

    @PostMapping("/papers/{paperId}/questions")
    public Result<?> assignQuestions(@PathVariable Integer paperId, @RequestBody List<PaperQuestionDTO> questions) {
        teacherService.assignQuestions(paperId, questions);
        return Result.success();
    }

    @GetMapping("/papers/{paperId}/questions")
    public Result<List<PaperQuestionDTO>> getPaperQuestions(@PathVariable Integer paperId) {
        return Result.success(teacherService.getPaperQuestions(paperId));
    }

    // ==================== Exam Records ====================

    @GetMapping("/exam-records")
    public Result<List<ExamRecordDTO>> getExamRecords(@RequestParam Integer paperId) {
        return Result.success(teacherService.getExamRecords(paperId));
    }

    @GetMapping("/exam-records/{recordId}")
    public Result<ExamRecordDetailDTO> getExamRecordDetail(@PathVariable Integer recordId) {
        return Result.success(teacherService.getExamRecordDetail(recordId));
    }

    @PostMapping("/exam-records/{recordId}/grade")
    public Result<?> gradeQuestion(@PathVariable Integer recordId, @RequestBody GradeRequest request) {
        teacherService.gradeQuestion(recordId, request);
        return Result.success();
    }
}
