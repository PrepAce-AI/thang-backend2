package backend.service;

import backend.dto.response.*;
import backend.entity.Course;
import backend.dto.*;
import backend.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private backend.repository.EnrollmentRepository enrollmentRepository;
    @Transactional(readOnly = true)
    public List<CourseListResponse> getAllCourses() {
        return courseRepository.findAll().stream().map(course -> {
            var dto = new backend.dto.response.CourseListResponse();
            dto.setId(course.getCourseId());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());

            // Hàm này sẽ nạp đường link ảnh từ bảng Courses (SQL) vào DTO,
            // nhờ có @JsonProperty ở bước 1, nó sẽ biến thành "thumbnail_url" cực chuẩn khi gửi qua React
            dto.setThumbnailUrl(course.getThumbnailUrl());

            dto.setPrice(course.getPrice());
            dto.setIsPublished(course.getIsPublished());
            dto.setStudents(enrollmentRepository.countByCourseId(course.getCourseId()));

// 🔥 ĐỒNG BỘ THÔNG TIN MÔN HỌC (SUBJECT)
            if (course.getSubject() != null) {
                dto.setSubjectId(course.getSubject().getId());
                dto.setSubjectName(course.getSubject().getSubjectName());
            } else {
                dto.setSubjectId(course.getSubjectId());
                dto.setSubjectName("Chung");
            }

            // 🔥 ĐỒNG BỘ THÔNG TIN GIÁO VIÊN (TEACHER)
            if (course.getTeacher() != null) {
                dto.setTeacherId(course.getTeacher().getId());
                dto.setTeacherName(course.getTeacher().getFullName());
            } else {
                dto.setTeacherId(course.getTeacherId());
                dto.setTeacherName("Giáo viên");
            }

            dto.setTeacherId(course.getTeacherId());

            if (course.getTeacherId() != null) {
                userRepository.findById(course.getTeacherId()).ifPresent(user -> {
                    dto.setTeacherName(user.getFullName());
                });
            }

            return dto;
        }).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetailById(Integer courseId) {
        // Tìm khóa học, nếu không thấy quăng lỗi (bạn có thể thay bằng Exception tự custom)
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));

        return mapToResponse(course);
    }

    @Transactional
    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(Integer courseId) {
        courseRepository.deleteById(courseId);
    }

    @Transactional
    public Course updateCourse(Integer courseId, java.util.Map<String, Object> updates) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));

        if (updates.containsKey("title")) {
            course.setTitle((String) updates.get("title"));
        }
        if (updates.containsKey("description")) {
            course.setDescription((String) updates.get("description"));
        }
        if (updates.containsKey("is_published")) {
            course.setIsPublished(Boolean.parseBoolean(String.valueOf(updates.get("is_published"))));
        }
        if (updates.containsKey("subjectId")) {
            try {
                course.setSubjectId(Integer.parseInt(String.valueOf(updates.get("subjectId"))));
            } catch (Exception e) {}
        }
        if (updates.containsKey("categoryId")) {
            try {
                course.setCategoryId(Integer.parseInt(String.valueOf(updates.get("categoryId"))));
            } catch (Exception e) {}
        }
        if (updates.containsKey("thumbnailUrl")) {
            course.setThumbnailUrl((String) updates.get("thumbnailUrl"));
        }
        if (updates.containsKey("thumbnail_url")) {
            course.setThumbnailUrl((String) updates.get("thumbnail_url"));
        }
        if (updates.containsKey("price")) {
            try {
                course.setPrice(new java.math.BigDecimal(String.valueOf(updates.get("price"))));
            } catch (Exception e) {}
        }
        return courseRepository.save(course);
    }

    @Autowired
    private backend.repository.UserRepository userRepository;

    // Hàm thực hiện chuyển đổi Entity -> DTO thủ công
    private CourseDetailResponse mapToResponse(Course course) {
        CourseDetailResponse response = new CourseDetailResponse();
        response.setId(course.getCourseId());
        response.setTitle(course.getTitle());
        response.setDescription(course.getDescription());
        response.setThumbnailUrl(course.getThumbnailUrl());
        response.setPrice(course.getPrice());
        response.setStudents(enrollmentRepository.countByCourseId(course.getCourseId()));
        response.setTeacherId(course.getTeacherId());

        if (course.getTeacherId() != null) {
            userRepository.findById(course.getTeacherId()).ifPresent(user -> {
                response.setTeacherName(user.getFullName());
            });
        }

        if (course.getSubject() != null) {
            response.setSubjectId(course.getSubject().getId());
            response.setSubjectName(course.getSubject().getSubjectName());
        } else {
            response.setSubjectId(course.getSubjectId());
            response.setSubjectName("Chung");
        }

        // Map danh sách Chapter
        response.setChapters(course.getChapters().stream().map(chapter -> {
            ChapterDto chapterDto = new ChapterDto();
            chapterDto.setId(chapter.getId());
            chapterDto.setTitle(chapter.getTitle());
            chapterDto.setOrder(chapter.getOrder());

            // Map danh sách Lesson bên trong Chapter
            chapterDto.setLessons(chapter.getLessons().stream().map(lesson -> {
                LessonDto lessonDto = new LessonDto();
                lessonDto.setId(lesson.getId());
                lessonDto.setTitle(lesson.getTitle());
                lessonDto.setDescription(lesson.getDescription());
                lessonDto.setVideoUrl(lesson.getVideoUrl());
                lessonDto.setDuration(lesson.getDuration());
                lessonDto.setOrder(lesson.getOrder());
                lessonDto.setIsPreview(lesson.getIsPreview() != null ? lesson.getIsPreview() : false);
// 🔥 ĐÃ THÊM: Bốc danh sách tài liệu thật từ database nạp vào DTO trả về cho React
                if (lesson.getMaterials() != null) {
                    lessonDto.setMaterials(lesson.getMaterials().stream().map(mat -> {
                        MaterialDto matDto = new MaterialDto();
                        matDto.setId(mat.getId());
                        matDto.setTitle(mat.getTitle());
                        matDto.setFileUrl(mat.getFileUrl());
                        return matDto;
                    }).collect(Collectors.toList()));
                } else {
                    lessonDto.setMaterials(new ArrayList<>());
                }
                return lessonDto;
            }).collect(Collectors.toList()));

            return chapterDto;
        }).collect(Collectors.toList()));

        return response;
    }

    public List<Course> getCoursesByTeacherId(Integer teacherId) {
        return courseRepository.findByTeacherId(teacherId);
    }
}