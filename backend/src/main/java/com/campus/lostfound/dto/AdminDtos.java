package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理后台的请求与响应契约。
 *
 * <p>其中 {@link ReviewRequest} 是审核接口的入参，{@link CategoryRequest} 服务于分类维护。
 */
public final class AdminDtos {

    /** 工具类，禁止实例化。 */
    private AdminDtos() {
    }

    /**
     * 审核请求。
     *
     * <p>{@code result} 用正则锁死在两个取值上，而不是留成自由字符串再在服务层校验：
     * 非法值会在进入业务代码之前就被拦下并返回统一的参数错误，服务层因此不必再写兜底分支。
     *
     * @param result  审核结论，只接受 {@code APPROVED} 或 {@code REJECTED}
     * @param comment 审核意见，可空；驳回时建议填写理由，会随通知一起发给发布者
     */
    public record ReviewRequest(
            @NotBlank(message = "审核结果不能为空")
            @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果只能是 APPROVED 或 REJECTED") String result,
            @Size(max = 500, message = "审核意见不能超过 500 个字符") String comment
    ) {}

    /**
     * 分类新增/修改请求。
     *
     * @param name      分类名称，最长 50 字符；重名由服务层做唯一性校验
     * @param sortOrder 排序权重，越小越靠前；可空，为空时由服务层给定默认值
     */
    public record CategoryRequest(
            @NotBlank(message = "分类名称不能为空")
            @Size(max = 50, message = "分类名称不能超过 50 个字符") String name,
            Integer sortOrder
    ) {}

    /**
     * 统计报表的强类型形态。
     *
     * <p><b>当前未被使用</b>：{@code StatisticsService} 实际返回的是弱类型 {@code Map}，
     * 前端按字符串键取值。本记录是重构期留下的目标形态，改造时需与前端同步，
     * 因此本期先保留定义不做切换。
     */
    public record Statistics(long userCount, long itemCount, long publishedCount,
                             long claimedCount, long pendingReviewCount, long claimCount,
                             double claimRate) {}
}
