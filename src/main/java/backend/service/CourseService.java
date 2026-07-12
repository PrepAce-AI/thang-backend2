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
        return courseRepository.save(course);
    }

    // Hàm thực hiện chuyển đổi Entity -> DTO thủ công
    private CourseDetailResponse mapToResponse(Course course) {
        CourseDetailResponse response = new CourseDetailResponse();
        response.setId(course.getCourseId());
        response.setTitle(course.getTitle());
        response.setDescription(course.getDescription());
        response.setThumbnailUrl(course.getThumbnailUrl());

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