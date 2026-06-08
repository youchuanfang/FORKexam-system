package com.exam.dto.common;

public class PaperTargetDTO {
    private String targetType;
    private Integer targetId;
    private String targetName;

    public PaperTargetDTO() {}

    public PaperTargetDTO(String targetType, Integer targetId, String targetName) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}
