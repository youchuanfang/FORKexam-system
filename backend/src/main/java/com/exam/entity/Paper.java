package com.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "papers")
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String title;

    private Integer duration;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "teacher_open_answer")
    private Boolean teacherOpenAnswer = false;

    @Column(name = "open_start_time")
    private LocalDateTime openStartTime;

    @Column(name = "open_end_time")
    private LocalDateTime openEndTime;

    public Paper() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public Boolean getTeacherOpenAnswer() { return teacherOpenAnswer; }
    public void setTeacherOpenAnswer(Boolean teacherOpenAnswer) { this.teacherOpenAnswer = teacherOpenAnswer; }
    public LocalDateTime getOpenStartTime() { return openStartTime; }
    public void setOpenStartTime(LocalDateTime openStartTime) { this.openStartTime = openStartTime; }
    public LocalDateTime getOpenEndTime() { return openEndTime; }
    public void setOpenEndTime(LocalDateTime openEndTime) { this.openEndTime = openEndTime; }
}
