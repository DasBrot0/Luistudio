package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.user.UserLookupResponse;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserLookupController {

    private final AccessGuard accessGuard;
    private final UserService userService;

    public UserLookupController(AccessGuard accessGuard, UserService userService) {
        this.accessGuard = accessGuard;
        this.userService = userService;
    }

    @GetMapping("/lookup")
    public UserLookupResponse lookupByCode(@RequestParam String code) {
        accessGuard.requireUser();
        return userService.findForReservationByCode(code);
    }
}
