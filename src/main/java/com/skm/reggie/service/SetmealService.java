package com.skm.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.skm.reggie.domain.Setmeal;
import com.skm.reggie.dto.SetmealDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDto
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐，同时删除套餐和菜品的关联关系
     * @param ids
     */
    public void removeWithDish(List<Long> ids);

    /**
     * 根据菜品id,查询套餐和菜品的关联关系
     * @return
     */
    public SetmealDto getByDishId(List<Long> dishIds);
}
