package com.exam.dto.student;

import java.time.LocalDateTime;
import java.util.List;

public class ExamRecordDetailDTO {
    private Integer recordId;
    private Integer paperId;
    private String paperTitle;
    private Integer duration;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Double totalScore;
    private List<QuestionDTO> questions;
    private List<AnswerDetailDTO> answers;

    public ExamRecordDetailDTO() {}

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }
    public Integer getPaperId() { return paperId; }
    public void setPaperId(Integer paperId) { this.paperId = paperId; }
    public String getPaperTitle() { return paperTitle; }
    public void setPaperTitle(String paperTitle) { this.paperTitle = paperTitle; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    public List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDTO> questions) { this.questions = questions; }
    public List<AnswerDetailDTO> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDetailDTO> answers) { this.answers = answers; }
}
