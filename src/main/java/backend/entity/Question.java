package backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonBackReference // Thay cho @JsonIgnore để phối hợp đồng bộ với @JsonManagedReference từ Quiz
    private Quiz quiz;

    @Column(name = "question_content", columnDefinition = "LONGTEXT", nullable = false)
    private String questionContent;

    @Column(name = "explanation", columnDefinition = "LONGTEXT")
    private String explanation;

    // 🌟 THÊM TRƯỜNG NÀY ĐỂ LƯU ĐÁP ÁN ĐIỀN/NGẮN CHO HỆ THỐNG TỰ SO KHỚP
    @Column(name = "correct_answer", columnDefinition = "LONGTEXT")
    private String correctAnswer;

    @Column(name = "topic", columnDefinition = "NVARCHAR(255)")
    private String topic;

    @Column(name = "subject", columnDefinition = "NVARCHAR(255)")
    private String subject;

    @Column(name = "created_at", insertable = false, updatable = true)
    public Date createdAt;

    @Column(name = "difficulty")
    private Integer difficulty;

    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<StudentAnswer> studentAnswers = new ArrayList<>();

    @Column(name = "question_type", length = 50)
    private String questionType;
}
