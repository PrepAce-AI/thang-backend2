package backend.controller;

import backend.dto.response.CourseResponse;
import backend.entity.Course;
import backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {
    @Autowired
    private CourseService courseService;

    @GetMapping
    public List<CourseResponse> getAll(){
        return courseService.getAll();
    }



    @PatchMapping("/{id}/status")
    public Course updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body){
        return courseService.updateStatus(id, body.get("status"));
    }
}
