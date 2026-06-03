package com.exam.dto.student;

public class AnswerDetailDTO {
    private Integer questionId;
    private String type;
    private String content;
    private String options;
    private String studentAnswer;
    private Boolean isCorrect;
    private Double score;
    private Double scoreGot;
    // Reserved for teacher-side answer publishing. Filled only when students are allowed to view answers.
    private String correctAnswer;

    public AnswerDetailDTO() {}

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
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Double getScoreGot() { return scoreGot; }
    public void setScoreGot(Double scoreGot) { this.scoreGot = scoreGot; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
}
