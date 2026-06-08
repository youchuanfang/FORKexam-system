package com.exam.dto.teacher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaperStatisticsDTO {
    private Integer submittedCount;
    private Double overallAverage;
    private Double selectedStudentsAverage;
    private Double maxScore;
    private Double minScore;
    private List<ClassAverageDTO> classAverages = new ArrayList<>();
    private List<StudentScoreDTO> studentScores = new ArrayList<>();
    private Map<String, Long> scoreDistribution = new LinkedHashMap<>();

    public Integer getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(Integer submittedCount) { this.submittedCount = submittedCount; }
    public Double getOverallAverage() { return overallAverage; }
    public void setOverallAverage(Double overallAverage) { this.overallAverage = overallAverage; }
    public Double getSelectedStudentsAverage() { return selectedStudentsAverage; }
    public void setSelectedStudentsAverage(Double selectedStudentsAverage) { this.selectedStudentsAverage = selectedStudentsAverage; }
    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }
    public Double getMinScore() { return minScore; }
    public void setMinScore(Double minScore) { this.minScore = minScore; }
    public List<ClassAverageDTO> getClassAverages() { return classAverages; }
    public void setClassAverages(List<ClassAverageDTO> classAverages) { this.classAverages = classAverages; }
    public List<StudentScoreDTO> getStudentScores() { return studentScores; }
    public void setStudentScores(List<StudentScoreDTO> studentScores) { this.studentScores = studentScores; }
    public Map<String, Long> getScoreDistribution() { return scoreDistribution; }
    public void setScoreDistribution(Map<String, Long> scoreDistribution) { this.scoreDistribution = scoreDistribution; }

    public static class ClassAverageDTO {
        private Integer classId;
        private String className;
        private Integer submittedCount;
        private Double averageScore;

        public Integer getClassId() { return classId; }
        public void setClassId(Integer classId) { this.classId = classId; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public Integer getSubmittedCount() { return submittedCount; }
        public void setSubmittedCount(Integer submittedCount) { this.submittedCount = submittedCount; }
        public Double getAverageScore() { return averageScore; }
        public void setAverageScore(Double averageScore) { this.averageScore = averageScore; }
    }

    public static class StudentScoreDTO {
        private Integer studentId;
        private String username;
        private Double score;

        public Integer getStudentId() { return studentId; }
        public void setStudentId(Integer studentId) { this.studentId = studentId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
