package com.exam.dto.student;

public class QuestionDTO {
    private Integer questionId;
    private String type;
    private String content;
    private String options;
    private Double score;

    public QuestionDTO() {}

    public QuestionDTO(Integer questionId, String type, String content, String options, Double score) {
        this.questionId = questionId;
        this.type = type;
        this.content = content;
        this.options = options;
        this.score = score;
    }

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
