package com.demo.aiknowledge.controller;

import com.demo.aiknowledge.common.Result;
import com.demo.aiknowledge.dto.LoginRequest;
import com.demo.aiknowledge.dto.RegisterRequest;
import com.demo.aiknowledge.dto.UpdateUserRequest;
import com.demo.aiknowledge.entity.User;
import com.demo.aiknowledge.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestParam String phone) {
        authService.sendSmsCode(phone);
        return Result.success("Verification code sent");
    }

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request.getPhone(), request.getCode(), request.getPassword(), request.getUsername());
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest request) {
        User user = authService.login(request.getPhone(), request.getPassword());
        return Result.success(user);
    }

    @PostMapping("/update")
    public Result<User> updateUserInfo(@RequestBody UpdateUserRequest request) {
        User user = authService.updateUserInfo(request.getUserId(), request.getUsername(), request.getPassword());
        return Result.success(user);
    }
}
