package backend.controller;

import backend.dto.request.StudentProgressRequest;
import backend.dto.response.CourseReportResponse;
import backend.dto.response.StudentProgressResponse;
import backend.entity.User;
import backend.service.StudentProgressService;
import backend.service.UserService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private StudentProgressService studentProgressService;

    @Autowired
    private UserService userService;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userService.getByEmail(auth.getName());
    }

    // Học sinh cập nhật tiến độ
    @PostMapping("/progress")
    public ResponseEntity<?> saveProgress(@RequestBody StudentProgressRequest request) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(403).body("Vui lòng đăng nhập");
        }
        StudentProgressResponse response = studentProgressService.saveProgress(user, request);
        return ResponseEntity.ok(response);
    }

    // Lấy tiến độ của học sinh hiện tại cho 1 bài học
    @GetMapping("/progress/lesson/{lessonId}")
    public ResponseEntity<?> getProgress(@PathVariable Integer lessonId) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(403).body("Vui lòng đăng nhập");
        }
        StudentProgressResponse response = studentProgressService.getProgress(user, lessonId);
        if (response == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(response);
    }

    // Lấy danh sách ID các bài học đã hoàn thành của học sinh
    @GetMapping("/progress/course/{courseId}/completed")
    public ResponseEntity<?> getCompletedLessonIds(@PathVariable Integer courseId) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(403).body("Vui lòng đăng nhập");
        }
        List<Integer> completedLessonIds = studentProgressService.getCompletedLessonIds(user, courseId);
        return ResponseEntity.ok(completedLessonIds);
    }

    // Giáo viên xem báo cáo tiến độ của cả lớp
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<?> getCourseReport(@PathVariable Integer courseId) {
        User user = getAuthenticatedUser();
        if (user == null || (user.getRoleId() != 2 && user.getRoleId() != 1)) {
            return ResponseEntity.status(403).body("Không có quyền truy cập");
        }
        List<CourseReportResponse> reports = studentProgressService.getCourseReport(courseId);
        return ResponseEntity.ok(reports);
    }

    // Giáo viên xuất Excel
    @GetMapping("/courses/{courseId}/export")
    public ResponseEntity<byte[]> exportCourseReportToExcel(@PathVariable Integer courseId) {
        User user = getAuthenticatedUser();
        if (user == null || (user.getRoleId() != 2 && user.getRoleId() != 1)) {
            return ResponseEntity.status(403).build();
        }

        List<CourseReportResponse> reports = studentProgressService.getCourseReport(courseId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bao Cao Tien Do");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID Học Sinh", "Họ và Tên", "Tổng Bài Học", "Đã Hoàn Thành", "Tiến Độ (%)", "Điểm Trung Bình"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                
                // Bold style
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Data rows
            int rowIdx = 1;
            for (CourseReportResponse report : reports) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(report.getUserId());
                row.createCell(1).setCellValue(report.getUserFullName() != null ? report.getUserFullName() : "");
                row.createCell(2).setCellValue(report.getTotalLessons());
                row.createCell(3).setCellValue(report.getCompletedLessons());
                row.createCell(4).setCellValue(report.getProgressPercentage() + "%");
                
                if (report.getAverageScore() != null) {
                    row.createCell(5).setCellValue(report.getAverageScore());
                } else {
                    row.createCell(5).setCellValue("N/A");
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            
            byte[] excelBytes = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "Bao_Cao_Tien_Do_Khoa_Hoc_" + courseId + ".xlsx");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
