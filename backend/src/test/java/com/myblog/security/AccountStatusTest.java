package com.myblog.security;

import static org.assertj.core.api.Assertions.*;

import com.myblog.entity.User;

import org.junit.jupiter.api.Test;

class AccountStatusTest {
    @Test
    void activeAccountWorks() {
        assertThat(User.builder().build().isEnabled()).isTrue();
    }

    @Test
    void disabledAccountIsDisabled() {
        assertThat(User.builder().status(0).build().isEnabled()).isFalse();
    }

    @Test
    void deletedAccountIsDisabled() {
        assertThat(User.builder().isDeleted(1).build().isEnabled()).isFalse();
    }
}
