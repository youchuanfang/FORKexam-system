package com.exam.dto.teacher;

import java.time.LocalDateTime;
import java.util.List;

public class ExamRecordDetailDTO {
    private Integer recordId;
    private Integer paperId;
    private String paperTitle;
    private Integer studentId;
    private String studentName;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Double totalScore;
    private Boolean submitted;
    private List<AnswerItem> answers;

    public ExamRecordDetailDTO() {}

    public static class AnswerItem {
        private Integer questionId;
        private String type;
        private String content;
        private String options;
        private String studentAnswer;
        private String correctAnswer;
        private Double score;
        private Double scoreGot;
        private Boolean isCorrect;
        private Boolean autoGraded;

        public AnswerItem() {}

        public Integer getQuestionId() { return questionId; }
        public void setQuestionId(Integer questionId) { this.questionId = questionId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getOptions() { return options; }
        public void setOptions(String options) { this.options = options; }
        public String getStudentAnswer() { return studentAnswer; }
        public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public Double getScoreGot() { return scoreGot; }
        public void setScoreGot(Double scoreGot) { this.scoreGot = scoreGot; }
        public Boolean getIsCorrect() { return isCorrect; }
        public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
        public Boolean getAutoGraded() { return autoGraded; }
        public void setAutoGraded(Boolean autoGraded) { this.autoGraded = autoGraded; }
    }

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
    public List<AnswerItem> getAnswers() { return answers; }
    public void setAnswers(List<AnswerItem> answers) { this.answers = answers; }
}
