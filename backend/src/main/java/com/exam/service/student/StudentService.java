package com.exam.service.student;

import com.exam.dto.student.ExamRecordDTO;
import com.exam.dto.student.ExamRecordDetailDTO;
import com.exam.dto.student.PaperDetailDTO;
import com.exam.dto.student.PaperListDTO;
import com.exam.dto.student.StartExamRequest;
import com.exam.dto.student.StartExamResponse;
import com.exam.dto.student.SubmitExamRequest;
import com.exam.dto.student.SubmitExamResponse;
import com.exam.dto.common.ClassRoomDTO;
import com.exam.dto.common.LeaderboardItemDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface StudentService {
    List<PaperListDTO> getPapers();

    PaperDetailDTO getPaperDetail(Integer paperId);

    StartExamResponse startExam(StartExamRequest request);

    SubmitExamResponse submitExam(Integer recordId, SubmitExamRequest request);

    List<ExamRecordDTO> getMyExamRecords();

    ExamRecordDetailDTO getExamRecordDetail(Integer recordId);

    List<ClassRoomDTO> getMyClasses();

    ClassRoomDTO joinClass(String joinCode);

    List<LeaderboardItemDTO> getLeaderboard(Integer paperId);

    void exportWrongQuestions(Integer recordId, HttpServletResponse response);
}
