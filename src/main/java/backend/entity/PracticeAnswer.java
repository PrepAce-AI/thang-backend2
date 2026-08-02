package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chi tiết 1 câu trả lời trong 1 lượt Luyện Đề (Practice Test).
 * Được tạo ngay khi bắt đầu làm bài (selected_option_id = null),
 * cập nhật khi nộp bài — nhờ đó server biết chính xác 25 câu đã phát
 * cho học sinh, chống gian lận và cho phép xem lại kết quả bất kỳ lúc nào.
 */
@Entity
@Table(name = "PracticeAnswers")
@Getter @Setter @NoArgsConstructor
public class PracticeAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_answer_id")
    private Integer practiceAnswerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    /** null = học sinh bỏ trống câu này */
    @Column(name = "selected_option_id")
    private Integer selectedOptionId;

    /** null = chưa chấm; điền khi nộp bài */
    @Column(name = "is_correct")
    private Boolean isCorrect;

    /** Thứ tự câu hỏi trong đề (1..25) để render lại đúng thứ tự */
    @Column(name = "question_order", nullable = false)
    private Integer questionOrder = 0;
    @Column(name = "essay_answer", columnDefinition = "NVARCHAR(MAX)")
    private String essayAnswer;
    @Column(name = "score")
    private Double score;

    @Column(name = "teacher_comment", columnDefinition = "NVARCHAR(MAX)")
    private String teacherComment;
}
