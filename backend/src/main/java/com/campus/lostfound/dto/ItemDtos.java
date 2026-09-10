package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ItemDtos {
    private ItemDtos() {}

    public record SaveRequest(
            @NotBlank(message = "物品名称不能为空")
            @Size(max = 100, message = "物品名称不能超过 100 个字符") String name,
            @NotBlank(message = "信息类型不能为空")
            @Pattern(regexp = "LOST|FOUND", message = "信息类型只能是 LOST 或 FOUND") String type,
            @NotNull(message = "请选择物品分类") Long categoryId,
            @NotBlank(message = "地点不能为空")
            @Size(max = 200, message = "地点不能超过 200 个字符") String location,
            @NotNull(message = "时间不能为空") LocalDateTime eventTime,
            @NotBlank(message = "物品描述不能为空")
            @Size(max = 500, message = "物品描述不能超过 500 个字符") String description,
            @Size(max = 500, message = "图片地址不能超过 500 个字符") String imageUrl,
            @Size(max = 100, message = "联系方式不能超过 100 个字符") String contact
    ) {}

    public record View(
            Long id, String name, String type, Long categoryId, String categoryName,
            String location, LocalDateTime eventTime, String description, String imageUrl,
            String contact, String status, Long publisherId, String publisherName,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}
}
