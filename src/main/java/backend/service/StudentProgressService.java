package backend.service;

import backend.dto.request.StudentProgressRequest;
import backend.dto.response.CourseReportResponse;
import backend.dto.response.StudentProgressResponse;
import backend.entity.Chapter;
import backend.entity.Course;
import backend.entity.Lesson;
import backend.entity.StudentProgress;
import backend.entity.User;
import backend.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class StudentProgressService {

    @Autowired
    private StudentProgressRepository studentProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AIChapterSummaryRepository aiChapterSummaryRepository;

    @Autowired
    private AIChapterSummaryService aiChapterSummaryService;

    public StudentProgressResponse saveProgress(User user, StudentProgressRequest request) {

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        StudentProgress progress = studentProgressRepository
                .findByUserIdAndLessonId(user.getId(), lesson.getId())
                .orElse(new StudentProgress());

        progress.setUser(user);
        progress.setLesson(lesson);

        if (request.getIsCompleted() != null) {
            progress.setIsCompleted(request.getIsCompleted());
        }

        if (request.getScore() != null) {
            progress.setScore(request.getScore());
        }

        if (request.getLastVideoTime() != null) {
            progress.setLastVideoTime(request.getLastVideoTime());
        }

        StudentProgress saved = studentProgressRepository.save(progress);

        boolean chapterCompleted = false;

        if (Boolean.TRUE.equals(saved.getIsCompleted())) {

            checkChapterCompleted(user, lesson);

            chapterCompleted = aiChapterSummaryRepository
                    .findFirstByStudent_IdAndChapter_Id(
                            user.getId(),
                            lesson.getChapter().getId()
                    )
                    .isPresent();
        }

        return new StudentProgressResponse(
                saved.getId(),
                user.getId(),
                user.getFullName(),
                lesson.getId(),
                chapterCompleted,
                lesson.getChapter() != null
                        ? lesson.getChapter().getId()
                        : null,
                saved.getIsCompleted(),
                saved.getScore(),
                saved.getLastAccessed(),
                saved.getLastVideoTime()
        );
    }

    private void checkChapterCompleted(User user, Lesson lesson) {

        Chapter chapter = lesson.getChapter();

        if (chapter == null) {
            return;
        }

        List<Lesson> lessons = chapter.getLessons();

        if (lessons == null || lessons.isEmpty()) {
            log.warn("Chapter {} has no lessons.", chapter.getId());
            return;
        }

        // Kiểm tra toàn bộ lesson đã hoàn thành chưa
        for (Lesson l : lessons) {

            Optional<StudentProgress> progress =
                    studentProgressRepository.findByUserIdAndLessonId(
                            user.getId(),
                            l.getId()
                    );

            if (progress.isEmpty()
                    || !Boolean.TRUE.equals(progress.get().getIsCompleted())) {

                return;
            }
        }

        // Đã sinh summary rồi thì thôi
        if (aiChapterSummaryRepository
                .findFirstByStudent_IdAndChapter_Id(
                        user.getId(),
                        chapter.getId())
                .isPresent()) {

            return;
        }
        // Sinh AI Summary
        try {
            aiChapterSummaryService.generateSummary(
                    user.getId(),
                    chapter.getCourse().getCourseId(),
                    chapter.getId()
            );
            log.info(
                    "Chapter {} completed by user {} -> AI Summary generated.",
                    chapter.getId(),
                    user.getId()
            );
        } catch (Exception e) {
            log.error(
                    "Generate AI Summary failed. chapter={}, user={}",
                    chapter.getId(),
                    user.getId(),
                    e
            );
        }
    }

    public StudentProgressResponse getProgress(User user, int lessonId) {
        return studentProgressRepository
                .findByUserIdAndLessonId(user.getId(), lessonId)
                .map(p -> {
                    Lesson lesson = p.getLesson();
                    Chapter chapter = lesson.getChapter();
                    boolean chapterCompleted = false;
                    Integer chapterId = null;

                    if (chapter != null) {
                        chapterId = chapter.getId();
                        chapterCompleted = aiChapterSummaryRepository
                                .findFirstByStudent_IdAndChapter_Id(
                                        user.getId(),
                                        chapterId
                                )
                                .isPresent();
                    }
                    return new StudentProgressResponse(
                            p.getId(),
                            user.getId(),
                            user.getFullName(),
                            lessonId,
                            chapterCompleted,
                            chapterId,
                            p.getIsCompleted(),
                            p.getScore(),
                            p.getLastAccessed(),
                            p.getLastVideoTime()
                    );
                })
                .orElse(null);
    }

    public List<Integer> getCompletedLessonIds(User user, int courseId) {
        return studentProgressRepository.findCompletedLessonIdsByUserIdAndCourseId(user.getId(), courseId);
    }

    public List<CourseReportResponse> getCourseReport(int courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<Lesson> allLessons = new ArrayList<>();
        if (course.getChapters() != null) {
            for (Chapter chapter : course.getChapters()) {
                if (chapter.getLessons() != null) {
                    allLessons.addAll(chapter.getLessons());
                }
            }
        }

        int totalLessons = allLessons.size();
        if (totalLessons == 0) {
            return Collections.emptyList();
        }

        // Retrieve all progress records for lessons in this course
        Map<Integer, CourseReportResponse> reportMap = new HashMap<>();

        for (Lesson lesson : allLessons) {
            List<StudentProgress> progressList = studentProgressRepository.findByLessonId(lesson.getId());
            for (StudentProgress progress : progressList) {
                User student = progress.getUser();
                if (student.getRoleId() == 1 || student.getRoleId() == 2) {
                    continue; // Optional: Only track students
                }

                CourseReportResponse report = reportMap.computeIfAbsent(student.getId(),
                        k -> new CourseReportResponse(student.getId(), student.getFullName(), totalLessons, 0, 0.0, null));

                if (Boolean.TRUE.equals(progress.getIsCompleted())) {
                    report.setCompletedLessons(report.getCompletedLessons() + 1);
                }

                if (progress.getScore() != null) {
                    double currentAvg = report.getAverageScore() == null ? 0 : report.getAverageScore();
                    // Just simple sum for now, will calculate average later
                    report.setAverageScore(currentAvg + progress.getScore());
                }
            }
        }

        List<CourseReportResponse> reports = new ArrayList<>(reportMap.values());
        for (CourseReportResponse r : reports) {
            r.setProgressPercentage(Math.round(((double) r.getCompletedLessons() / totalLessons) * 100));
            if (r.getAverageScore() != null && r.getCompletedLessons() > 0) {
                // Average over completed lessons
                r.setAverageScore(Math.round((r.getAverageScore() / r.getCompletedLessons()) * 10.0) / 10.0);
            }
        }

        return reports;
    }
}
