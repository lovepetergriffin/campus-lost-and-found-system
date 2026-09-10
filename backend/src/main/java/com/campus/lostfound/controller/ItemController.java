package com.campus.lostfound.controller;

import com.campus.lostfound.common.ApiResponse;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
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
        return ApiResponse.ok(itemService.publicList(keyword, type, categoryId, location, startDate, endDate, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemDtos.View> detail(@PathVariable Long id) {
        return ApiResponse.ok(itemService.detail(id));
    }

    @PostMapping
    public ApiResponse<ItemDtos.View> create(@Valid @RequestBody ItemDtos.SaveRequest request) {
        return ApiResponse.ok(itemService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ItemDtos.View> update(@PathVariable Long id, @Valid @RequestBody ItemDtos.SaveRequest request) {
        return ApiResponse.ok(itemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id) {
        itemService.close(id);
        return ApiResponse.ok();
    }

    @GetMapping("/mine")
    public ApiResponse<List<ItemDtos.View>> mine() {
        return ApiResponse.ok(itemService.mine());
    }
}
