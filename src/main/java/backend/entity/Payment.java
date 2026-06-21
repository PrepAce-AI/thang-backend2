package backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "Payments")
@Getter @Setter @NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "amount", precision = 10, scale = 2)
    private BigDecimal amount;

    /** VNPAY | MOMO | BANK_TRANSFER | FREE */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /** PENDING | SUCCESS | FAILED | REFUNDED */
    @Column(name = "payment_status", length = 50)
    private String paymentStatus;

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Column(name = "paid_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date paidAt;
}
