package com.skm.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skm.reggie.common.CustomException;
import com.skm.reggie.common.R;
import com.skm.reggie.domain.Category;
import com.skm.reggie.domain.Dish;
import com.skm.reggie.domain.Setmeal;
import com.skm.reggie.mapper.CategoryMapper;
import com.skm.reggie.service.CategoryService;
import com.skm.reggie.service.DishService;
import com.skm.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类,删除之前需要判断
     * @param ids
     */
    @Override
    public void remove(Long ids) {
        //查询当前分类是否关联了菜品,如果已经关联,抛出一个业务异常
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件,根据分类id进行查询
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,ids);
        int count1 = dishService.count(dishLambdaQueryWrapper);

        if(count1 > 0) {
            //已经关联菜品,抛出一个业务异常
            throw new CustomException("当前分类关联了菜品,不能删除~~");
        }


        //查询当前分类是否关联了套餐,如果已经关联,抛出一个业务异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件,根据分类id进行查询
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,ids);
        int count2 = setmealService.count();

        if(count2 > 0) {
            //已经关联套餐,抛出一个业务异常
            throw new CustomException("当前分类关联了套餐,不能删除~~");
        }
        //正常删除
        super.removeById(ids);
    }


}
