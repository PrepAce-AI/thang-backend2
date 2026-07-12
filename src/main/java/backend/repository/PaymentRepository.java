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

    List<Payment> findByStudentIdOrderByPaidAtDesc(Integer studentId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    /** Kiểm tra student đã thanh toán thành công cho course chưa */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.studentId = :studentId AND p.courseId = :courseId AND p.paymentStatus = 'SUCCESS'")
    boolean existsSuccessfulPayment(@Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

    Optional<Payment> findByTransactionCodeContaining(String transactionCode);
}
