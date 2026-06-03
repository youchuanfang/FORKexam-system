package com.exam.dto.student;

public class SubmitExamResponse {
    private Integer recordId;
    private Double totalScore;
    private Double objectiveScore;
    private String message;

    public SubmitExamResponse() {}

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    public Double getObjectiveScore() { return objectiveScore; }
    public void setObjectiveScore(Double objectiveScore) { this.objectiveScore = objectiveScore; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
