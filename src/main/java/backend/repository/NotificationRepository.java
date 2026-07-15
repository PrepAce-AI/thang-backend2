package backend.repository;

import backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByTargetRoleOrTargetRole(String target1, String target2);

    List<Notification> findTop10ByOrderByCreatedAtDesc();

    // ✅ ĐÃ SỬA: Đưa về n.receiverId để khớp 100% với thuộc tính trong lớp Notification Entity của bạn
    @Query("SELECT n FROM Notification n WHERE " +
            "(n.targetRole = 'ALL' OR n.targetRole = :role) " +
            "AND (n.receiverId IS NULL OR n.receiverId = :userId) " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findMyNotifications(@Param("role") String role, @Param("userId") Integer userId);
}
