package com.luistudio.reservas.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.model.AuditLogEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.AuditLogRepository;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import com.luistudio.reservas.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccessGuard accessGuard;
    @Mock
    private UserService userService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AvailabilitySubscriptionService subscriptionService;
    @InjectMocks
    private AccountController controller;

    @Test
    void getMyActivityWithoutDatesUsesAQueryWithoutNullDateParameters() {
        UserEntity user = new UserEntity();
        when(accessGuard.requireUser()).thenReturn(new AuthPrincipal(7L, "student@luistudio.edu.pe", "student"));
        when(userService.getById(7L)).thenReturn(user);
        when(auditLogRepository.findByActorAndAccionInOrderByCreadoEnDesc(
            eq(user), any(), eq(PageRequest.of(0, 10))
        )).thenReturn(new PageImpl<>(List.of()));

        PageResponse<?> response = controller.getMyActivity(null, null, 0, 10);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        verify(auditLogRepository).findByActorAndAccionInOrderByCreadoEnDesc(
            eq(user),
            eq(List.of("LOGIN_SUCCESS", "LOGOUT_CURRENT", "LOGOUT_REMOTE", "LOGOUT_ALL", "SENSITIVE_CHANGE_CONFIRMED", "LOGIN_UNUSUAL_ACCESS")),
            eq(PageRequest.of(0, 10))
        );
    }
}
