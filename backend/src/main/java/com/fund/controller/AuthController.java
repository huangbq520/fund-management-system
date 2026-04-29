package com.fund.controller;

import com.fund.dto.LoginRequest;
import com.fund.dto.RegisterRequest;
import com.fund.dto.SendVerifyCodeRequest;
import com.fund.entity.User;
import com.fund.service.AuthService;
import com.fund.util.JwtUtil;
import com.fund.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/send-verify-code")
    public ApiResponse<Void> sendVerifyCode(@RequestBody SendVerifyCodeRequest request) {
        authService.sendVerifyCode(request.getEmail());
        return ApiResponse.success();
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return ApiResponse.error("密码长度不能少于6位");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ApiResponse.error("两次密码输入不一致");
        }

        User user = authService.register(
            request.getEmail(),
            request.getPassword(),
            request.getNickname(),
            request.getVerifyCode()
        );

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("token", token);

        return ApiResponse.success(data);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        User user = authService.login(request.getEmail(), request.getPassword());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("token", token);

        return ApiResponse.success(data);
    }

    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResponse.error("未登录");
        }
        User user = authService.getUserById(userId);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        return ApiResponse.success(user);
    }
}
