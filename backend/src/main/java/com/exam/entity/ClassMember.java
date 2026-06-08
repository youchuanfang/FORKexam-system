package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "class_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_member", columnNames = {"class_id", "student_id"})
)
public class ClassMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    public ClassMember() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
