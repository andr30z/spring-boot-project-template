package com.project.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.project.template.service.impl.UserServiceImpl;
import com.project.template.utils.CookieUtil;
import com.project.template.utils.SecurityCipher;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTests {

  public static final String WANNABE_ACCESS_TOKEN = "WANNABE_ACCESS_TOKEN";

  private static final Long MILLIS_PER_DAY = 86400000L;

  @Captor
  private ArgumentCaptor<User> userArgumentCaptor;

  @Captor
  private ArgumentCaptor<Long> tokenDurationArgumentCaptor;

  @Captor
  private ArgumentCaptor<String> accessTokenValueArgumentCaptor;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TokenProvider tokenProvider;

  @Mock
  private CookieUtil cookieUtil;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication auth;

  private MockedStatic<SecurityCipher> securityCipherStaticMocked;

  private AutoCloseable autoCloseable;

  private UserServiceImpl underTest;

  private String userMail = "testemail@gmail.com";
  private String password = "123456";
  private String testName = "Test";
  private Long userId = 1L;
  private User currentUser = User
    .builder()
    .id(userId)
    .name(testName)
    .email(userMail)
    .password(password)
    .build();

  @BeforeEach
  void setUp() throws IOException {
    securityCipherStaticMocked = Mockito.mockStatic(SecurityCipher.class);
    autoCloseable = MockitoAnnotations.openMocks(this);

    underTest =
      new UserServiceImpl(
        userRepository,
        tokenProvider,
        cookieUtil,
        passwordEncoder
      );
  }

  @AfterEach
  void tearDown() throws Exception {
    SecurityContextHolder.clearContext();
    securityCipherStaticMocked.close();
    autoCloseable.close();
  }

  @Test
  @DisplayName("It should create an user.")
  void itShouldCreateAnUser()
    throws JsonProcessingException, InterruptedException {
    // given
    String userMail = "testemail@gmail.com";
    String password = "testepassword";
    String testName = "Test";
    Long userId = 1L;
    User createdUser = User
      .builder()
      .id(userId)
      .name(testName)
      .email(userMail)
      .password(password)
      .build();

    when(userRepository.save(any())).thenReturn(createdUser);

    underTest.create(
      new CreateUserDTO(
        createdUser.getEmail(),
        createdUser.getName(),
        createdUser.getPassword()
      )
    );

    verify(userRepository, times(1)).save(userArgumentCaptor.capture());

    // assert that the user created in this method is the same as to the one
    // captured by userArgumentCaptor
    assertThat(userArgumentCaptor.getValue().getEmail()).isEqualTo(userMail);
  }

  @Test
  @DisplayName("Will throw error when Email is already in use by another User.")
  void willThrowWhenEmailIsTaken() {
    // given
    String userMail = "testemail@gmail.com";
    String password = "testepassword";
    String testName = "Test";
    Long userId = 1L;
    User createdUser = User
      .builder()
      .id(userId)
      .name(testName)
      .email(userMail)
      .password(password)
      .build();

    BDDMockito
      .given(userRepository.findUserByEmail(Mockito.anyString()))
      .willReturn(Optional.of(createdUser));

    // when
    // then
    assertThatThrownBy(() ->
        underTest.create(
          new CreateUserDTO(
            createdUser.getEmail(),
            createdUser.getName(),
            createdUser.getPassword()
          )
        )
      )
      .isInstanceOf(EntityExistsException.class)
      .hasMessageContaining(
        "User with email: " + userMail + " already exists."
      );

    verify(userRepository, never()).save(Mockito.any());
  }

  @Test
  @DisplayName("It should login the user successfully.")
  void itShouldLoginUser() {
    // given
    LoginRequest loginRequest = new LoginRequest("teste@mail.com", "123456");
    String userMail = "testemail@gmail.com";
    String password = "testepassword";
    String testName = "Test";
    Long userId = 1L;
    User userToLogin = User
      .builder()
      .id(userId)
      .name(testName)
      .email(userMail)
      .password(password)
      .build();

    when(userRepository.findUserByEmail(anyString()))
      .thenReturn(Optional.of(userToLogin));
    var tokenMock = new Token(
      Token.TokenType.ACCESS,
      WANNABE_ACCESS_TOKEN,
      MILLIS_PER_DAY,
      null
    );
    when(tokenProvider.generateAccessToken(anyString())).thenReturn(tokenMock);
    when(tokenProvider.generateRefreshToken(anyString())).thenReturn(tokenMock);
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    when(cookieUtil.createAccessTokenCookie(anyString(), anyLong()))
      .thenReturn(
        ResponseCookie.from("accessToken", WANNABE_ACCESS_TOKEN).build()
      );

    when(cookieUtil.createRefreshTokenCookie(anyString(), anyLong()))
      .thenReturn(
        ResponseCookie.from("refreshToken", WANNABE_ACCESS_TOKEN).build()
      );

    var response = underTest.login(loginRequest, null, null);

    assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNotNull();
    var responseBody = response.getBody();
    assertThat(responseBody).isNotNull();
    assertThat(responseBody.getStatus())
      .isEqualTo(LoginResponse.SuccessFailure.SUCCESS);
  }

  @Test
  @DisplayName("Will throw error when login credentials are wrong.")
  void willThrowWhenLoginCredentialsMismatch() {
    LoginRequest loginRequest = new LoginRequest("teste@mail.com", "123456");
    String userMail = "testemail@gmail.com";
    String password = "testepassword";
    String testName = "Test";
    Long userId = 1L;
    User userToLogin = User
      .builder()
      .id(userId)
      .name(testName)
      .email(userMail)
      .password(password)
      .build();

    when(userRepository.findUserByEmail(anyString()))
      .thenReturn(Optional.of(userToLogin));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
    assertThatThrownBy(() -> underTest.login(loginRequest, null, null))
      .isInstanceOf(BadRequestException.class)
      .hasMessageContaining("Password doesn't match!");
    verify(tokenProvider, never()).generateAccessToken(Mockito.any());
    verify(tokenProvider, never()).generateRefreshToken(Mockito.any());
  }

  @Test
  @DisplayName("It should refresh user token.")
  void itShouldRefreshToken() {
    String generatedToken = "NEW_TOKEN_TEST";
    String emailFromToken = "testmail@yay.com";
    when(SecurityCipher.decrypt(anyString())).thenReturn(emailFromToken);
    when(tokenProvider.validateToken(anyString())).thenReturn(true);
    var tokenMock = new Token(
      Token.TokenType.ACCESS,
      generatedToken,
      MILLIS_PER_DAY,
      null
    );
    when(tokenProvider.getUsernameFromToken(anyString()))
      .thenReturn(emailFromToken);
    when(tokenProvider.generateAccessToken(anyString())).thenReturn(tokenMock);
    when(cookieUtil.createAccessTokenCookie(anyString(), anyLong()))
      .thenReturn(ResponseCookie.from("accessToken", generatedToken).build());

    // when
    var response = underTest.refresh(
      WANNABE_ACCESS_TOKEN,
      WANNABE_ACCESS_TOKEN
    );
    verify(cookieUtil, Mockito.times(1))
      .createAccessTokenCookie(
        accessTokenValueArgumentCaptor.capture(),
        tokenDurationArgumentCaptor.capture()
      );

    // assert that generated token is new
    assertThat(accessTokenValueArgumentCaptor.getValue())
      .isEqualTo(generatedToken);

    assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).isNotNull();
    var responseBody = response.getBody();
    assertThat(responseBody).isNotNull();
    assertThat(responseBody.getStatus())
      .isEqualTo(LoginResponse.SuccessFailure.SUCCESS);
  }

  @Test
  @DisplayName("It should return an user by his access token")
  void getTokenUser() {
    // given
    String userMail = "testemail@gmail.com";
    String password = "123456";
    String testName = "Test";
    Long userId = 1L;
    User user = User
      .builder()
      .id(userId)
      .name(testName)
      .email(userMail)
      .password(password)
      .build();
    when(SecurityCipher.decrypt(anyString(), anyBoolean())).thenReturn("");
    when(tokenProvider.validateToken(anyString())).thenReturn(true);

    when(tokenProvider.getUsernameFromToken(anyString())).thenReturn(userMail);
    when(userRepository.findUserByEmail(anyString()))
      .thenReturn(Optional.of(user));

    var userByToken = underTest.getTokenUser(WANNABE_ACCESS_TOKEN);

    assertThat(userByToken.getId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("It should return an user by Id.")
  void itShouldFindUserById() {
    String userMail = "testemail@gmail.com";
    String password = "123456";
    String testName = "Test";
    Long userId = 1L;
    User user = User
      .builder()
      .id(userId)
      .name(testName)
      .email(userMail)
      .password(password)
      .build();

    when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
    var userById = underTest.findById(user.getId());

    assertThat(userById.getId()).isEqualTo(user.getId());
  }

  @Test
  @DisplayName("Will throw error when user Id doesn't exist.")
  void willThrowWhenUserIdDontExist() {
    assertThatThrownBy(() -> underTest.findById(anyLong()))
      .isInstanceOf(ResourceNotFoundException.class)
      .hasMessageContaining("User not found.");
  }

  @Test
  @DisplayName("It should get the current logged user.")
  void itShouldGetTheCurrentLoggedUser() {
    mockAuthentication();

    User loggedUser = underTest.me();

    assertThat(loggedUser.getId()).isEqualTo(currentUser.getId());
  }

  private void mockAuthentication() {
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(auth.getPrincipal()).thenReturn(new CustomUserDetails(currentUser));
    SecurityContextHolder.setContext(securityContext);
  }
}
