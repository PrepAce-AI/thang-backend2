package backend.controller;

import backend.dto.InVideoQuestionDto;
import backend.entity.InVideoQuestion;
import backend.entity.Lesson;
import backend.repository.InVideoQuestionRepository;
import backend.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/outlines")
public class InVideoQuestionController {

    @Autowired
    private InVideoQuestionRepository inVideoQuestionRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @GetMapping("/lessons/{lessonId}/in-video-questions")
    public ResponseEntity<List<InVideoQuestionDto>> getQuestions(@PathVariable Integer lessonId) {
        List<InVideoQuestion> questions = inVideoQuestionRepository.findByLessonIdOrderByTimestampSecondsAsc(lessonId);
        List<InVideoQuestionDto> dtos = questions.stream().map(q -> {
            InVideoQuestionDto dto = new InVideoQuestionDto();
            dto.setId(q.getId());
            dto.setTimestampSeconds(q.getTimestampSeconds());
            dto.setQuestionText(q.getQuestionText());
            dto.setOptionA(q.getOptionA());
            dto.setOptionB(q.getOptionB());
            dto.setOptionC(q.getOptionC());
            dto.setOptionD(q.getOptionD());
            dto.setCorrectOption(q.getCorrectOption());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/lessons/{lessonId}/in-video-questions")
    public ResponseEntity<?> addQuestion(@PathVariable Integer lessonId, @RequestBody InVideoQuestionDto dto) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.badRequest().body("Lesson not found");
        }

        InVideoQuestion q = new InVideoQuestion();
        q.setLesson(lesson);
        q.setTimestampSeconds(dto.getTimestampSeconds());
        q.setQuestionText(dto.getQuestionText());
        q.setOptionA(dto.getOptionA());
        q.setOptionB(dto.getOptionB());
        q.setOptionC(dto.getOptionC());
        q.setOptionD(dto.getOptionD());
        q.setCorrectOption(dto.getCorrectOption());

        InVideoQuestion saved = inVideoQuestionRepository.save(q);
        dto.setId(saved.getId());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/in-video-questions/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Integer id) {
        if (!inVideoQuestionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        inVideoQuestionRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
