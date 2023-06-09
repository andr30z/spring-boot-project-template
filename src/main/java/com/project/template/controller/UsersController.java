package com.project.template.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.template.dto.CreateUserDTO;
import com.project.template.dto.LoginRequest;
import com.project.template.dto.LoginResponse;
import com.project.template.model.User;
import com.project.template.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

  private final UserService usersService;

  public UsersController(UserService usersService) {
    this.usersService = usersService;
  }

  @PostMapping
  public User create(@RequestBody @Validated CreateUserDTO createUserDTO) {
    return this.usersService.create(createUserDTO);
  }

  @PostMapping(
    value = "/login",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<LoginResponse> login(
    @CookieValue(name = "accessToken", required = false) String accessToken,
    @CookieValue(name = "refreshToken", required = false) String refreshToken,
    @RequestBody @Validated LoginRequest loginRequest
  ) {
    return usersService.login(loginRequest, accessToken, refreshToken);
  }

  @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LoginResponse> refreshToken(
    @CookieValue(name = "accessToken", required = false) String accessToken,
    @CookieValue(name = "refreshToken", required = false) String refreshToken
  ) {
    return usersService.refresh(accessToken, refreshToken);
  }

  @GetMapping("/me")
  public User me() {
    return this.usersService.me();
  }

  @GetMapping("/{id}")
  public User findById(@PathVariable Long id) {
    return usersService.findById(id);
  }

  @GetMapping("/validate-token")
  public User validateToken(@RequestParam String token) {
    return this.usersService.getTokenUser(token);
  }
}
