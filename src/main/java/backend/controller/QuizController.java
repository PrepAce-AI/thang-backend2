package backend.controller;
import backend.entity.Quiz;
import backend.repository.QuizRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class QuizController {
    private final QuizRepository quizRepository;

    public QuizController(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    //Get All Quiz
    @GetMapping
    public ResponseEntity<List<Quiz>> getAllQuizzes(){
        List<Quiz> quizzes = quizRepository.findAll();
        return ResponseEntity.ok(quizzes);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Quiz>> getQuizzesByCourse(@PathVariable Integer courseId){
        List<Quiz> quizzes = quizRepository.findByCourse_CourseId(courseId);
        return ResponseEntity.ok(quizzes);
    }

    //Lay 1 quiz
    @GetMapping("/{quizId}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Integer quizId){
        Quiz quiz = quizRepository.findById(quizId).orElseThrow(() -> new RuntimeException("Không Tìm Thấy Quiz"));
        return ResponseEntity.ok(quiz);
    }
}
