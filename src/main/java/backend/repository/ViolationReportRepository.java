package backend.repository;

import backend.entity.ViolationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ViolationReportRepository extends JpaRepository<ViolationReport, Integer> {
    // Hàm đếm số báo cáo đang chờ xử lý phục vụ cho thống kê Dashboard
    long countByStatus(String status);
}