package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.dto.AdminDtos;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/items/pending")
    public ApiResponse<List<ItemDtos.View>> pendingItems() {
        return ApiResponse.ok(adminService.pendingItems());
    }

    @PostMapping("/items/{id}/review")
    public ApiResponse<ItemDtos.View> review(@PathVariable Long id,
                                             @Valid @RequestBody AdminDtos.ReviewRequest request) {
        return ApiResponse.ok(adminService.review(id, request.result(), request.comment()));
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        return ApiResponse.ok(adminService.statistics());
    }
}
