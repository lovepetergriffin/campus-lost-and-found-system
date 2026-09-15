package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 负责物品查询与详情可见性检查，委托筛选器和组装器完成条件构建及视图转换。
 */
@Service
public class ItemQueryService {
    private final ItemMapper itemMapper;
    private final ItemViewAssembler assembler;
    private final ItemFilterBuilder filterBuilder;
    private final ItemAccessPolicy accessPolicy;

    /**
     * 注入物品查询所需的持久化、组装、筛选和访问策略依赖。
     */
    public ItemQueryService(ItemMapper itemMapper, ItemViewAssembler assembler,
                            ItemFilterBuilder filterBuilder, ItemAccessPolicy accessPolicy) {
        this.itemMapper = itemMapper;
        this.assembler = assembler;
        this.filterBuilder = filterBuilder;
        this.accessPolicy = accessPolicy;
    }

    /**
     * 分页查询已公开物品，并组装分类名称和发布者名称。
     * 页码最小为 1，每页条数限制在 1 至 50，避免一次返回过多数据。
     *
     * @param keyword 名称或描述关键词，空白表示不限
     * @param type 物品类型，空白表示不限
     * @param categoryId 分类编号，null 表示不限
     * @param location 地点片段，空白表示不限
     * @param startDate 发生日期下界，包含当天
     * @param endDate 发生日期上界，包含当天
     * @param page 请求页码，从 1 开始
     * @param size 请求每页条数
     * @return 物品视图、总条数和实际分页参数
     */
    public PageResult<ItemDtos.View> publicList(String keyword, String type, Long categoryId, String location,
                                                LocalDate startDate, LocalDate endDate, long page, long size) {
        LambdaQueryWrapper<Item> query = filterBuilder.build(
                keyword, type, categoryId, location, startDate, endDate);

        Page<Item> result = itemMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 50)), query);
        return new PageResult<>(result.getRecords().stream().map(assembler::toView).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 查询物品详情；未公开物品仅发布者本人或管理员可查看。
     * 对无权查看的请求返回不存在，避免暴露未公开物品信息。
     *
     * @param id 物品编号
     * @return 允许当前访问者查看的物品视图
     * @throws BusinessException 物品不存在或当前访问者无权查看
     */
    public ItemDtos.View detail(Long id) {
        Item item = require(id);
        if (!"PUBLISHED".equals(item.getStatus()) && !accessPolicy.canManage(item)) {
            throw BusinessException.notFound("物品信息不存在或尚未公开");
        }
        return assembler.toView(item);
    }

    /**
     * 查询当前登录用户发布的全部状态的物品，按发布时间倒序返回。
     *
     * @return 当前用户发布的物品视图列表
     */
    public List<ItemDtos.View> mine() {
        return itemMapper.selectList(new LambdaQueryWrapper<Item>()
                        .eq(Item::getUserId, CurrentUser.get().id()).orderByDesc(Item::getCreatedAt))
                .stream().map(assembler::toView).toList();
    }

    /**
     * 按编号加载物品，供业务服务内部使用。
     * 此方法仅检查存在性，不校验访问权限；调用方负责所需的权限检查。
     *
     * @param id 物品编号
     * @return 已存在的物品实体
     * @throws BusinessException 物品不存在
     */
    public Item require(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) throw BusinessException.notFound("物品信息不存在");
        return item;
    }

}
