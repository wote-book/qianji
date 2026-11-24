package com.example.finance.category.service;

import com.example.finance.category.dto.CategoryBatchRequest;
import com.example.finance.category.dto.CategoryRequest;
import com.example.finance.category.entity.Category;
import com.example.finance.category.mapper.CategoryMapper;
import com.example.finance.common.BizException;
import com.example.finance.common.PresetCategories;
import com.example.finance.record.mapper.RecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final RecordMapper recordMapper;

    public List<Category> list(Long userId, String type) {
        return categoryMapper.selectByUser(userId, type);
    }

    public Category getOwned(Long userId, Long id) {
        Category category = categoryMapper.selectByIdAndUser(id, userId);
        if (category == null) {
            throw BizException.notFound();
        }
        return category;
    }

    @Transactional(rollbackFor = Exception.class)
    public Category create(Long userId, CategoryRequest request) {
        if (categoryMapper.selectByUserTypeName(userId, request.type(), request.name()) != null) {
            throw BizException.conflict("同名分类已存在");
        }
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setIcon(request.icon());
        category.setIsPreset(0);
        category.setStatus(1);
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 批量添加分类：自动去重、跳过同类型下已存在的名称，返回新增数量与跳过的名称。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCreate(Long userId, CategoryBatchRequest request) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String raw : request.names()) {
            String name = raw == null ? "" : raw.trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        int added = 0;
        List<String> skipped = new ArrayList<>();
        for (String name : names) {
            if (categoryMapper.selectByUserTypeName(userId, request.type(), name) != null) {
                skipped.add(name);
                continue;
            }
            Category category = new Category();
            category.setUserId(userId);
            category.setName(name);
            category.setType(request.type());
            category.setIsPreset(0);
            category.setStatus(1);
            categoryMapper.insert(category);
            added++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("added", added);
        result.put("skipped", skipped);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, CategoryRequest request) {
        Category exists = getOwned(userId, id);
        if (exists.getIsPreset() == 1 && !exists.getName().equals(request.name())) {
            Category same = categoryMapper.selectByUserTypeName(userId, exists.getType(), request.name());
            if (same != null && !same.getId().equals(id)) {
                throw BizException.conflict("同名分类已存在");
            }
        }
        Category sameName = categoryMapper.selectByUserTypeName(userId, exists.getType(), request.name());
        if (sameName != null && !sameName.getId().equals(id)) {
            throw BizException.conflict("同名分类已存在");
        }
        categoryMapper.update(id, userId, request.name().trim(), request.icon());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Long id, Integer status) {
        getOwned(userId, id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态只能为 0 或 1");
        }
        categoryMapper.updateStatus(id, userId, status);
    }

    /**
     * 删除规则（Q-08）：预设不可删；被流水引用不可删（可停用）；未引用可删。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        Category category = getOwned(userId, id);
        if (category.getIsPreset() != null && category.getIsPreset() == 1) {
            throw BizException.conflict("预设分类不可删除，可改为停用");
        }
        int used = recordMapper.countByCategory(userId, id);
        if (used > 0) {
            throw BizException.conflict("该分类已被 " + used + " 条流水引用，不能删除，请改为停用");
        }
        categoryMapper.delete(id, userId);
    }

    /** 注册时复制预设分类 */
    @Transactional(rollbackFor = Exception.class)
    public void initPresets(Long userId) {
        for (PresetCategories.Preset preset : PresetCategories.ALL) {
            Category category = new Category();
            category.setUserId(userId);
            category.setName(preset.name());
            category.setType(preset.type());
            category.setIcon(preset.icon());
            category.setIsPreset(1);
            category.setStatus(1);
            categoryMapper.insert(category);
        }
    }

    public Category findByName(Long userId, String type, String name) {
        return categoryMapper.selectByUserTypeName(userId, type, name);
    }
}
