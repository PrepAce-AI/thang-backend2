package backend.service;

import backend.dto.request.QuestionRequest;
import backend.dto.response.QuestionResponse;
import backend.entity.*;
import backend.repository.*;
import backend.dto.request.AnswerRequest;
import backend.dto.response.AnswerResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AcademicQuestionService {

    @Autowired
    private AcademicQuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AcademicAnswerRepository answerRepository;

    @Autowired
    private ViolationReportRepository violationReportRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private NotificationService notificationService;

    // 1. Đăng câu hỏi mới
    @Transactional
    public QuestionResponse createQuestion(QuestionRequest request) {
        String currentUserEmail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        AcademicQuestion question = new AcademicQuestion();
        question.setContent(request.getContent());
        question.setTimestampSeconds(request.getTimestampSeconds());
        question.setUser(user);

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));
        question.setLesson(lesson);

        AcademicQuestion savedQuestion = questionRepository.save(question);
        return mapToResponse(savedQuestion);
    }

    // 2. Lấy danh sách câu hỏi theo bài học
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByLesson(Integer lessonId) {
        List<AcademicQuestion> questions = questionRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
        return questions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // 3. Xóa câu hỏi (Dành cho Admin)
    @Transactional
    public void deleteQuestion(Integer questionId) {
        questionRepository.deleteById(questionId);
    }

    // 4. Xóa câu trả lời (Dành cho Admin)
    @Transactional
    public void deleteAnswer(Integer answerId) {
        answerRepository.deleteById(answerId);
    }

    // 5. Lấy câu hỏi cho giáo viên
    @Transactional(readOnly = true)
    public List<backend.dto.response.TeacherQuestionResponse> getAllQuestionsForTeacher(Integer teacherId) {
        return questionRepository.findAllQuestionsWithDetails()
                .stream()
                .filter(q -> {
                    if (q.getLesson() == null || q.getLesson().getChapter() == null || q.getLesson().getChapter().getCourse() == null) {
                        return false;
                    }
                    Integer courseTeacherId = q.getLesson().getChapter().getCourse().getTeacherId();
                    boolean isMyCourse = (courseTeacherId != null && courseTeacherId.equals(teacherId));
                    boolean isNotMyOwnQuestion = (q.getUser().getId() != teacherId);
                    return isMyCourse && isNotMyOwnQuestion;
                })
                .map(this::mapToTeacherResponse)
                .collect(Collectors.toList());
    }

    // 6. Đăng câu trả lời & Tự động bắn thông báo
    @Transactional
    public AnswerResponse createAnswer(Integer questionId, AnswerRequest request) {
        String currentUserEmail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User replier = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        AcademicQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question không tồn tại"));

        AcademicAnswer answer = new AcademicAnswer();
        answer.setContent(request.getContent());
        answer.setUser(replier);
        answer.setQuestion(question);
        AcademicAnswer savedAnswer = answerRepository.save(answer);

        if (question.getUser() != null && !Objects.equals(question.getUser().getId(), replier.getId())) {
            String contentSummary = request.getContent().length() > 30
                    ? request.getContent().substring(0, 30) + "..."
                    : request.getContent();

            notificationService.sendReplyNotification(
                    question.getUser().getId(),
                    replier.getFullName(),
                    contentSummary,
                    replier.getId()
            );
        }

        return mapToAnswerResponse(savedAnswer);
    }

    // --- CÁC HÀM MAP DTO ---
    private AnswerResponse mapToAnswerResponse(AcademicAnswer answer) {
        AnswerResponse response = new AnswerResponse();
        response.setId(answer.getId());
        response.setContent(answer.getContent());
        response.setCreatedAt(answer.getCreatedAt());
        response.setUserFullName(answer.getUser().getFullName());
        response.setUserAvatarUrl(answer.getUser().getAvatarUrl());
        response.setUserRoleId(answer.getUser().getRoleId());
        return response;
    }

    private QuestionResponse mapToResponse(AcademicQuestion question) {
        QuestionResponse response = new QuestionResponse();
        response.setQuestionId(question.getId());
        response.setContent(question.getContent());
        response.setCreatedAt(question.getCreatedAt());
        response.setTimestampSeconds(question.getTimestampSeconds());
        response.setUserFullName(question.getUser().getFullName());
        response.setUserAvatarUrl(question.getUser().getAvatarUrl());
        response.setUserRoleId(question.getUser().getRoleId());

        List<AnswerResponse> answerResponses = answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId())
                .stream()
                .map(this::mapToAnswerResponse)
                .collect(Collectors.toList());
        response.setAnswers(answerResponses);

        return response;
    }

    private backend.dto.response.TeacherQuestionResponse mapToTeacherResponse(AcademicQuestion question) {
        backend.dto.response.TeacherQuestionResponse response = new backend.dto.response.TeacherQuestionResponse();
        response.setQuestionId(question.getId());
        response.setContent(question.getContent());
        response.setCreatedAt(question.getCreatedAt());
        response.setTimestampSeconds(question.getTimestampSeconds());
        response.setUserFullName(question.getUser().getFullName());
        response.setUserAvatarUrl(question.getUser().getAvatarUrl());
        response.setUserRoleId(question.getUser().getRoleId());

        if (question.getLesson() != null) {
            response.setLessonId(question.getLesson().getId());
            response.setLessonTitle(question.getLesson().getTitle());
            if (question.getLesson().getChapter() != null) {
                response.setChapterId(question.getLesson().getChapter().getId());
                response.setChapterTitle(question.getLesson().getChapter().getTitle());
                if (question.getLesson().getChapter().getCourse() != null) {
                    response.setCourseId(question.getLesson().getChapter().getCourse().getCourseId());
                    response.setCourseTitle(question.getLesson().getChapter().getCourse().getTitle());
                }
            }
        }

        List<AnswerResponse> answerResponses = answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId())
                .stream()
                .map(this::mapToAnswerResponse)
                .collect(Collectors.toList());
        response.setAnswers(answerResponses);

        return response;
    }

    public void reportAnswer(Integer answerId, String reason) {

        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        User user = userService.getByEmail(auth.getName());

        ViolationReport report = new ViolationReport();

        report.setReporterId(user.getId());

        AcademicAnswer answer = answerRepository
                .findById(answerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
        report.setReportedTarget(
                "Phản hồi của " +
                        answer.getUser().getFullName() +
                        ": " +
                        answer.getContent()
        );

        report.setReason(reason);

        report.setStatus("PENDING");

        report.setCreatedAt(new Date());

        violationReportRepository.save(report);
    }
}