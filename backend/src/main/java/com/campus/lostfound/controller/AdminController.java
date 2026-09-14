package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.dto.AdminDtos;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.service.AdminReviewService;
import com.campus.lostfound.service.StatisticsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminReviewService reviewService;
    private final StatisticsService statisticsService;

    public AdminController(AdminReviewService reviewService, StatisticsService statisticsService) {
        this.reviewService = reviewService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/items/pending")
    public ApiResponse<List<ItemDtos.View>> pendingItems() {
        return ApiResponse.ok(reviewService.pendingItems());
    }

    @PostMapping("/items/{id}/review")
    public ApiResponse<ItemDtos.View> review(@PathVariable Long id,
                                             @Valid @RequestBody AdminDtos.ReviewRequest request) {
        return ApiResponse.ok(reviewService.review(id, request.result(), request.comment()));
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        return ApiResponse.ok(statisticsService.statistics());
    }
}
