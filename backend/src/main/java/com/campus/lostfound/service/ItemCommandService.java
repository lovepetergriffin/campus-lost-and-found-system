package com.campus.lostfound.service;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Item;
import com.campus.lostfound.dto.ItemDtos;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 处理物品发布、修改、删除和关闭，统一执行编辑权限及状态限制。
 */
@Service
public class ItemCommandService {
    private final ItemMapper itemMapper;
    private final CategoryService categoryService;
    private final ItemQueryService itemQueryService;
    private final ItemViewAssembler assembler;
    private final ItemAccessPolicy accessPolicy;

    /**
     * 注入物品写入、分类校验、查询、视图组装和权限检查依赖。
     */
    public ItemCommandService(ItemMapper itemMapper, CategoryService categoryService,
                              ItemQueryService itemQueryService, ItemViewAssembler assembler,
                              ItemAccessPolicy accessPolicy) {
        this.itemMapper = itemMapper;
        this.categoryService = categoryService;
        this.itemQueryService = itemQueryService;
        this.assembler = assembler;
        this.accessPolicy = accessPolicy;
    }

    /**
     * 以当前登录用户为发布者创建物品，初始状态为待审核。
     * 创建与更新时间保持一致，分类必须存在。
     *
     * @param request 已通过接口参数校验的物品信息
     * @return 创建后的物品视图
     * @throws BusinessException 分类不存在
     */
    public ItemDtos.View create(ItemDtos.SaveRequest request) {
        categoryService.require(request.categoryId());
        Item item = new Item();
        copyRequest(request, item);
        item.setUserId(CurrentUser.get().id());
        item.setStatus("PENDING");
        touch(item);
        item.setCreatedAt(item.getUpdatedAt());
        itemMapper.insert(item);
        return assembler.toView(item);
    }

    /**
     * 修改有管理权限且尚未认领或关闭的物品，修改后重新进入待审核状态。
     * 保留发布者及原始创建时间，并更新最后修改时间。
     *
     * @param id 待修改的物品编号
     * @param request 已通过接口参数校验的新物品信息
     * @return 修改后的物品视图
     * @throws BusinessException 物品或分类不存在、无编辑权限或状态不允许修改
     */
    public ItemDtos.View update(Long id, ItemDtos.SaveRequest request) {
        Item item = itemQueryService.require(id);
        accessPolicy.assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus()) || "CLOSED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已认领或已关闭的信息不能修改");
        }
        categoryService.require(request.categoryId());
        copyRequest(request, item);
        item.setStatus("PENDING");
        touch(item);
        itemMapper.updateById(item);
        return assembler.toView(item);
    }

    /**
     * 删除当前用户有权管理且尚未认领的物品。
     *
     * @param id 待删除的物品编号
     * @throws BusinessException 物品不存在、无编辑权限或物品已认领
     */
    public void delete(Long id) {
        Item item = itemQueryService.require(id);
        accessPolicy.assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已认领的信息不能删除");
        }
        itemMapper.deleteById(id);
    }

    /**
     * 关闭当前用户有权管理且尚未认领的物品，并更新修改时间。
     * 关闭后的物品不再出现在公开列表中。
     *
     * @param id 待关闭的物品编号
     * @throws BusinessException 物品不存在、无编辑权限或物品已认领
     */
    public void close(Long id) {
        Item item = itemQueryService.require(id);
        accessPolicy.assertCanEdit(item);
        if ("CLAIMED".equals(item.getStatus())) {
            throw BusinessException.badRequest("已认领的信息不能关闭");
        }
        item.setStatus("CLOSED");
        touch(item);
        itemMapper.updateById(item);
    }

    /**
     * 仅复制可编辑字段；发布者、状态及时间戳由业务方法维护。
     */
    private void copyRequest(ItemDtos.SaveRequest request, Item item) {
        item.setName(request.name().trim());
        item.setType(request.type().toUpperCase());
        item.setCategoryId(request.categoryId());
        item.setLocation(request.location().trim());
        item.setEventTime(request.eventTime());
        item.setDescription(request.description().trim());
        item.setImageUrl(blankToNull(request.imageUrl()));
        item.setContact(blankToNull(request.contact()));
    }

    private void touch(Item item) {
        item.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 将可选图片地址与联系方式中的空白值统一保存为 null。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
