package backend.controller;

import backend.dto.StudentNoteDto;
import backend.entity.Lesson;
import backend.entity.StudentNote;
import backend.entity.User;
import backend.repository.LessonRepository;
import backend.repository.StudentNoteRepository;
import backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class StudentNoteController {

    @Autowired
    private StudentNoteRepository studentNoteRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<List<StudentNoteDto>> getNotes(@PathVariable Integer lessonId) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(403).build();
        }

        List<StudentNote> notes = studentNoteRepository.findByLessonIdAndUserIdOrderByTimestampSecondsAsc(lessonId, user.getId());
        List<StudentNoteDto> dtos = notes.stream().map(n -> {
            StudentNoteDto dto = new StudentNoteDto();
            dto.setId(n.getId());
            dto.setContent(n.getContent());
            dto.setTimestampSeconds(n.getTimestampSeconds());
            dto.setCreatedAt(n.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/lessons/{lessonId}")
    public ResponseEntity<?> addNote(@PathVariable Integer lessonId, @RequestBody StudentNoteDto dto) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(403).build();
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return ResponseEntity.badRequest().body("Lesson not found");
        }

        StudentNote note = new StudentNote();
        note.setLesson(lesson);
        note.setUser(user);
        note.setContent(dto.getContent());
        note.setTimestampSeconds(dto.getTimestampSeconds());

        StudentNote saved = studentNoteRepository.save(note);
        dto.setId(saved.getId());
        dto.setCreatedAt(saved.getCreatedAt());

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<?> deleteNote(@PathVariable Integer noteId) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(403).build();
        }

        StudentNote note = studentNoteRepository.findById(noteId).orElse(null);
        if (note == null) {
            return ResponseEntity.notFound().build();
        }

        if (note.getUser().getId() != user.getId()) {
            return ResponseEntity.status(403).body("Not your note");
        }

        studentNoteRepository.deleteById(noteId);
        return ResponseEntity.ok().build();
    }
}
