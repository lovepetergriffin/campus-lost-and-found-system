package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.dto.ClaimDtos;
import com.campus.lostfound.service.ClaimApplicationService;
import com.campus.lostfound.service.ClaimDecisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimApplicationService applicationService;
    private final ClaimDecisionService decisionService;

    public ClaimController(ClaimApplicationService applicationService, ClaimDecisionService decisionService) {
        this.applicationService = applicationService;
        this.decisionService = decisionService;
    }

    @PostMapping("/items/{itemId}")
    public ApiResponse<ClaimDtos.View> create(@PathVariable Long itemId,
                                              @Valid @RequestBody ClaimDtos.CreateRequest request) {
        return ApiResponse.ok(applicationService.create(itemId, request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ClaimDtos.View>> mine() {
        return ApiResponse.ok(applicationService.myApplications());
    }

    @GetMapping("/received")
    public ApiResponse<List<ClaimDtos.View>> received() {
        return ApiResponse.ok(applicationService.received());
    }

    @PatchMapping("/{id}/decision")
    public ApiResponse<ClaimDtos.View> decide(@PathVariable Long id,
                                              @Valid @RequestBody ClaimDtos.DecisionRequest request) {
        return ApiResponse.ok(decisionService.decide(id, request.result()));
    }
}
