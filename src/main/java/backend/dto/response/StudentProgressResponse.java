package backend.dto.response;

import java.util.Date;

public class StudentProgressResponse {
    private Integer id;
    private int userId;
    private String userFullName;
    private int lessonId;
    private Boolean isCompleted;
    private Double score;
    private Date lastAccessed;

    public StudentProgressResponse() {}

    public StudentProgressResponse(Integer id, int userId, String userFullName, int lessonId, Boolean isCompleted, Double score, Date lastAccessed) {
        this.id = id;
        this.userId = userId;
        this.userFullName = userFullName;
        this.lessonId = lessonId;
        this.isCompleted = isCompleted;
        this.score = score;
        this.lastAccessed = lastAccessed;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public int getLessonId() {
        return lessonId;
    }

    public void setLessonId(int lessonId) {
        this.lessonId = lessonId;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Date getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(Date lastAccessed) {
        this.lastAccessed = lastAccessed;
    }
}
