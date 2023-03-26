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

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    public SetmealDto getByIdWithDish(Long id);

    /**
     * 更新套餐信息，同时更新对应的菜品信息
     * @param setmealDto
     */
    public void updateWithDish(SetmealDto setmealDto);


}
