package backend.repository;
import backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer>{
    List<Notification> findByTargetRoleOrTargetRole(String target1, String target2);

    List<Notification> findTop10ByOrderByCreatedAtDesc();
}
