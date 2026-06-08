package com.exam.dto.student;

public class PaperListDTO {
    private Integer paperId;
    private String title;
    private Integer duration;
    private String openStartTime;
    private String openEndTime;
    private Integer maxAttempts;
    private Integer attemptCount;
    private Integer remainingAttempts;
    private Double bestScore;
    private String status;
    private String statusText;
    private Integer inProgressRecordId;

    public PaperListDTO() {}

    public PaperListDTO(Integer paperId, String title, Integer duration) {
        this.paperId = paperId;
        this.title = title;
        this.duration = duration;
    }

    public Integer getPaperId() { return paperId; }
    public void setPaperId(Integer paperId) { this.paperId = paperId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getOpenStartTime() { return openStartTime; }
    public void setOpenStartTime(String openStartTime) { this.openStartTime = openStartTime; }
    public String getOpenEndTime() { return openEndTime; }
    public void setOpenEndTime(String openEndTime) { this.openEndTime = openEndTime; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
    public Integer getRemainingAttempts() { return remainingAttempts; }
    public void setRemainingAttempts(Integer remainingAttempts) { this.remainingAttempts = remainingAttempts; }
    public Double getBestScore() { return bestScore; }
    public void setBestScore(Double bestScore) { this.bestScore = bestScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Integer getInProgressRecordId() { return inProgressRecordId; }
    public void setInProgressRecordId(Integer inProgressRecordId) { this.inProgressRecordId = inProgressRecordId; }
}
