package com.myblog.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myblog.config.SecurityConfig;
import com.myblog.entity.User;
import com.myblog.service.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

@WebMvcTest(
        controllers = SecurityRoutesTest.Endpoints.class,
        properties = "cors.allowed-origins=http://localhost")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    IpBlacklistFilter.class,
    SecurityRoutesTest.Endpoints.class
})
class SecurityRoutesTest {
    @Autowired MockMvc mvc;
    @MockBean JwtUtils jwt;
    @MockBean JwtBlacklistService blacklist;
    @MockBean UserService users;
    @MockBean IpBlacklistService ips;

    @RestController
    static class Endpoints {
        @GetMapping({"/api/articles", "/api/tags", "/api/auth/me", "/api/tags/followed"})
        String get() {
            return "ok";
        }

        @PostMapping("/api/auth/login")
        String login() {
            return "ok";
        }

        @GetMapping("/api/articles/not-found")
        String missing() {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "文章不存在");
        }
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingPublicArticlePreserves404() throws Exception {
        mvc.perform(get("/api/articles/not-found")).andExpect(status().isNotFound());
    }

    @Test
    void followedTagsRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/tags/followed")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicReadsAndLoginRemainPublic() throws Exception {
        mvc.perform(get("/api/articles")).andExpect(status().isOk());
        mvc.perform(get("/api/tags")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void authenticatedPersonalRoutesWork() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isOk());
        mvc.perform(get("/api/tags/followed")).andExpect(status().isOk());
    }

    @Test
    void disabledJwtAccountCannotAuthenticate() throws Exception {
        when(jwt.validateToken("valid")).thenReturn(true);
        when(jwt.getUsernameFromToken("valid")).thenReturn("disabled");
        when(users.loadUserByUsername("disabled"))
                .thenReturn(
                        User.builder()
                                .id(1L)
                                .username("disabled")
                                .role(User.Role.USER)
                                .status(0)
                                .build());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedJwtAccountCannotAuthenticate() throws Exception {
        when(jwt.validateToken("valid")).thenReturn(true);
        when(jwt.getUsernameFromToken("valid")).thenReturn("deleted");
        when(users.loadUserByUsername("deleted"))
                .thenReturn(
                        User.builder()
                                .id(1L)
                                .username("deleted")
                                .role(User.Role.USER)
                                .isDeleted(1)
                                .build());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer valid"))
                .andExpect(status().isUnauthorized());
    }
}
