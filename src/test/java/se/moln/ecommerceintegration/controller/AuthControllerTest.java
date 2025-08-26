package se.moln.ecommerceintegration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import se.moln.ecommerceintegration.dto.LoginRequest;
import se.moln.ecommerceintegration.dto.RegisterRequest;
import se.moln.ecommerceintegration.exception.GlobalExceptionHandler;
import se.moln.ecommerceintegration.model.User;
import se.moln.ecommerceintegration.repository.UserRepository;
import se.moln.ecommerceintegration.service.JwtService;
import se.moln.ecommerceintegration.service.UserService;

import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private UserService userService;
    private UserRepository userRepository;
    private JwtService jwtService;
    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        userRepository = Mockito.mock(UserRepository.class);
        jwtService = Mockito.mock(JwtService.class);

        AuthController controller = new AuthController(userService, userRepository, jwtService);
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_returns201_andToken() throws Exception {
        RegisterRequest req = new RegisterRequest("user@example.com", "Password123!", "David", "Andreasson");
        User saved = User.newUser(req.email(), "{bcrypt}", req.firstName(), req.lastName());
        when(userService.register(req.email(), req.password(), req.firstName(), req.lastName())).thenReturn(saved);
        when(jwtService.createAccessToken(saved.getId(), saved.getEmail(), saved.getRole())).thenReturn("token-123");

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", equalTo("token-123")))
                .andExpect(jsonPath("$.tokenType", equalTo("Bearer")));
    }

    @Test
    void register_whenEmailExists_returns409_errorPayload() throws Exception {
        RegisterRequest req = new RegisterRequest("user@example.com", "Password123!", "David", "Andreasson");
        when(userService.register(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("Email is already registered"));

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("conflict")));
    }

    @Test
    void register_validationError_returns400() throws Exception {
        // too short password (min 8 per DTO validation)
        RegisterRequest req = new RegisterRequest("user@example.com", "short", "D", "A");

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", equalTo("validation_failed")));
    }

    @Test
    void login_ok_returns200_withToken() throws Exception {
        String email = "user@example.com";
        String rawPassword = "Password123!";
        // controller skapar själv en ny BCryptPasswordEncoder() så vi använder en riktig hash här
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword);
        User user = User.newUser(email, hash, "David", "Andreasson");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("token-xyz");

        LoginRequest req = new LoginRequest(email, rawPassword);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", equalTo("token-xyz")))
                .andExpect(jsonPath("$.tokenType", equalTo("Bearer")));
    }

    @Test
    void login_unknownEmail_returns409_conflict() throws Exception {
        when(userRepository.findByEmail("nouser@example.com")).thenReturn(Optional.empty());
        LoginRequest req = new LoginRequest("nouser@example.com", "whatever");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("conflict")))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_wrongPassword_returns409_conflict() throws Exception {
        String email = "user@example.com";
        String wrongHash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("OtherPassword!");
        User user = User.newUser(email, wrongHash, "D", "A");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        LoginRequest req = new LoginRequest(email, "Password123!");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("conflict")));
    }
}