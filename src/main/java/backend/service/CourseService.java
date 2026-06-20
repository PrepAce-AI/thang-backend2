package backend.service;

import backend.dto.response.CourseResponse;
import backend.entity.Course;
import backend.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepo;

    public List<CourseResponse> getAll(){
        return courseRepo.findAll().stream().map(c -> {
            CourseResponse res = new CourseResponse();

            res.setId(c.getCourseId());
            res.setTitle(c.getTitle());
            res.setTeacherId(c.getTeacherId());
            res.setPrice(c.getPrice().doubleValue());

            //Map is_published --> status
            res.setStatus(Boolean.TRUE.equals(c.getIsPublished()) ? "PUBLISHED" : "PENDING");

            //Tam thoi chua join users
            res.setTeacherName("Teacher #" + c.getTeacherId());

            return res;
        }).collect(Collectors.toList());
    }

    public Course updateStatus(Integer id, String status){
        Course c = courseRepo.findById(id).orElseThrow(() -> new RuntimeException("Course Not Found"));
        c.setIsPublished("PUBLISHED".equals(status));

        return courseRepo.save(c);
    }

    //GET ALL IN PUBLIC WEBSITE
    public List<CourseResponse> getPublishedCourse(){
        return courseRepo.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                .map(c -> {
                    CourseResponse cRes = new CourseResponse();
                    cRes.setId(c.getCourseId());
                    cRes.setTitle(c.getTitle());
                    cRes.setPrice(c.getPrice().doubleValue());
                    cRes.setTeacherName("Teacher #"+c.getTeacherId());
                    cRes.setStatus("PUBLISHED");
                    return cRes;
                }).toList();
    }
}
