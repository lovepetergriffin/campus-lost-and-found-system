package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminDtos {
    private AdminDtos() {}

    public record ReviewRequest(
            @NotBlank(message = "审核结果不能为空")
            @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果只能是 APPROVED 或 REJECTED") String result,
            @Size(max = 500, message = "审核意见不能超过 500 个字符") String comment
    ) {}

    public record CategoryRequest(
            @NotBlank(message = "分类名称不能为空")
            @Size(max = 50, message = "分类名称不能超过 50 个字符") String name,
            Integer sortOrder
    ) {}

    public record Statistics(long userCount, long itemCount, long publishedCount,
                             long claimedCount, long pendingReviewCount, long claimCount,
                             double claimRate) {}
}
