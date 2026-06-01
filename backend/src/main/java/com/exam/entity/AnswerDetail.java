package com.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "answer_details")
public class AnswerDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "record_id")
    private Integer recordId;

    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "student_answer", columnDefinition = "NVARCHAR(MAX)")
    private String studentAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "score_got")
    private Double scoreGot;

    public AnswerDetail() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public String getStudentAnswer() { return studentAnswer; }
    public void setStudentAnswer(String studentAnswer) { this.studentAnswer = studentAnswer; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public Double getScoreGot() { return scoreGot; }
    public void setScoreGot(Double scoreGot) { this.scoreGot = scoreGot; }
}
