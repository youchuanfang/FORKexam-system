package com.exam.dto.common;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ClassRoomDTO {
    private Integer id;
    private String name;
    private Integer teacherId;
    private String joinCode;
    private LocalDateTime createdAt;
    private Integer memberCount;
    private List<StudentDTO> students = new ArrayList<>();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }
    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public List<StudentDTO> getStudents() { return students; }
    public void setStudents(List<StudentDTO> students) { this.students = students; }
}
