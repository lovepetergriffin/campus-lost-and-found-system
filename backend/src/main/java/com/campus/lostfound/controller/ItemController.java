package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.service.ItemCommandService;
import com.campus.lostfound.service.ItemQueryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemQueryService itemQueryService;
    private final ItemCommandService itemCommandService;

    public ItemController(ItemQueryService itemQueryService, ItemCommandService itemCommandService) {
        this.itemQueryService = itemQueryService;
        this.itemCommandService = itemCommandService;
    }

    @GetMapping
    public ApiResponse<PageResult<ItemDtos.View>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long size) {
        return ApiResponse.ok(itemQueryService.publicList(
                keyword, type, categoryId, location, startDate, endDate, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemDtos.View> detail(@PathVariable Long id) {
        return ApiResponse.ok(itemQueryService.detail(id));
    }

    @PostMapping
    public ApiResponse<ItemDtos.View> create(@Valid @RequestBody ItemDtos.SaveRequest request) {
        return ApiResponse.ok(itemCommandService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ItemDtos.View> update(@PathVariable Long id, @Valid @RequestBody ItemDtos.SaveRequest request) {
        return ApiResponse.ok(itemCommandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        itemCommandService.delete(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id) {
        itemCommandService.close(id);
        return ApiResponse.ok();
    }

    @GetMapping("/mine")
    public ApiResponse<List<ItemDtos.View>> mine() {
        return ApiResponse.ok(itemQueryService.mine());
    }
}
