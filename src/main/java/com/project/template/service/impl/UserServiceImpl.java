package com.project.template.service.impl;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.template.dto.CreateUserDTO;
import com.project.template.dto.CustomUserDetails;
import com.project.template.dto.LoginRequest;
import com.project.template.dto.LoginResponse;
import com.project.template.dto.Token;
import com.project.template.exception.BadRequestException;
import com.project.template.exception.EntityExistsException;
import com.project.template.exception.ResourceNotFoundException;
import com.project.template.model.User;
import com.project.template.repository.UserRepository;
import com.project.template.service.TokenProvider;
import com.project.template.service.UserService;
import com.project.template.utils.CookieUtil;
import com.project.template.utils.SecurityCipher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  private static final int PASSWORD_STRENGTH = 10;

  private final UserRepository userRepository;

  private final TokenProvider tokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final CookieUtil cookieUtil;

  public UserServiceImpl(
      UserRepository userRepository,
      TokenProvider tokenProvider,
      CookieUtil cookieUtil,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.tokenProvider = tokenProvider;
    this.cookieUtil = cookieUtil;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public User create(CreateUserDTO userDTO) {
    Optional<User> userOptional = this.userRepository.findUserByEmail(userDTO.getEmail());
    if (userOptional.isPresent())
      throw new EntityExistsException(
          "User with email: " + userDTO.getEmail() + " already exists.");
    User user = new User();
    BeanUtils.copyProperties(userDTO, user);
    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(
        PASSWORD_STRENGTH,
        new SecureRandom());
    String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
    user.setPassword(encodedPassword);
    log.info("Creating user with email {}", userDTO.getEmail());
    return this.userRepository.save(user);
  }

  @Override
  public ResponseEntity<LoginResponse> login(
      LoginRequest loginRequest,
      String encryptedAccessToken,
      String encryptedRefreshToken) {
    String accessToken = SecurityCipher.decrypt(encryptedAccessToken);
    String refreshToken = SecurityCipher.decrypt(encryptedRefreshToken);
    String email = loginRequest.getEmail();
    User user = this.findByEmail(email);
    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
      throw new BadRequestException("Password doesn't match!");
    var accessTokenValid = tokenProvider.validateToken(accessToken);
    var refreshTokenValid = tokenProvider.validateToken(refreshToken);

    HttpHeaders responseHeaders = new HttpHeaders();
    Token newAccessToken;
    Token newRefreshToken;
    if (!accessTokenValid && !refreshTokenValid) {
      newAccessToken = tokenProvider.generateAccessToken(user.getEmail());
      newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail());
      addAccessTokenCookie(responseHeaders, newAccessToken);
      addRefreshTokenCookie(responseHeaders, newRefreshToken);
    }

    if (!accessTokenValid && refreshTokenValid) {
      newAccessToken = tokenProvider.generateAccessToken(user.getEmail());
      addAccessTokenCookie(responseHeaders, newAccessToken);
    }

    if (accessTokenValid && refreshTokenValid) {
      newAccessToken = tokenProvider.generateAccessToken(user.getEmail());
      newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail());
      addAccessTokenCookie(responseHeaders, newAccessToken);
      addRefreshTokenCookie(responseHeaders, newRefreshToken);
    }

    LoginResponse loginResponse = new LoginResponse(
        LoginResponse.SuccessFailure.SUCCESS,
        "Auth successful. Tokens are created in cookies.");
    return ResponseEntity.ok().headers(responseHeaders).body(loginResponse);
  }

  @Override
  public ResponseEntity<LoginResponse> refresh(
      String encryptedAccessToken,
      String encryptedRefreshToken) {
    String refreshToken = SecurityCipher.decrypt(encryptedRefreshToken);
    var refreshTokenValid = tokenProvider.validateToken(refreshToken);
    if (!refreshTokenValid)
      throw new BadRequestException(
          "Refresh Token is invalid!");

    String currentUserEmail = tokenProvider.getUsernameFromToken(refreshToken);
    Token newAccessToken = tokenProvider.generateAccessToken(currentUserEmail);
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add(
        HttpHeaders.SET_COOKIE,
        cookieUtil
            .createAccessTokenCookie(
                newAccessToken.getTokenValue(),
                newAccessToken.getDuration())
            .toString());

    LoginResponse loginResponse = new LoginResponse(
        LoginResponse.SuccessFailure.SUCCESS,
        "Auth successful. Tokens are created in cookies.");
    return ResponseEntity.ok().headers(responseHeaders).body(loginResponse);
  }

  @Override
  public User getTokenUser(String encryptedToken) {
    String token = SecurityCipher.decrypt(
        URLDecoder.decode(encryptedToken, StandardCharsets.UTF_8),
        true);

    boolean isTokenValid = this.tokenProvider.validateToken(token);
    if (!isTokenValid)
      throw new BadRequestException("Token invalid!");
    var tokenUsername = this.tokenProvider.getUsernameFromToken(token);
    return this.userRepository.findUserByEmail(tokenUsername)
        .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
  }

  @Override
  public User findById(Long id) {
    return this.userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
  }

  @Override
  public User me() {
    CustomUserDetails currentUser = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication()
        .getPrincipal();
    return currentUser.getUser();
  }

  private User findByEmail(String email) {
    return userRepository
        .findUserByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found" + " with email " + email));
  }

  private void addAccessTokenCookie(HttpHeaders httpHeaders, Token token) {
    httpHeaders.add(
        HttpHeaders.SET_COOKIE,
        cookieUtil
            .createAccessTokenCookie(token.getTokenValue(), token.getDuration())
            .toString());
  }

  private void addRefreshTokenCookie(HttpHeaders httpHeaders, Token token) {
    httpHeaders.add(
        HttpHeaders.SET_COOKIE,
        cookieUtil
            .createRefreshTokenCookie(token.getTokenValue(), token.getDuration())
            .toString());
  }

}
