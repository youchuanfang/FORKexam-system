package com.exam.dto.teacher;

public class PaperQuestionDTO {
    private Integer questionId;
    private Double score;

    public PaperQuestionDTO() {}

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
