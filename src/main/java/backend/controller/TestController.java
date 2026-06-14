package backend.controller;
import backend.dto.request.StartTestRequest;
import backend.dto.request.SubmitAnswerRequest;
import backend.dto.response.QuestionResponse;
import backend.dto.response.TestResultResponse;
import backend.dto.response.TestSessionResponse;
import backend.entity.TestSession;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.JwtService;
import backend.service.TestService;

import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tests")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class TestController {
    private final TestService testService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public TestController(TestService testService, JwtService jwtService, UserRepository userRepository) {
        this.testService = testService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<TestSessionResponse> startTest(
            @RequestBody StartTestRequest request,
            @RequestHeader("Authorization") String token,
            HttpServletRequest httpServletRequest)
    {
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        int userId = user.getId();

        String ip = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        TestSessionResponse response =
                testService.startTest(request, userId, ip, userAgent);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionsId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable int sessionsId, @RequestHeader("Authorization") String token){
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElseThrow();

        int userId = user.getId();
        return ResponseEntity.ok(testService.getQuestions(sessionsId, userId));
    }

    @PostMapping("/{sessionsId}/answer")
    public ResponseEntity<Void> submitAnswer(@PathVariable int sessionsId, @RequestBody SubmitAnswerRequest request, @RequestHeader("Authorization") String token){
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElseThrow();

        int userId = user.getId();

        testService.submitAnswer(sessionsId, request, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sessionsId}/submit")
    public ResponseEntity<String> submitTest(
            @PathVariable int sessionsId,
            @RequestHeader("Authorization") String token
    ){
        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email).orElseThrow();

        int userId = user.getId();

        testService.submitTest(sessionsId, userId);
        return ResponseEntity.ok("Nộp Bài Thành Công !!!");
    }

    @GetMapping("/{sessionsId}/result")
    public ResponseEntity<TestResultResponse> getResult(
            @PathVariable int sessionsId,
            @RequestHeader("Authorization") String token
    ) {

        String jwt = token.replace("Bearer ", "");
        String email = jwtService.extractUsername(jwt);

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        return ResponseEntity.ok(
                testService.getResult(sessionsId, user.getId())
        );
    }
}
