package com.exam.dto.student;

import java.util.List;

public class SubmitExamRequest {
    private List<SubmitAnswerDTO> answers;

    public SubmitExamRequest() {}

    public List<SubmitAnswerDTO> getAnswers() { return answers; }
    public void setAnswers(List<SubmitAnswerDTO> answers) { this.answers = answers; }
}
