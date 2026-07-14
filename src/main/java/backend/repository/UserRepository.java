package backend.repository;
import backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRoleName(String roleName);

    // 🔥 THÊM ĐÚNG METHOD NÀY ĐỂ ĐẾM HỌC SINH THEO TÊN ROLE
    long countByRoleName(String roleName);
}
