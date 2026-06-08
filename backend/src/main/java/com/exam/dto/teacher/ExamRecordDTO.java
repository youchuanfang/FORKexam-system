package com.exam.dto.teacher;

import java.time.LocalDateTime;

public class ExamRecordDTO {
    private Integer recordId;
    private Integer paperId;
    private String paperTitle;
    private Integer studentId;
    private String studentName;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Double totalScore;
    private Boolean submitted;
    private Boolean hasSubjective;

    public ExamRecordDTO() {}

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }
    public Integer getPaperId() { return paperId; }
    public void setPaperId(Integer paperId) { this.paperId = paperId; }
    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    public Boolean getSubmitted() { return submitted; }
    public void setSubmitted(Boolean submitted) { this.submitted = submitted; }
    public Boolean getHasSubjective() { return hasSubjective; }
    public void setHasSubjective(Boolean hasSubjective) { this.hasSubjective = hasSubjective; }
}
