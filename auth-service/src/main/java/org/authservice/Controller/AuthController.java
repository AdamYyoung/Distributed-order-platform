package org.authservice.Controller;

import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import org.authservice.DTO.AuthRequest;
import org.authservice.Service.AuthService;
import org.commonlib.Response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody AuthRequest authRequest){
        return ApiResponse.success(authService.register(
                authRequest.getUsername(),
                authRequest.getPassword(),
                authRequest.getInvitationCode()
        ));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody AuthRequest authRequest){
        String token = authService.login(authRequest.getUsername(), authRequest.getPassword());
        return ApiResponse.success(Map.of(
                "token", "Bearer "+ token,
                "type", "Bearer"
        ));
    }
}
