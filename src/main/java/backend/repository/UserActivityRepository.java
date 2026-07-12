package backend.repository;


import backend.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Integer> {

    // Tìm lịch sử hoạt động của 1 user và sắp xếp giảm dần theo thời gian
    List<UserActivity> findByUserIdOrderByTimestampDesc(Integer userId);
}