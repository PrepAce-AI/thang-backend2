package backend.repository;

import backend.entity.StudySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudyScheduleRepository extends JpaRepository<StudySchedule, Integer> {

    // Lọc lịch học bằng hàm MONTH() và YEAR() có sẵn của SQL Server
    @Query(value = "SELECT * FROM StudySchedules WHERE MONTH(schedule_date) = :month AND YEAR(schedule_date) = :year", nativeQuery = true)
    List<StudySchedule> findByMonthAndYear(@Param("month") int month, @Param("year") int year);
}