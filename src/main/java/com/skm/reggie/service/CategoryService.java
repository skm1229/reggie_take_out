package com.skm.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skm.reggie.domain.Category;

public interface CategoryService extends IService<Category> {
    /**
     * 根据id删除分类,删除之前需要判断
     * @param ids
     */
    public void remove(Long ids);
}
