package backend.service;

import backend.dto.response.ChapterDto;
import backend.dto.response.CourseDetailResponse;
import backend.dto.response.LessonDto;
import backend.dto.response.MaterialDto;
import backend.entity.Chapter;
import backend.entity.Course;
import backend.entity.Lesson;
import backend.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetailById(Integer courseId) {
        // Tìm khóa học, nếu không thấy quăng lỗi (bạn có thể thay bằng Exception tự custom)
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));

        return mapToResponse(course);
    }

    // Hàm thực hiện chuyển đổi Entity -> DTO thủ công
    private CourseDetailResponse mapToResponse(Course course) {
        CourseDetailResponse response = new CourseDetailResponse();
        response.setId(course.getId());
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
}