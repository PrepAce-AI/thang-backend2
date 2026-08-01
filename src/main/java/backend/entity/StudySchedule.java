package backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "StudySchedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    @JsonProperty("schedule_id")
    private Integer scheduleId;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Integer userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "schedule_date", nullable = false)
    @JsonProperty("schedule_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    @Column(name = "schedule_time", nullable = false, length = 10)
    @JsonProperty("schedule_time")
    private String scheduleTime;

    @Column(name = "schedule_type", length = 20)
    @JsonProperty("schedule_type")
    private String scheduleType;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    @JsonProperty("created_at")
    private Date createdAt = new Date();

    // 🔥 THÊM THUỘC TÍNH NÀY VÀO ĐÂY:
    @Column(name = "reminder_sent", nullable = false)
    @JsonProperty("reminder_sent")
    private Boolean reminderSent = false;
}
