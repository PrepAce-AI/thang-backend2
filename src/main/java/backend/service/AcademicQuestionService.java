package backend.service;

import backend.dto.request.QuestionRequest;
import backend.dto.response.QuestionResponse;
import backend.entity.AcademicQuestion;
import backend.entity.Lesson;
import backend.entity.User;
import backend.repository.AcademicQuestionRepository;
import backend.repository.UserRepository;
import backend.repository.CourseRepository;
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

    // 1. Logic Đăng câu hỏi mới
    @Transactional
    public QuestionResponse createQuestion(QuestionRequest request) {
        // Lấy email của User đang đăng nhập từ SecurityContext (do JwtAuthenticationFilter xử lý)
        String currentUserEmail = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        AcademicQuestion question = new AcademicQuestion();
        question.setContent(request.getContent());
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
        return questionRepository.findByLessonIdOrderByCreatedAtDesc(lessonId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Hàm phụ chuyển đổi sang DTO
    private QuestionResponse mapToResponse(AcademicQuestion question) {
        QuestionResponse response = new QuestionResponse();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setCreatedAt(question.getCreatedAt());
        response.setUserFullName(question.getUser().getFullName());
        response.setUserAvatarUrl(question.getUser().getAvatarUrl());
        return response;
    }
}