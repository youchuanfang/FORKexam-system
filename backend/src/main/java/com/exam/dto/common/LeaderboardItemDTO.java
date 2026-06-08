package com.exam.dto.common;

import java.time.LocalDateTime;

public class LeaderboardItemDTO {
    private Integer rank;
    private String username;
    private Double score;
    private LocalDateTime submitTime;

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
}
