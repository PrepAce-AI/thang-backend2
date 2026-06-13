package backend.controller;
import backend.dto.request.StartTestRequest;
import backend.dto.request.SubmitAnswerRequest;
import backend.dto.response.QuestionResponse;
import backend.dto.response.TestSessionResponse;
import backend.entity.TestSession;
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

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @PostMapping("/start")
    public ResponseEntity<TestSessionResponse> startTest(@RequestBody StartTestRequest request,
                                                         @RequestAttribute("userId") int userId, //Lay tu JWT Filter
                                                         HttpServletRequest httpServletRequest){
        String ip = httpServletRequest.getRemoteAddr();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        TestSessionResponse response = testService.startTest(request, userId, ip, userAgent);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionsId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(@PathVariable int sessionsId, @RequestAttribute("userId") int userId){
        return ResponseEntity.ok(testService.getQuestions(sessionsId, userId));
    }

    @PostMapping("/{sessionsId}/answer")
    public ResponseEntity<Void> submitAnswer(@PathVariable int sessionsId, @RequestBody SubmitAnswerRequest request, @RequestAttribute("userId") int userId){
        testService.submitAnswer(sessionsId, request, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sessionsId}/submit")
    public ResponseEntity<String> submitTest(
            @PathVariable int sessionsId,
            @RequestAttribute("userId") int userId
    ){
        testService.submitTest(sessionsId, userId);
        return ResponseEntity.ok("Nộp Bài Thành Công !!!");
    }
}
