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

/**
 * 物品信息管理接口，负责参数绑定与响应封装，业务规则交由物品服务执行。
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemQueryService itemQueryService;
    private final ItemCommandService itemCommandService;

    /**
     * 注入物品查询与写操作服务。
     */
    public ItemController(ItemQueryService itemQueryService, ItemCommandService itemCommandService) {
        this.itemQueryService = itemQueryService;
        this.itemCommandService = itemCommandService;
    }

    /**
     * 按可选条件分页查询已公开物品，日期参数采用 ISO 日期格式。
     * 页码和每页条数的边界由查询服务统一处理。
     *
     * @return 公开物品的分页响应
     */
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

    /**
     * 读取详情，未公开物品的可见性由查询服务校验。
     *
     * @param id 物品编号
     * @return 物品详情响应
     */
    @GetMapping("/{id}")
    public ApiResponse<ItemDtos.View> detail(@PathVariable Long id) {
        return ApiResponse.ok(itemQueryService.detail(id));
    }

    /**
     * 校验请求并发布物品，新信息须经过审核才能进入公开列表。
     *
     * @param request 物品发布信息
     * @return 新建物品响应
     */
    @PostMapping
    public ApiResponse<ItemDtos.View> create(@Valid @RequestBody ItemDtos.SaveRequest request) {
        return ApiResponse.ok(itemCommandService.create(request));
    }

    /**
     * 校验请求并修改物品，服务层检查权限和状态并重新置为待审核。
     *
     * @param id 物品编号
     * @param request 修改后的物品信息
     * @return 修改后的物品响应
     */
    @PutMapping("/{id}")
    public ApiResponse<ItemDtos.View> update(@PathVariable Long id, @Valid @RequestBody ItemDtos.SaveRequest request) {
        return ApiResponse.ok(itemCommandService.update(id, request));
    }

    /**
     * 删除物品，由服务层检查管理权限及认领状态。
     *
     * @param id 物品编号
     * @return 成功时不包含数据的响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        itemCommandService.delete(id);
        return ApiResponse.ok();
    }

    /**
     * 关闭物品，由服务层检查管理权限及认领状态。
     *
     * @param id 物品编号
     * @return 成功时不包含数据的响应
     */
    @PatchMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id) {
        itemCommandService.close(id);
        return ApiResponse.ok();
    }

    /**
     * 返回当前登录用户发布的物品，包括未公开状态。
     *
     * @return 当前用户的物品列表响应
     */
    @GetMapping("/mine")
    public ApiResponse<List<ItemDtos.View>> mine() {
        return ApiResponse.ok(itemQueryService.mine());
    }
}
