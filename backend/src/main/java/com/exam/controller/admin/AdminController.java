package com.exam.controller.admin;

import com.exam.common.PageResult;
import com.exam.common.Result;
import com.exam.common.UserContext;
import com.exam.entity.User;
import com.exam.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final PaperRepository paperRepository;
    private final ExamRecordRepository examRecordRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           QuestionRepository questionRepository,
                           PaperRepository paperRepository,
                           ExamRecordRepository examRecordRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.paperRepository = paperRepository;
        this.examRecordRepository = examRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private void ensureAdmin() {
        if (!"admin".equals(UserContext.getRole())) {
            throw new RuntimeException("仅管理员可访问该接口");
        }
    }

    @GetMapping("/overview")
    public Result<Map<String, Long>> overview() {
        ensureAdmin();
        return Result.success(Map.of(
                "totalUsers", userRepository.count(),
                "totalQuestions", questionRepository.count(),
                "totalPapers", paperRepository.count(),
                "totalRecords", examRecordRepository.count()
        ));
    }

    @GetMapping("/users")
    public Result<?> getUsers(@RequestParam(required = false) String role,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        ensureAdmin();
        List<User> allUsers;
        if (role != null && !role.isBlank()) {
            allUsers = userRepository.findByRole(role);
        } else {
            allUsers = userRepository.findAll();
        }

        int total = allUsers.size();
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<User> pageContent;
        if (fromIndex >= total) {
            pageContent = List.of();
        } else {
            pageContent = allUsers.subList(fromIndex, toIndex);
        }

        List<Map<String, Object>> safeUsers = pageContent.stream().map(u -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("role", u.getRole());
            m.put("status", u.getStatus() != null ? u.getStatus() : "active");
            return m;
        }).collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) total / size);
        return Result.success(new PageResult<>(safeUsers, total, totalPages, page, size));
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUserRole(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        ensureAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String newRole = body.get("role");
        if (newRole == null || newRole.isBlank()) {
            throw new RuntimeException("角色不能为空");
        }
        if (!List.of("student", "teacher", "admin").contains(newRole)) {
            throw new RuntimeException("角色无效，可选值：student、teacher、admin");
        }
        user.setRole(newRole);
        userRepository.save(user);
        return Result.success();
    }

    @PutMapping("/users/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        ensureAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 3) {
            throw new RuntimeException("密码不能为空且不少于3位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Result.success();
    }

    @PutMapping("/users/{id}/toggle-status")
    public Result<?> toggleUserStatus(@PathVariable Integer id) {
        ensureAdmin();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String currentStatus = user.getStatus();
        boolean disabling = currentStatus == null || "active".equals(currentStatus);
        if (disabling && id.equals(UserContext.getUserId())) {
            throw new RuntimeException("不能禁用当前登录管理员账号");
        }
        user.setStatus(disabling ? "disabled" : "active");
        userRepository.save(user);
        return Result.success(Map.of("status", user.getStatus()));
    }
}
