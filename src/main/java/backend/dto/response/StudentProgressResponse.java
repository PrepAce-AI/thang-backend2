package backend.dto.response;

import java.util.Date;

public class StudentProgressResponse {
    private Integer id;
    private int userId;
    private String userFullName;
    private int lessonId;
    private Boolean chapterCompleted;
    private Integer chapterId;
    private Boolean isCompleted;
    private Double score;
    private Date lastAccessed;
    private Double lastVideoTime;

    public StudentProgressResponse() {}

    public StudentProgressResponse(Integer id, int userId, String userFullName, int lessonId, Boolean chapterCompleted, Integer chapterId, Boolean isCompleted, Double score, Date lastAccessed, Double lastVideoTime) {
        this.id = id;
        this.userId = userId;
        this.userFullName = userFullName;
        this.lessonId = lessonId;
        this.chapterCompleted = chapterCompleted;
        this.chapterId = chapterId;
        this.isCompleted = isCompleted;
        this.score = score;
        this.lastAccessed = lastAccessed;
        this.lastVideoTime = lastVideoTime;
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

    public Double getLastVideoTime() {
        return lastVideoTime;
    }

    public void setLastVideoTime(Double lastVideoTime) {
        this.lastVideoTime = lastVideoTime;
    }

    public Boolean getChapterCompleted() {
        return chapterCompleted;
    }

    public void setChapterCompleted(Boolean chapterCompleted) {
        this.chapterCompleted = chapterCompleted;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Boolean getCompleted() {
        return isCompleted;
    }

    public void setCompleted(Boolean completed) {
        isCompleted = completed;
    }
}
