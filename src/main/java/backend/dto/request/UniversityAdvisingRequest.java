package backend.dto.request;

import lombok.*;

@Data
public class UniversityAdvisingRequest {
    private Integer studentId;
    private String block; // A, A1, B, D...
    private Double score;  // optional
    private String interest; // optional (IT, medicine...)

    public UniversityAdvisingRequest(Integer studentId, String block, Double score, String interest) {
        this.studentId = studentId;
        this.block = block;
        this.score = score;
        this.interest = interest;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }
}
