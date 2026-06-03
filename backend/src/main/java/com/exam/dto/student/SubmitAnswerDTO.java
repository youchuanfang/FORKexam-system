package com.exam.dto.student;

public class SubmitAnswerDTO {
    private Integer questionId;
    private Object answer;

    public SubmitAnswerDTO() {}

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Object getAnswer() { return answer; }
    public void setAnswer(Object answer) { this.answer = answer; }
}
