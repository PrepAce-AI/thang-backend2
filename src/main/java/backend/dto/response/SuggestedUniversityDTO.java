package backend.dto.response;

public class SuggestedUniversityDTO {
    private String universityName;
    private String location;
    private Double matchScore;

    public SuggestedUniversityDTO(String universityName, String location, Double matchScore) {
        this.universityName = universityName;
        this.location = location;
        this.matchScore = matchScore;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }
}