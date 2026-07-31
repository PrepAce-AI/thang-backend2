package backend.repository;

import backend.entity.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {


    // ==============================
    // STUDENT PAYMENT
    // ==============================

    // Lịch sử thanh toán
    List<Payment> findByStudentIdOrderByCreatedAtDesc(Integer studentId);


    // Tìm theo mã giao dịch hệ thống
    Optional<Payment> findByTransactionCode(String transactionCode);


    // Tìm theo nội dung chuyển khoản
    Optional<Payment> findByTransactionCodeContaining(String transactionCode);


    // Tìm theo mã giao dịch ngân hàng
    Optional<Payment> findByBankTransactionId(String bankTransactionId);



    // ==============================
    // CHECK PAYMENT
    // ==============================


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


    // Kiểm tra payment đang pending
    boolean existsByStudentIdAndCourseIdAndPaymentStatus(
            Integer studentId,
            Integer courseId,
            String paymentStatus
    );


    // Payment mới nhất của khóa học
    Optional<Payment> findTopByStudentIdAndCourseIdOrderByCreatedAtDesc(
            Integer studentId,
            Integer courseId
    );


    // Admin lấy danh sách pending
    List<Payment> findByPaymentStatus(String paymentStatus);



    // ===================================================
    // ADMIN DASHBOARD STATISTICS - THÊM MỚI
    // ===================================================


    /**
     * Tổng doanh thu
     * Chỉ tính payment thành công
     */
    // Tổng doanh thu
    @Query("""
    SELECT COALESCE(SUM(p.amount),0)
    FROM Payment p
    WHERE p.paymentStatus = 'SUCCESS'
""")
    BigDecimal getTotalRevenue();


    // Tổng số giao dịch thành công
    @Query("""
    SELECT COUNT(p)
    FROM Payment p
    WHERE p.paymentStatus = 'SUCCESS'
""")
    Long countSuccessfulPayments();


    // Danh sách mua gần đây
    List<Payment> findTop10ByPaymentStatusOrderByPaidAtDesc(String status);



    /**
     * Tổng số học sinh đã mua khóa học
     * DISTINCT tránh 1 người mua nhiều khóa bị tính nhiều lần
     */
    @Query("""
        SELECT COUNT(DISTINCT p.studentId)
        FROM Payment p
        WHERE p.paymentStatus = 'SUCCESS'
    """)
    Integer countStudents();



    /**
     * Lấy 5 giao dịch gần nhất
     */
    List<Payment> findByPaymentStatusOrderByPaidAtDesc(
            String paymentStatus,
            Pageable pageable
    );

}