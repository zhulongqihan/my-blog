package com.myblog.security;

import com.myblog.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountStatusResponseTest {
    @RestController
    static class LoginFailure {
        @GetMapping("/disabled") public void disabled() { throw new DisabledException("disabled"); }
        @GetMapping("/locked") public void locked() { throw new LockedException("locked"); }
    }

    @Test void disabledLoginReturnsAuthenticationFailure() throws Exception { assertUnauthorized("/disabled"); }
    @Test void lockedLoginReturnsAuthenticationFailure() throws Exception { assertUnauthorized("/locked"); }

    private void assertUnauthorized(String path) throws Exception {
        MockMvcBuilders.standaloneSetup(new LoginFailure()).setControllerAdvice(new GlobalExceptionHandler())
                .build().perform(get(path)).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("message").value("用户名或密码错误"));
    }
}
