package backend.service;

import backend.config.YouTubeConfig;
import backend.entity.Chapter;
import backend.entity.Course;
import backend.entity.Lesson;
import backend.repository.ChapterRepository;
import backend.repository.CourseRepository;
import backend.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class YouTubeService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Transactional
    public void importPlaylist(Integer courseId, String playlistUrl, String chapterTitle) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        String playlistId = extractPlaylistId(playlistUrl);
        if (playlistId == null) {
            throw new RuntimeException("Invalid YouTube Playlist URL");
        }

        // 1. Create the new chapter
        List<Chapter> existingChapters = chapterRepository.findByCourseIdOrderByOrderAsc(courseId);
        int nextOrder = existingChapters.size() + 1;

        Chapter chapter = new Chapter();
        chapter.setTitle(chapterTitle == null || chapterTitle.trim().isEmpty() ? "Danh sách phát YouTube" : chapterTitle);
        chapter.setOrder(nextOrder);
        chapter.setCourse(course);
        chapter = chapterRepository.save(chapter);

        // 2. Fetch Playlist Items
        String apiUrl = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=" 
                + playlistId + "&key=" + YouTubeConfig.API_KEY;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("items")) return;

            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

            int lessonOrder = 1;
            List<String> videoIds = new ArrayList<>();
            List<Lesson> lessonsToSave = new ArrayList<>();

            for (Map<String, Object> item : items) {
                Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                Map<String, Object> resourceId = (Map<String, Object>) snippet.get("resourceId");
                String videoId = (String) resourceId.get("videoId");
                
                String title = (String) snippet.get("title");
                if ("Private video".equals(title) || "Deleted video".equals(title)) continue;

                videoIds.add(videoId);

                Lesson lesson = new Lesson();
                lesson.setTitle(title);
                lesson.setDescription((String) snippet.get("description"));
                lesson.setVideoUrl("https://www.youtube.com/watch?v=" + videoId);
                lesson.setOrder(lessonOrder++);
                lesson.setChapter(chapter);
                lessonsToSave.add(lesson);
            }

            // 3. Batch Fetch Durations
            if (!videoIds.isEmpty()) {
                String videoIdsParam = String.join(",", videoIds);
                String videoApiUrl = "https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id=" 
                        + videoIdsParam + "&key=" + YouTubeConfig.API_KEY;
                
                ResponseEntity<Map> videoResponse = restTemplate.getForEntity(videoApiUrl, Map.class);
                Map<String, Object> videoBody = videoResponse.getBody();
                if (videoBody != null && videoBody.containsKey("items")) {
                    List<Map<String, Object>> videoItems = (List<Map<String, Object>>) videoBody.get("items");
                    for (int i = 0; i < videoItems.size(); i++) {
                        if (i < lessonsToSave.size()) {
                            Map<String, Object> vItem = videoItems.get(i);
                            Map<String, Object> contentDetails = (Map<String, Object>) vItem.get("contentDetails");
                            String durationIso = (String) contentDetails.get("duration");
                            lessonsToSave.get(i).setDuration(parseIsoDuration(durationIso));
                        }
                    }
                }
            }

            // 4. Save all lessons
            lessonRepository.saveAll(lessonsToSave);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi kết nối với YouTube API: " + e.getMessage());
        }
    }

    private String extractPlaylistId(String url) {
        if (url == null) return null;
        String[] parts = url.split("list=");
        if (parts.length > 1) {
            String idPart = parts[1];
            int ampersandIndex = idPart.indexOf('&');
            if (ampersandIndex != -1) {
                return idPart.substring(0, ampersandIndex);
            }
            return idPart;
        }
        return null;
    }

    private String parseIsoDuration(String isoDuration) {
        if (isoDuration == null || !isoDuration.startsWith("PT")) return "00:00";
        isoDuration = isoDuration.substring(2);
        
        int hours = 0, minutes = 0, seconds = 0;
        
        int hIndex = isoDuration.indexOf('H');
        if (hIndex != -1) {
            hours = Integer.parseInt(isoDuration.substring(0, hIndex));
            isoDuration = isoDuration.substring(hIndex + 1);
        }
        
        int mIndex = isoDuration.indexOf('M');
        if (mIndex != -1) {
            minutes = Integer.parseInt(isoDuration.substring(0, mIndex));
            isoDuration = isoDuration.substring(mIndex + 1);
        }
        
        int sIndex = isoDuration.indexOf('S');
        if (sIndex != -1) {
            seconds = Integer.parseInt(isoDuration.substring(0, sIndex));
        }

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
