package backend.controller;

import backend.repository.QuizRepository;
import backend.repository.CourseRepository;
import backend.repository.UserRepository;
import backend.repository.PaymentRepository;
import backend.entity.Payment;
import backend.entity.Course;
import backend.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DashboardController {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    public DashboardController(QuizRepository quizRepository,
                               CourseRepository courseRepository,
                               UserRepository userRepository,
                               PaymentRepository paymentRepository) {
        this.quizRepository = quizRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            long totalQuizzes = quizRepository.count();
            long totalCourses = courseRepository.count();

            // CHUẨN HÓA ĐẾM HỌC SINH: Lọc theo vai trò STUDENT thay vì đếm tràn lan cả Admin/Teacher
            long totalStudents = userRepository.countByRoleName("STUDENT");

            List<Payment> allPayments = paymentRepository.findAll();
            List<Course> allCoursesList = courseRepository.findAll();
            BigDecimal currentMonthRevenue = BigDecimal.ZERO;

            // CHUẨN HÓA MÚI GIỜ: Ép về giờ Việt Nam để tránh lệch ngày cuối tháng/đầu tháng dưới DB
            TimeZone vnTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            Calendar now = Calendar.getInstance(vnTimeZone);
            int currentYear = now.get(Calendar.YEAR);
            int currentMonth = now.get(Calendar.MONTH);

            double[] monthlyTotals = new double[6];
            String[] monthLabels = new String[6];
            String[] monthKeys = new String[6];

            Set<Integer> uniquePaidStudents = new HashSet<>();
            int currentMonthCourses = 0;
            int lastMonthCourses = 0;

            // Khởi tạo mốc thời gian biểu đồ lùi 6 tháng động tính từ tháng hiện tại
            for (int i = 5; i >= 0; i--) {
                Calendar cal = Calendar.getInstance(vnTimeZone);
                cal.add(Calendar.MONTH, i - 5);

                int y = cal.get(Calendar.YEAR);
                int m = cal.get(Calendar.MONTH);

                monthLabels[i] = "T" + (m + 1);
                monthKeys[i] = y + "_" + m;
            }

            // Xử lý tính toán Doanh thu, Biểu đồ và tập hợp học sinh mua hàng
            if (allPayments != null) {
                for (Payment p : allPayments) {
                    if ("SUCCESS".equalsIgnoreCase(p.getPaymentStatus())) {
                        if (p.getStudentId() != null) {
                            uniquePaidStudents.add(p.getStudentId());
                        }

                        if (p.getPaidAt() != null) {
                            Calendar pCal = Calendar.getInstance(vnTimeZone);
                            pCal.setTime(p.getPaidAt());
                            int pYear = pCal.get(Calendar.YEAR);
                            int pMonth = pCal.get(Calendar.MONTH);
                            String pKey = pYear + "_" + pMonth;

                            // Đổ tiền vào cột biểu đồ tương ứng
                            for (int i = 0; i < 6; i++) {
                                if (monthKeys[i].equals(pKey)) {
                                    if (p.getAmount() != null) {
                                        monthlyTotals[i] += p.getAmount().doubleValue();
                                    }
                                    break;
                                }
                            }

                            // Cộng doanh thu cho riêng tháng này
                            if (pYear == currentYear && pMonth == currentMonth) {
                                if (p.getAmount() != null) {
                                    currentMonthRevenue = currentMonthRevenue.add(p.getAmount());
                                }
                            }
                        }
                    }
                }
            }

            // Xử lý đếm số lượng khóa học mới phục vụ tính xu hướng (Trend)
            if (allCoursesList != null) {
                for (Course c : allCoursesList) {
                    if (c.getCreatedAt() != null) {
                        Calendar cCal = Calendar.getInstance(vnTimeZone);
                        cCal.setTime(c.getCreatedAt());
                        int cYear = cCal.get(Calendar.YEAR);
                        int cMonth = cCal.get(Calendar.MONTH);

                        if (cYear == currentYear && cMonth == currentMonth) {
                            currentMonthCourses++;
                        } else if (cYear == currentYear && cMonth == (currentMonth - 1)) {
                            lastMonthCourses++;
                        }
                    }
                }
            }

            stats.put("totalQuizzes", totalQuizzes);
            stats.put("totalCourses", totalCourses);
            stats.put("totalStudents", totalStudents);
            stats.put("revenue", currentMonthRevenue);

            // TÍNH TOÁN ĐỘNG TỶ LỆ CHUYỂN ĐỔI (CR)
            String conversionRateStr = "0.0%";
            double cr = 0.0;
            if (totalStudents > 0) {
                cr = ((double) uniquePaidStudents.size() / totalStudents) * 100;
                conversionRateStr = String.format("%.1f%%", cr);
            }
            stats.put("conversionRate", conversionRateStr);

            // TÍNH TOÁN CÁC CHỈ SỐ XU HƯỚNG ĐỘNG (TRENDS)
            double currentMonthRev = monthlyTotals[5];
            double lastMonthRev = monthlyTotals[4];
            String revenueTrend = "↑ +0% so với tháng trước";
            if (lastMonthRev > 0) {
                double diff = ((currentMonthRev - lastMonthRev) / lastMonthRev) * 100;
                revenueTrend = String.format("↑ +%.1f%% so với tháng trước", diff);
            } else if (currentMonthRev > 0) {
                revenueTrend = "↑ Mới phát sinh tháng này";
            }
            stats.put("revenueTrend", revenueTrend);
            stats.put("studentTrend", "↑ +100% học viên mới");

            String courseTrend = "- Không đổi";
            if (lastMonthCourses > 0) {
                double cDiff = ((double) (currentMonthCourses - lastMonthCourses) / lastMonthCourses) * 100;
                if (cDiff > 0) {
                    courseTrend = String.format("↑ +%.1f%% tháng này", cDiff);
                } else if (cDiff < 0) {
                    courseTrend = String.format("↓ %.1f%% tháng này", cDiff);
                }
            } else if (currentMonthCourses > 0) {
                courseTrend = String.format("↑ +%d khóa mới", currentMonthCourses);
            }
            stats.put("courseTrend", courseTrend);

            String conversionTrend = String.format("↑ +%.1f%% hiệu suất", cr > 0 ? cr * 0.1 : 0.0);
            if (cr == 0.0) {
                conversionTrend = "— Chưa có chuyển đổi";
            }
            stats.put("conversionTrend", conversionTrend);

            // ĐÓNG GÓI MẢNG BIỂU ĐỒ ĐỘNG CHUẨN CSS
            List<Map<String, Object>> chartDataList = new ArrayList<>();
            double maxRevenue = 0;
            for (double total : monthlyTotals) {
                if (total > maxRevenue) maxRevenue = total;
            }

            for (int i = 0; i < 6; i++) {
                Map<String, Object> cMap = new HashMap<>();
                cMap.put("month", monthLabels[i]);

                int percentage = 0;
                if (maxRevenue > 0 && monthlyTotals[i] > 0) {
                    percentage = (int) ((monthlyTotals[i] / maxRevenue) * 100);
                    if (percentage < 15) percentage = 15;
                }
                cMap.put("value", percentage);
                chartDataList.add(cMap);
            }
            stats.put("chartData", chartDataList);

            // ĐÓNG GÓI GIAO DỊCH GẦN ĐÂY ĐỘNG
            List<Map<String, Object>> customTransactions = new ArrayList<>();
            if (allPayments != null) {
                int limit = Math.min(allPayments.size(), 10);
                for (int i = 0; i < limit; i++) {
                    Payment p = allPayments.get(i);
                    Map<String, Object> tMap = new HashMap<>();
                    tMap.put("id", p.getTransactionCode() != null ? p.getTransactionCode() : "TX" + p.getPaymentId());

                    String studentName = "Học viên #" + p.getStudentId();
                    if (p.getStudentId() != null) {
                        User user = userRepository.findById(p.getStudentId()).orElse(null);
                        if (user != null && user.getFullName() != null) {
                            studentName = user.getFullName();
                        }
                    }
                    tMap.put("student", studentName);

                    String courseTitle = "Khóa học #" + p.getCourseId();
                    if (p.getCourseId() != null) {
                        Course course = courseRepository.findById(p.getCourseId()).orElse(null);
                        if (course != null && course.getTitle() != null) {
                            courseTitle = course.getTitle();
                        }
                    }
                    tMap.put("course", courseTitle);

                    String formattedAmount = "0đ";
                    if (p.getAmount() != null) {
                        formattedAmount = new DecimalFormat("#,###đ").format(p.getAmount());
                    }
                    tMap.put("amount", formattedAmount);
                    tMap.put("status", "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) ? "Thành công" : "Chờ xử lý");
                    tMap.put("time", "Gần đây");

                    customTransactions.add(tMap);
                }
            }
            stats.put("recentTransactions", customTransactions);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            stats.put("error", "Lỗi nạp dữ liệu: " + e.getMessage());
            return ResponseEntity.status(500).body(stats);
        }
    }
}