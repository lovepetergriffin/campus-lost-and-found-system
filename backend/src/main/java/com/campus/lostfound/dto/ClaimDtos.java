package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ClaimDtos {
    private ClaimDtos() {}

    public record CreateRequest(
            @NotBlank(message = "认领说明不能为空")
            @Size(max = 500, message = "认领说明不能超过 500 个字符") String description,
            @NotBlank(message = "验证信息不能为空")
            @Size(max = 500, message = "验证信息不能超过 500 个字符") String proof
    ) {}

    public record DecisionRequest(
            @NotBlank(message = "处理结果不能为空")
            @Pattern(regexp = "APPROVED|REJECTED", message = "处理结果只能是 APPROVED 或 REJECTED") String result
    ) {}

    public record View(
            Long id, Long itemId, String itemName, Long applicantId, String applicantName,
            String description, String proof, String status,
            LocalDateTime createdAt, LocalDateTime processedAt
    ) {}
}
