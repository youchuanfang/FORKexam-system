package com.exam.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "paper_targets")
public class PaperTarget {

    public static final String TYPE_ALL = "ALL";
    public static final String TYPE_CLASS = "CLASS";
    public static final String TYPE_STUDENT = "STUDENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "paper_id", nullable = false)
    private Integer paperId;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Integer targetId;

    public PaperTarget() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getPaperId() { return paperId; }
    public void setPaperId(Integer paperId) { this.paperId = paperId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }
}
