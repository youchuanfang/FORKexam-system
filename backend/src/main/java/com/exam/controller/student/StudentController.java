package com.exam.controller.student;

import com.exam.common.Result;
import com.exam.dto.student.ExamRecordDTO;
import com.exam.dto.student.ExamRecordDetailDTO;
import com.exam.dto.student.PaperDetailDTO;
import com.exam.dto.student.PaperListDTO;
import com.exam.dto.student.StartExamRequest;
import com.exam.dto.student.StartExamResponse;
import com.exam.dto.student.SubmitExamRequest;
import com.exam.dto.student.SubmitExamResponse;
import com.exam.service.student.StudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
