package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.AdminUserDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api-user-account")
public class AuthResource {

    final UserService userService;

    public AuthResource(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<AdminUserDTO> getApiUserAccount(@Valid @RequestBody AuthParams authParams) {
        return ResponseUtil.wrapOrNotFound(userService.getUsers(authParams));
    }
}
