package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.domain.Category;
import com.campus.lostfound.mapper.CategoryMapper;
import com.campus.lostfound.mapper.ItemMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;
    private final ItemMapper itemMapper;

    public CategoryService(CategoryMapper categoryMapper, ItemMapper itemMapper) {
        this.categoryMapper = categoryMapper;
        this.itemMapper = itemMapper;
    }

    public List<Category> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSortOrder).orderByAsc(Category::getId));
    }

    public Category create(String name, Integer sortOrder) {
        validateUnique(name, null);
        Category category = new Category();
        category.setName(name.trim());
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        categoryMapper.insert(category);
        return category;
    }

    public Category update(Long id, String name, Integer sortOrder) {
        Category category = require(id);
        validateUnique(name, id);
        category.setName(name.trim());
        category.setSortOrder(sortOrder == null ? 0 : sortOrder);
        categoryMapper.updateById(category);
        return category;
    }

    public void delete(Long id) {
        require(id);
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<com.campus.lostfound.domain.Item>()
                .eq(com.campus.lostfound.domain.Item::getCategoryId, id));
        if (count > 0) throw BusinessException.badRequest("该分类已有物品信息，不能删除");
        categoryMapper.deleteById(id);
    }

    public Category require(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) throw BusinessException.badRequest("物品分类不存在");
        return category;
    }

    private void validateUnique(String name, Long excludeId) {
        if (name == null || name.isBlank()) throw BusinessException.badRequest("分类名称不能为空");
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<Category>().eq(Category::getName, name.trim());
        if (excludeId != null) query.ne(Category::getId, excludeId);
        if (categoryMapper.selectCount(query) > 0) throw BusinessException.badRequest("分类名称已存在");
    }
}
