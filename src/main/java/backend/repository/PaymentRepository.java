package backend.repository;

import backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // Lịch sử thanh toán
    List<Payment> findByStudentIdOrderByCreatedAtDesc(Integer studentId);

    // Tìm theo mã giao dịch của hệ thống
    Optional<Payment> findByTransactionCode(String transactionCode);

    // Tìm theo nội dung chuyển khoản (SePay)
    Optional<Payment> findByTransactionCodeContaining(String transactionCode);

    // Tìm theo mã giao dịch ngân hàng
    Optional<Payment> findByBankTransactionId(String bankTransactionId);

    // Kiểm tra đã thanh toán thành công chưa
    @Query("""
            SELECT COUNT(p) > 0
            FROM Payment p
            WHERE p.studentId = :studentId
              AND p.courseId = :courseId
              AND p.paymentStatus = 'SUCCESS'
            """)
    boolean existsSuccessfulPayment(
            @Param("studentId") Integer studentId,
            @Param("courseId") Integer courseId
    );

    // Kiểm tra đã tạo payment PENDING chưa
    boolean existsByStudentIdAndCourseIdAndPaymentStatus(
            Integer studentId,
            Integer courseId,
            String paymentStatus
    );

    // Lấy payment mới nhất của khóa học
    Optional<Payment> findTopByStudentIdAndCourseIdOrderByCreatedAtDesc(
            Integer studentId,
            Integer courseId
    );

    List<Payment> findByPaymentStatus(String paymentStatus);
}
