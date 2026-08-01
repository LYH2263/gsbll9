package com.ctf.controller;

import com.ctf.dto.ApiResponse;
import com.ctf.dto.statistics.StatisticsDTO;
import com.ctf.entity.Announcement;
import com.ctf.entity.Category;
import com.ctf.entity.Hint;
import com.ctf.entity.Question;
import com.ctf.entity.User;
import com.ctf.exception.EntityNotFoundException;
import com.ctf.mapper.UserMapper;
import com.ctf.service.AdminService;
import com.ctf.service.AnnouncementService;
import com.ctf.service.ContestService;
import com.ctf.service.HintService;
import com.ctf.util.JwtTokenUtil;
import com.ctf.util.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ContestService contestService;

    @Autowired
    private HintService hintService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private Integer getUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenUtil.getUserIdFromToken(token);
    }

    private String getRoleFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenUtil.getRoleFromToken(token);
    }

    private void checkAdminPermission(String token) {
        String role = getRoleFromToken(token);
        if (!"admin".equals(role)) {
            throw new SecurityException("Permission denied");
        }
    }

    // ========== User Management ==========
    @GetMapping("/users")
    public ApiResponse<List<User>> getAllUsers(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        List<User> users = userMapper.selectAll();
        return ApiResponse.success(users);
    }

    @GetMapping("/users/{id}")
    public ApiResponse<User> getUserById(@RequestHeader("Authorization") String token, @PathVariable Integer id) {
        checkAdminPermission(token);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }
        return ApiResponse.success(user);
    }

    @PostMapping("/users")
    public ApiResponse<String> addUser(@RequestHeader("Authorization") String token, @RequestBody User user) {
        checkAdminPermission(token);

        log.info("Received user data: studentId={}, username={}, passwordHash={}", 
                 user.getStudentId(), user.getUsername(), 
                 user.getPasswordHash() != null ? "***" : "null");

        if (user.getStudentId() == null || user.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (userMapper.selectByStudentId(user.getStudentId()) != null) {
            throw new IllegalArgumentException("Student ID already exists");
        }

        user.setPasswordHash(PasswordUtil.encodePassword(user.getPasswordHash()));
        user.setIsActive(true);
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("user");
        }

        userMapper.insert(user);
        log.info("User added successfully: studentId={}", user.getStudentId());
        return ApiResponse.success(null, "User added successfully");
    }

    @PutMapping("/users/{id}")
    public ApiResponse<String> updateUser(@RequestHeader("Authorization") String token, @PathVariable Integer id,
                                         @RequestBody User user) {
        checkAdminPermission(token);

        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new EntityNotFoundException("User not found");
        }

        user.setId(id);
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            user.setPasswordHash(PasswordUtil.encodePassword(user.getPasswordHash()));
        } else {
            user.setPasswordHash(existing.getPasswordHash());
        }

        userMapper.update(user);
        log.info("User updated: id={}", id);
        return ApiResponse.success(null, "User updated successfully");
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<String> deleteUser(@RequestHeader("Authorization") String token, @PathVariable Integer id) {
        checkAdminPermission(token);

        userMapper.delete(id);
        log.info("User deleted: id={}", id);
        return ApiResponse.success(null, "User deleted successfully");
    }

    // ========== Category Management ==========
    @GetMapping("/categories")
    public ApiResponse<List<Category>> getAllCategories(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        List<Category> categories = adminService.getAllCategories();
        return ApiResponse.success(categories);
    }

    @PostMapping("/categories")
    public ApiResponse<String> addCategory(@RequestHeader("Authorization") String token, @RequestBody Category category) {
        checkAdminPermission(token);

        category.setIsActive(true);
        adminService.addCategory(category);
        log.info("Category added: name={}", category.getName());
        return ApiResponse.success(null, "Category added successfully");
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<String> updateCategory(@RequestHeader("Authorization") String token, @PathVariable Integer id,
                                             @RequestBody Category category) {
        checkAdminPermission(token);

        category.setId(id);
        adminService.updateCategory(category);
        log.info("Category updated: id={}", id);
        return ApiResponse.success(null, "Category updated successfully");
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<String> deleteCategory(@RequestHeader("Authorization") String token, @PathVariable Integer id) {
        checkAdminPermission(token);

        adminService.deleteCategory(id);
        log.info("Category deleted: id={}", id);
        return ApiResponse.success(null, "Category deleted successfully");
    }

    // ========== Question Management ==========
    @GetMapping("/questions")
    public ApiResponse<List<Question>> getAllQuestions(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        List<Question> questions = adminService.getAllQuestions();
        return ApiResponse.success(questions);
    }

    @GetMapping("/categories/{categoryId}/questions")
    public ApiResponse<List<Question>> getQuestionsByCategory(@RequestHeader("Authorization") String token,
                                                              @PathVariable Integer categoryId) {
        checkAdminPermission(token);
        List<Question> questions = adminService.getQuestionsByCategory(categoryId);
        return ApiResponse.success(questions);
    }

    @PostMapping("/questions")
    public ApiResponse<String> addQuestion(@RequestHeader("Authorization") String token, @RequestBody Question question) {
        checkAdminPermission(token);

        question.setIsActive(true);
        adminService.addQuestion(question);
        log.info("Question added: title={}", question.getTitle());
        return ApiResponse.success(null, "Question added successfully");
    }

    @PutMapping("/questions/{id}")
    public ApiResponse<String> updateQuestion(@RequestHeader("Authorization") String token, @PathVariable Integer id,
                                             @RequestBody Question question) {
        checkAdminPermission(token);

        question.setId(id);
        adminService.updateQuestion(question);
        log.info("Question updated: id={}", id);
        return ApiResponse.success(null, "Question updated successfully");
    }

    @DeleteMapping("/questions/{id}")
    public ApiResponse<String> deleteQuestion(@RequestHeader("Authorization") String token, @PathVariable Integer id) {
        checkAdminPermission(token);

        adminService.deleteQuestion(id);
        log.info("Question deleted: id={}", id);
        return ApiResponse.success(null, "Question deleted successfully");
    }

    // ========== Statistics ==========
    @GetMapping("/statistics")
    public ApiResponse<StatisticsDTO> getStatistics(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);

        StatisticsDTO statistics = adminService.getStatistics();
        return ApiResponse.success(statistics);
    }

    // ========== Maintenance ==========
    @PostMapping("/cleanup-corrupted-records")
    public ApiResponse<Map<String, Object>> cleanupCorruptedRecords(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);

        log.info("Admin initiated corrupted records cleanup");
        int cleanedCount = contestService.cleanupCorruptedContestRecords();
        
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", cleanedCount);
        result.put("message", "Successfully cleaned " + cleanedCount + " corrupted records");
        
        log.info("Cleanup completed: {} records removed", cleanedCount);
        return ApiResponse.success(result);
    }

    // ========== Hint Management ==========
    @GetMapping("/questions/{questionId}/hints")
    public ApiResponse<List<Hint>> getHintsByQuestion(@RequestHeader("Authorization") String token,
                                                       @PathVariable Integer questionId) {
        checkAdminPermission(token);

        List<Hint> hints = hintService.getHintsByQuestionId(questionId);
        return ApiResponse.success(hints);
    }

    @PostMapping("/questions/{questionId}/hints")
    public ApiResponse<Hint> addHint(@RequestHeader("Authorization") String token,
                                      @PathVariable Integer questionId,
                                      @RequestBody Hint hint) {
        checkAdminPermission(token);

        hint.setQuestionId(questionId);
        Hint created = hintService.createHint(hint);
        log.info("Hint created: questionId={}, hintNumber={}", questionId, hint.getHintNumber());
        return ApiResponse.success(created, "Hint created successfully");
    }

    @PutMapping("/hints/{id}")
    public ApiResponse<Hint> updateHint(@RequestHeader("Authorization") String token,
                                         @PathVariable Integer id,
                                         @RequestBody Hint hint) {
        checkAdminPermission(token);

        Hint updated = hintService.updateHint(id, hint);
        log.info("Hint updated: id={}", id);
        return ApiResponse.success(updated, "Hint updated successfully");
    }

    @DeleteMapping("/hints/{id}")
    public ApiResponse<String> deleteHint(@RequestHeader("Authorization") String token,
                                           @PathVariable Integer id) {
        checkAdminPermission(token);

        hintService.deleteHint(id);
        log.info("Hint deleted: id={}", id);
        return ApiResponse.success(null, "Hint deleted successfully");
    }

    // ========== Announcement Management ==========
    @GetMapping("/announcements")
    public ApiResponse<List<Announcement>> getAllAnnouncements(@RequestHeader("Authorization") String token) {
        checkAdminPermission(token);
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        return ApiResponse.success(announcements);
    }

    @GetMapping("/announcements/{id}")
    public ApiResponse<Announcement> getAnnouncementById(@RequestHeader("Authorization") String token,
                                                          @PathVariable Integer id) {
        checkAdminPermission(token);
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            throw new EntityNotFoundException("Announcement not found");
        }
        return ApiResponse.success(announcement);
    }

    @PostMapping("/announcements")
    public ApiResponse<Announcement> createAnnouncement(@RequestHeader("Authorization") String token,
                                                         @RequestBody Announcement announcement) {
        checkAdminPermission(token);

        if (announcement.getContent() == null || announcement.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement content is required");
        }

        Announcement created = announcementService.createAnnouncement(announcement);
        return ApiResponse.success(created, "Announcement created successfully");
    }

    @PutMapping("/announcements/{id}")
    public ApiResponse<Announcement> updateAnnouncement(@RequestHeader("Authorization") String token,
                                                         @PathVariable Integer id,
                                                         @RequestBody Announcement announcement) {
        checkAdminPermission(token);

        Announcement updated = announcementService.updateAnnouncement(id, announcement);
        if (updated == null) {
            throw new EntityNotFoundException("Announcement not found");
        }
        return ApiResponse.success(updated, "Announcement updated successfully");
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<String> deleteAnnouncement(@RequestHeader("Authorization") String token,
                                                   @PathVariable Integer id) {
        checkAdminPermission(token);

        boolean deleted = announcementService.deleteAnnouncement(id);
        if (!deleted) {
            throw new EntityNotFoundException("Announcement not found");
        }
        return ApiResponse.success(null, "Announcement deleted successfully");
    }

    @PostMapping("/announcements/{id}/publish")
    public ApiResponse<Announcement> publishAnnouncement(@RequestHeader("Authorization") String token,
                                                          @PathVariable Integer id) {
        checkAdminPermission(token);

        Announcement published = announcementService.publishAnnouncement(id);
        if (published == null) {
            throw new EntityNotFoundException("Announcement not found");
        }
        log.info("Announcement published: id={}", id);
        return ApiResponse.success(published, "Announcement published successfully");
    }

    @PostMapping("/announcements/{id}/withdraw")
    public ApiResponse<String> withdrawAnnouncement(@RequestHeader("Authorization") String token,
                                                      @PathVariable Integer id) {
        checkAdminPermission(token);

        boolean withdrawn = announcementService.withdrawAnnouncement(id);
        if (!withdrawn) {
            throw new EntityNotFoundException("Announcement not found");
        }
        log.info("Announcement withdrawn: id={}", id);
        return ApiResponse.success(null, "Announcement withdrawn successfully");
    }
}
