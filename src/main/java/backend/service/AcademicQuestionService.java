package backend.service;

import backend.dto.request.QuestionRequest;
import backend.dto.response.QuestionResponse;
import backend.entity.AcademicQuestion;
import backend.entity.Lesson;
import backend.entity.User;
import backend.repository.AcademicQuestionRepository;
import backend.repository.UserRepository;
import backend.repository.CourseRepository;
import backend.repository.AcademicAnswerRepository;
import backend.dto.request.AnswerRequest;
import backend.dto.response.AnswerResponse;
import backend.entity.AcademicAnswer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AcademicQuestionService {

    @Autowired
    private AcademicQuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AcademicAnswerRepository answerRepository;

    // 1. Logic Đăng câu hỏi mới
    @Transactional
    public QuestionResponse createQuestion(QuestionRequest request) {
        // Lấy email của User đang đăng nhập từ SecurityContext (do JwtAuthenticationFilter xử lý)
        String currentUserEmail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        AcademicQuestion question = new AcademicQuestion();
        question.setContent(request.getContent());
        question.setTimestampSeconds(request.getTimestampSeconds());
        question.setUser(user);

        // Thiết lập liên kết tạm thời cho Lesson qua ID
        Lesson lesson = new Lesson();
        lesson.setId(request.getLessonId());
        question.setLesson(lesson);

        AcademicQuestion savedQuestion = questionRepository.save(question);
        return mapToResponse(savedQuestion);
    }

    // 2. Logic Lấy danh sách câu hỏi theo bài học
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByLesson(Integer lessonId) {
        List<AcademicQuestion> questions = questionRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
        return questions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<AcademicQuestion> getAllRawQuestions() {
        return questionRepository.findAll();
    }

    // 2.5 Logic Lấy danh sách câu hỏi theo giáo viên (Centralized Q&A)
    @Transactional(readOnly = true)
    public List<backend.dto.response.TeacherQuestionResponse> getAllQuestionsForTeacher(Integer teacherId) {
        return questionRepository.findAllQuestionsWithDetails()
                .stream()
                .filter(q -> {
                    // Loại bỏ các câu hỏi mà lesson, chapter, course bị null (nếu có)
                    if (q.getLesson() == null || q.getLesson().getChapter() == null || q.getLesson().getChapter().getCourse() == null) {
                        return false;
                    }
                    Integer courseTeacherId = q.getLesson().getChapter().getCourse().getTeacherId();
                    
                    // Do hệ thống hiện tại Frontend đang hiển thị TẤT CẢ các khóa học cho Giáo viên
                    // (chức năng phân quyền khóa học chưa bật), nên Q&A cũng sẽ tạm thời hiển thị
                    // câu hỏi từ TẤT CẢ các khóa học để giáo viên có thể nhìn thấy.
                    boolean isMyCourse = true; 
                    
                    // 2. Loại trừ những câu hỏi do chính giáo viên này tự hỏi (để test)
                    boolean isNotMyOwnQuestion = (q.getUser().getId() != teacherId);
                    
                    return isMyCourse && isNotMyOwnQuestion;
                })
                .map(this::mapToTeacherResponse)
                .collect(Collectors.toList());
    }

    // 3. Logic Đăng câu trả lời
    @Transactional
    public AnswerResponse createAnswer(Integer questionId, AnswerRequest request) {
        String currentUserEmail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        AcademicQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question không tồn tại"));

        AcademicAnswer answer = new AcademicAnswer();
        answer.setContent(request.getContent());
        answer.setUser(user);
        answer.setQuestion(question);

        AcademicAnswer savedAnswer = answerRepository.save(answer);
        return mapToAnswerResponse(savedAnswer);
    }

    // Hàm phụ chuyển đổi sang DTO cho Answer
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

    // Hàm phụ chuyển đổi sang DTO cho Question
    private QuestionResponse mapToResponse(AcademicQuestion question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
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

    // Hàm phụ chuyển đổi sang Teacher DTO
    private backend.dto.response.TeacherQuestionResponse mapToTeacherResponse(AcademicQuestion question) {
        backend.dto.response.TeacherQuestionResponse response = new backend.dto.response.TeacherQuestionResponse();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setCreatedAt(question.getCreatedAt());
        response.setTimestampSeconds(question.getTimestampSeconds());
        response.setUserFullName(question.getUser().getFullName());
        response.setUserAvatarUrl(question.getUser().getAvatarUrl());
        response.setUserRoleId(question.getUser().getRoleId());
        
        // Lấy thông tin bài học, chương, khóa học
        if (question.getLesson() != null) {
            response.setLessonId(question.getLesson().getId());
            response.setLessonTitle(question.getLesson().getTitle());
            if (question.getLesson().getChapter() != null) {
                response.setChapterId(question.getLesson().getChapter().getId());
                response.setChapterTitle(question.getLesson().getChapter().getTitle());
                if (question.getLesson().getChapter().getCourse() != null) {
                    response.setCourseId(question.getLesson().getChapter().getCourse().getId());
                    response.setCourseTitle(question.getLesson().getChapter().getCourse().getTitle());
                }
            }
        }

        // Load danh sách answers
        List<AnswerResponse> answerResponses = answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId())
                .stream()
                .map(this::mapToAnswerResponse)
                .collect(Collectors.toList());
        response.setAnswers(answerResponses);

        return response;
    }
}