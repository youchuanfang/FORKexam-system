package com.exam.controller.student;

import com.exam.common.Result;
import com.exam.dto.common.ClassRoomDTO;
import com.exam.dto.common.LeaderboardItemDTO;
import com.exam.dto.student.ExamRecordDTO;
import com.exam.dto.student.ExamRecordDetailDTO;
import com.exam.dto.student.PaperDetailDTO;
import com.exam.dto.student.PaperListDTO;
import com.exam.dto.student.StartExamRequest;
import com.exam.dto.student.StartExamResponse;
import com.exam.dto.student.SubmitExamRequest;
import com.exam.dto.student.SubmitExamResponse;
import com.exam.service.student.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/papers")
    public Result<List<PaperListDTO>> getPapers() {
        return Result.success(studentService.getPapers());
    }

    @GetMapping("/papers/{paperId}")
    public Result<PaperDetailDTO> getPaperDetail(@PathVariable Integer paperId) {
        return Result.success(studentService.getPaperDetail(paperId));
    }

    @PostMapping("/exam-records")
    public Result<StartExamResponse> startExam(@RequestBody StartExamRequest request) {
        return Result.success(studentService.startExam(request));
    }

    @PostMapping("/exam-records/{recordId}/submit")
    public Result<SubmitExamResponse> submitExam(@PathVariable Integer recordId,
                                                 @RequestBody SubmitExamRequest request) {
        return Result.success(studentService.submitExam(recordId, request));
    }

    @GetMapping("/exam-records")
    public Result<List<ExamRecordDTO>> getMyExamRecords() {
        return Result.success(studentService.getMyExamRecords());
    }

    @GetMapping("/exam-records/{recordId}")
    public Result<ExamRecordDetailDTO> getExamRecordDetail(@PathVariable Integer recordId) {
        return Result.success(studentService.getExamRecordDetail(recordId));
    }

    @GetMapping("/classes")
    public Result<List<ClassRoomDTO>> getMyClasses() {
        return Result.success(studentService.getMyClasses());
    }

    @PostMapping("/classes/join")
    public Result<ClassRoomDTO> joinClass(@RequestBody Map<String, String> body) {
        return Result.success(studentService.joinClass(body == null ? null : body.get("joinCode")));
    }

    @GetMapping("/papers/{paperId}/leaderboard")
    public Result<List<LeaderboardItemDTO>> getLeaderboard(@PathVariable Integer paperId) {
        return Result.success(studentService.getLeaderboard(paperId));
    }

    @GetMapping("/exam-records/{recordId}/export-wrong-questions")
    public void exportWrongQuestions(@PathVariable Integer recordId, HttpServletResponse response) {
        studentService.exportWrongQuestions(recordId, response);
    }
}
