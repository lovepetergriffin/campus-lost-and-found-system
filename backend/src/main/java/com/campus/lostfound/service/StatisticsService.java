package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台的统计报表：把物品、认领、用户三张表的计数汇总成一个概览。
 *
 * <p>指标口径如下（前端 {@code AdminView.vue} 按下述键名取值，两边必须同步修改）：
 * <ul>
 *   <li>{@code userCount} —— 普通用户数，<b>不含管理员</b></li>
 *   <li>{@code itemCount} —— 全部物品数，含待审、已驳回</li>
 *   <li>{@code publishedCount} / {@code pendingCount} / {@code claimedCount} —— 按状态分列</li>
 *   <li>{@code claimCount} —— 认领申请总数（一张申请算一条，与物品数不同量纲）</li>
 *   <li>{@code claimRate} —— 认领达成率，分子是已认领物品数，分母是"已发布 + 已认领"</li>
 *   <li>{@code lostCount} / {@code foundCount} —— 按丢失/招领类型分列</li>
 * </ul>
 *
 * <p>已知取舍：返回 {@code Map<String, Object>} 而非强类型 DTO，
 * 前端按字符串键取值，字段名写错编译器不会报错。改造方案见报告的度量闭环清单，
 * 本期未实施以免破坏既有前端契约。
 */
@Service
public class StatisticsService {
    private final ItemMapper itemMapper;
    private final ClaimMapper claimMapper;
    private final UserMapper userMapper;

    public StatisticsService(ItemMapper itemMapper, ClaimMapper claimMapper, UserMapper userMapper) {
        this.itemMapper = itemMapper;
        this.claimMapper = claimMapper;
        this.userMapper = userMapper;
    }

    /**
     * 汇总一次概览统计。
     *
     * <p>用 {@link LinkedHashMap} 而非 {@code HashMap}：保持指标按业务分组排列，
     * 返回的 JSON 字段顺序稳定，方便调试时肉眼比对。
     *
     * <p>实现上对每个指标各发一条 {@code COUNT} 查询。当前数据量下开销可忽略，
     * 若日后统计维度继续增加，应改为一条 {@code GROUP BY} 聚合查询。
     *
     * @return 指标名到取值的映射，键名与前端约定见类注释
     */
    public Map<String, Object> statistics() {
        Map<String, Object> data = new LinkedHashMap<>();
        long claimedCount = countItemsByStatus("CLAIMED");
        long publishedOrClaimed = itemMapper.selectCount(new LambdaQueryWrapper<Item>()
                .in(Item::getStatus, List.of("PUBLISHED", "CLAIMED")));

        data.put("userCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER")));
        data.put("itemCount", itemMapper.selectCount(null));
        data.put("publishedCount", countItemsByStatus("PUBLISHED"));
        data.put("pendingCount", countItemsByStatus("PENDING"));
        data.put("claimedCount", claimedCount);
        data.put("claimCount", claimMapper.selectCount(null));
        data.put("claimRate", percentage(claimedCount, publishedOrClaimed));
        data.put("lostCount", countItemsByType("LOST"));
        data.put("foundCount", countItemsByType("FOUND"));
        return data;
    }

    /**
     * 按发布状态统计物品数。
     *
     * @param status 状态标识，取值 {@code PENDING} / {@code PUBLISHED} / {@code CLAIMED} 等
     */
    private long countItemsByStatus(String status) {
        return itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getStatus, status));
    }

    /**
     * 按物品类型统计数量。
     *
     * @param type 类型标识，取值 {@code LOST}（寻物）/ {@code FOUND}（招领）
     */
    private long countItemsByType(String type) {
        return itemMapper.selectCount(new LambdaQueryWrapper<Item>().eq(Item::getType, type));
    }

    /**
     * 求百分比并保留两位小数。
     *
     * <p>分母为 0 时直接返回 0 而不是抛出除零异常 —— 系统刚上线还没有任何已发布物品时，
     * 统计接口仍然要能正常返回，否则管理后台首屏就会报错。
     *
     * @param part  分子
     * @param total 分母
     * @return 百分数值，如 {@code 37.5} 表示 37.5%
     */
    private double percentage(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 10000.0 / total) / 100.0;
    }
}
