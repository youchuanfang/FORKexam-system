package com.exam.dto.student;

import java.time.LocalDateTime;

public class StartExamResponse {
    private Integer recordId;
    private Integer paperId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime openStartTime;
    private LocalDateTime openEndTime;
    private Integer duration;
    private java.util.List<QuestionDTO> questions;
    private Boolean resumed;

    public StartExamResponse() {}

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }
    public Integer getPaperId() { return paperId; }
    public void setPaperId(Integer paperId) { this.paperId = paperId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getOpenStartTime() { return openStartTime; }
    public void setOpenStartTime(LocalDateTime openStartTime) { this.openStartTime = openStartTime; }
    public LocalDateTime getOpenEndTime() { return openEndTime; }
    public void setOpenEndTime(LocalDateTime openEndTime) { this.openEndTime = openEndTime; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public java.util.List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(java.util.List<QuestionDTO> questions) { this.questions = questions; }
    public Boolean getResumed() { return resumed; }
    public void setResumed(Boolean resumed) { this.resumed = resumed; }
}
