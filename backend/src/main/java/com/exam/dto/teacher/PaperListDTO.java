package com.exam.dto.teacher;

import com.exam.dto.common.PaperTargetDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaperListDTO {
    private Integer id;
    private String title;
    private Integer duration;
    private LocalDateTime openStartTime;
    private LocalDateTime openEndTime;
    private Integer maxAttempts;
    private Boolean releaseAnswerFlag;
    private LocalDateTime answerReleaseTime;
    private Boolean published;
    private LocalDateTime publishedAt;
    private Boolean leaderboardPublic;
    private String targetSummary;
    private Double averageScore;
    private List<PaperTargetDTO> targets = new ArrayList<>();
    private Integer questionCount;
    private Integer recordCount;

    public PaperListDTO() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public LocalDateTime getOpenStartTime() { return openStartTime; }
    public void setOpenStartTime(LocalDateTime openStartTime) { this.openStartTime = openStartTime; }
    public LocalDateTime getOpenEndTime() { return openEndTime; }
    public void setOpenEndTime(LocalDateTime openEndTime) { this.openEndTime = openEndTime; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public Boolean getReleaseAnswerFlag() { return releaseAnswerFlag; }
    public void setReleaseAnswerFlag(Boolean releaseAnswerFlag) { this.releaseAnswerFlag = releaseAnswerFlag; }
    public LocalDateTime getAnswerReleaseTime() { return answerReleaseTime; }
    public void setAnswerReleaseTime(LocalDateTime answerReleaseTime) { this.answerReleaseTime = answerReleaseTime; }
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public Boolean getLeaderboardPublic() { return leaderboardPublic; }
    public void setLeaderboardPublic(Boolean leaderboardPublic) { this.leaderboardPublic = leaderboardPublic; }
    public String getTargetSummary() { return targetSummary; }
    public void setTargetSummary(String targetSummary) { this.targetSummary = targetSummary; }
    public Double getAverageScore() { return averageScore; }
    public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
    public List<PaperTargetDTO> getTargets() { return targets; }
    public void setTargets(List<PaperTargetDTO> targets) { this.targets = targets; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
}
