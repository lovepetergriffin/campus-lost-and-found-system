package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.dto.ClaimDtos;
import com.campus.lostfound.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/items/{itemId}")
    public ApiResponse<ClaimDtos.View> create(@PathVariable Long itemId,
                                              @Valid @RequestBody ClaimDtos.CreateRequest request) {
        return ApiResponse.ok(claimService.create(itemId, request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ClaimDtos.View>> mine() {
        return ApiResponse.ok(claimService.myApplications());
    }

    @GetMapping("/received")
    public ApiResponse<List<ClaimDtos.View>> received() {
        return ApiResponse.ok(claimService.received());
    }

    @PatchMapping("/{id}/decision")
    public ApiResponse<ClaimDtos.View> decide(@PathVariable Long id,
                                              @Valid @RequestBody ClaimDtos.DecisionRequest request) {
        return ApiResponse.ok(claimService.decide(id, request.result()));
    }
}
