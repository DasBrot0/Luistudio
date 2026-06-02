package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.user.UserLookupResponse;
import com.luistudio.reservas.dto.user.UserResponse;
import com.luistudio.reservas.dto.user.UserStatusUpdateRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.model.UserStatus;
import com.luistudio.reservas.repository.NotificationPreferenceRepository;
import com.luistudio.reservas.repository.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final DtoMapper dtoMapper;

    public UserService(
        UserRepository userRepository,
        NotificationPreferenceRepository notificationPreferenceRepository,
        DtoMapper dtoMapper
    ) {
        this.userRepository = userRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public UserEntity getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public UserEntity getByEmail(String email) {
        return userRepository.findByCorreoIgnoreCase(email)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public UserLookupResponse findForReservationByCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El código de usuario es obligatorio");
        }

        UserEntity user = userRepository.findByCodigoIgnoreCase(normalized)
            .orElseThrow(() -> new NotFoundException("No se encontró un usuario con ese código"));

        if (user.getEstado() != UserStatus.HABILITADO) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El usuario está deshabilitado");
        }

        return new UserLookupResponse(
            user.getCodigo(),
            user.getNombres(),
            user.getApellidos(),
            user.getNombres() + " " + user.getApellidos()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(
        int page,
        int size,
        String query,
        String year,
        String status,
        String sortBy,
        String sortDir
    ) {
        UserStatus userStatus = parseOptionalStatus(status);
        PageRequest pageRequest = PageRequest.of(page, size, buildUserSort(sortBy, sortDir));
        Page<UserEntity> users = userRepository.searchUsers(query, year, userStatus, pageRequest);
        return new PageResponse<>(
            users.getContent().stream().map(dtoMapper::toUser).toList(),
            users.getNumber(),
            users.getSize(),
            users.getTotalElements(),
            users.getTotalPages()
        );
    }

    private UserStatus parseOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Estado invalido");
        }
    }

    private Sort buildUserSort(String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = switch (sortBy == null ? "" : sortBy) {
            case "firstName" -> "nombres";
            case "lastName" -> "apellidos";
            case "code" -> "codigo";
            case "status" -> "estado";
            default -> "id";
        };
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UserStatusUpdateRequest request) {
        UserEntity user = getById(userId);
        UserStatus status;
        try {
            status = UserStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Estado invalido");
        }

        user.setEstado(status);
        user.setActualizadoEn(OffsetDateTime.now());
        return dtoMapper.toUser(userRepository.save(user));
    }

    @Transactional
    public NotificationPreferenceEntity getOrCreatePreferences(UserEntity user) {
        return notificationPreferenceRepository.findByUsuario(user).orElseGet(() -> {
            NotificationPreferenceEntity pref = new NotificationPreferenceEntity();
            pref.setUsuario(user);
            pref.setEmailHabilitado(true);
            pref.setRecordatorioHabilitado(true);
            pref.setCambiosReservaHabilitado(true);
            pref.setThemeMode("LIGHT");
            pref.setFontScale(1.0);
            pref.setLoginLandingView(
                "ADMIN".equalsIgnoreCase(user.getRol().getNombre()) ? "ADMIN_ROOMS" : "STUDENT_MY_BOOKINGS"
            );
            return notificationPreferenceRepository.save(pref);
        });
    }
}
