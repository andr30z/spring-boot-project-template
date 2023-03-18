package com.project.template.service;

import org.springframework.http.ResponseEntity;

import com.project.template.dto.CreateUserDTO;
import com.project.template.dto.LoginRequest;
import com.project.template.dto.LoginResponse;
import com.project.template.model.User;




public interface UserService {

    User create(CreateUserDTO userDTO);

    User findById(Long id);

    User me();

    ResponseEntity<LoginResponse> login(LoginRequest loginRequest, String accessToken, String refreshToken);

    ResponseEntity<LoginResponse> refresh(String accessToken, String refreshToken);

    User getTokenUser(String token);
}
