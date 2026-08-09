package com.der.kitchen.auth.service;

import com.der.kitchen.user.entity.User;
import com.der.kitchen.user.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StpInterfaceImplTest {

    private final UserService userService = mock(UserService.class);
    private final StpInterfaceImpl stpInterface = new StpInterfaceImpl(userService);

    @Test
    void returnsActiveUserRole() {
        User user = new User();
        user.setRole("admin");
        user.setStatus("active");
        when(userService.getById(1L)).thenReturn(user);

        assertThat(stpInterface.getRoleList("1", "login")).containsExactly("admin");
    }

    @Test
    void rejectsDisabledUserRole() {
        User user = new User();
        user.setRole("admin");
        user.setStatus("disabled");
        when(userService.getById(2L)).thenReturn(user);

        assertThat(stpInterface.getRoleList(2L, "login")).isEqualTo(List.of());
    }
}
