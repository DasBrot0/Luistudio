package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.admin.AdminConfigResponse;
import com.luistudio.reservas.dto.admin.AdminConfigUpdateRequest;
import com.luistudio.reservas.dto.admin.CampusMapResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.user.UserResponse;
import com.luistudio.reservas.dto.user.UserStatusUpdateRequest;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.CampusMapService;
import com.luistudio.reservas.service.SystemConfigService;
import com.luistudio.reservas.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AdminController {

    private final AccessGuard accessGuard;
    private final UserService userService;
    private final SystemConfigService systemConfigService;
    private final CampusMapService campusMapService;

    public AdminController(
        AccessGuard accessGuard,
        UserService userService,
        SystemConfigService systemConfigService,
        CampusMapService campusMapService
    ) {
        this.accessGuard = accessGuard;
        this.userService = userService;
        this.systemConfigService = systemConfigService;
        this.campusMapService = campusMapService;
    }

    @GetMapping("/admin/users")
    public PageResponse<UserResponse> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String query
    ) {
        accessGuard.requireAdmin();
        return userService.listUsers(page, size, query);
    }

    @PatchMapping("/admin/users/{userId}/estado")
    public UserResponse updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        accessGuard.requireAdmin();
        return userService.updateStatus(userId, request);
    }

    @GetMapping("/admin/config")
    public AdminConfigResponse getConfig() {
        accessGuard.requireAdmin();
        return systemConfigService.getConfig();
    }

    @PutMapping("/admin/config")
    public AdminConfigResponse updateConfig(@Valid @RequestBody AdminConfigUpdateRequest request) {
        accessGuard.requireAdmin();
        return systemConfigService.updateConfig(request);
    }

    @GetMapping("/campus/map")
    public CampusMapResponse getCampusMap() {
        accessGuard.requireUser();
        return campusMapService.getCampusMap();
    }
}
