package backend.dto.response;

public class CourseReportResponse {
    private int userId;
    private String userFullName;
    private int totalLessons;
    private int completedLessons;
    private double progressPercentage;
    private Double averageScore;

    public CourseReportResponse() {}

    public CourseReportResponse(int userId, String userFullName, int totalLessons, int completedLessons, double progressPercentage, Double averageScore) {
        this.userId = userId;
        this.userFullName = userFullName;
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.progressPercentage = progressPercentage;
        this.averageScore = averageScore;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }
}
