package backend.dto.response;

public class SuggestedMajorDTO {
    private String majorName;
    private String reason;

    public SuggestedMajorDTO(String majorName, String reason) {
        this.majorName = majorName;
        this.reason = reason;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}