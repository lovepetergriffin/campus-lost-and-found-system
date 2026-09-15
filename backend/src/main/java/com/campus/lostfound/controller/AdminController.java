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

/**
 * 管理后台的 HTTP 入口，统一挂在 {@code /api/admin} 下。
 *
 * <p>类级 {@link PreAuthorize} 把整组端点收口为"仅管理员"：
 * 写在类上而非逐个方法上，好处是<b>新增端点默认就是受保护的</b>，
 * 不会因为忘记标注解而无意中对外开放。放行规则见 {@code SecurityConfig}。
 */
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

    /**
     * 待审核物品列表，供审核工作台首屏使用。
     *
     * @return 待审核物品的只读视图列表，按提交时间正序
     */
    @GetMapping("/items/pending")
    public ApiResponse<List<ItemDtos.View>> pendingItems() {
        return ApiResponse.ok(reviewService.pendingItems());
    }

    /**
     * 通过或驳回一件物品。
     *
     * @param id      物品主键
     * @param request 审核请求体，{@code result} 只接受 {@code APPROVED} / {@code REJECTED}
     * @return 审核后物品的最新视图
     */
    @PostMapping("/items/{id}/review")
    public ApiResponse<ItemDtos.View> review(@PathVariable Long id,
                                             @Valid @RequestBody AdminDtos.ReviewRequest request) {
        return ApiResponse.ok(reviewService.review(id, request.result(), request.comment()));
    }

    /**
     * 概览统计报表。
     *
     * <p>返回值为弱类型的映射，键名约定见 {@link StatisticsService} 的类注释。
     *
     * @return 各项统计指标
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics() {
        return ApiResponse.ok(statisticsService.statistics());
    }
}
