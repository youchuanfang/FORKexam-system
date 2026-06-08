package com.exam.controller;

import com.exam.common.Result;
import com.exam.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 登录接口
     * POST /api/user/login
     * 参数：{ username, password, role }
     * 返回：{ code:200, data: { token, role } }
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String role = params.get("role");
        if (username == null || password == null || role == null) {
            return Result.error(400, "用户名、密码和角色不能为空");
        }
        Map<String, Object> data = userService.login(username, password, role);
        return Result.success(data);
    }

    /**
     * 注册接口
     * POST /api/user/register
     * 参数：{ username, password, role }
     * 返回：{ code:200, message: "success" }
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String role = params.get("role");
        if (username == null || password == null || role == null) {
            return Result.error(400, "用户名、密码和角色不能为空");
        }
        userService.register(username, password, role);
        return Result.success("注册成功");
    }
}
