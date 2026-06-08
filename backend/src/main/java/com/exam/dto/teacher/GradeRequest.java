package com.exam.dto.teacher;

public class GradeRequest {
    private Integer questionId;
    private Double scoreGot;

    public GradeRequest() {}

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Double getScoreGot() { return scoreGot; }
    public void setScoreGot(Double scoreGot) { this.scoreGot = scoreGot; }
}
