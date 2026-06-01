package com.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "paper_questions")
public class PaperQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "paper_id")
    private Integer paperId;

    @Column(name = "question_id")
    private Integer questionId;

    @Column
    private Double score;

    public PaperQuestion() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPaperId() { return paperId; }
    public void setPaperId(Integer paperId) { this.paperId = paperId; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
