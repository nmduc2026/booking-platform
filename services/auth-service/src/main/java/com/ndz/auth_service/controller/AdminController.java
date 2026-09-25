package com.ndz.auth_service.controller;

import com.ndz.auth_service.dto.AdminCreateUserRequest;
import com.ndz.auth_service.dto.PageResponse;
import com.ndz.auth_service.dto.UserResponse;
import com.ndz.auth_service.dto.UserShopMappingRequest;
import com.ndz.auth_service.dto.UserShopMappingResponse;
import com.ndz.auth_service.entity.Role;
import com.ndz.auth_service.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String q
    ) {
        return adminService.listUsers(page, size, role, q);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        return adminService.createUser(request);
    }

    @PostMapping("/user-shop-mapping")
    @ResponseStatus(HttpStatus.CREATED)
    public UserShopMappingResponse assignShop(@Valid @RequestBody UserShopMappingRequest request) {
        return adminService.assignShop(request);
    }
}
