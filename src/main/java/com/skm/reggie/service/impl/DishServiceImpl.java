package com.skm.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skm.reggie.common.CustomException;
import com.skm.reggie.common.R;
import com.skm.reggie.domain.Dish;
import com.skm.reggie.domain.DishFlavor;
import com.skm.reggie.domain.SetmealDish;
import com.skm.reggie.dto.DishDto;
import com.skm.reggie.dto.SetmealDto;
import com.skm.reggie.mapper.DishMapper;
import com.skm.reggie.service.DishFlavorService;
import com.skm.reggie.service.DishService;
import com.skm.reggie.service.SetmealDishService;
import com.skm.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品，同时保存口味数据
     * @param dishDto
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品基本信息到dish表
        this.save(dishDto);

        //菜品id
        Long dishId = dishDto.getId();

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) ->{
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id查询菜品信息和对应口味信息
     * @param id
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息
        Dish dish = this.getById(id);

        //拷贝Dish到DishDto
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);


        //查询当前菜品对应的口味信息,从dish_flavor飙车查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> list = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(list);

        return dishDto;

    }

    /**
     * /更新菜品信息，同时更新对应的口味信息
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);
        //清理当前菜品对应口味数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(queryWrapper);

        //添加当前提交过来的口味数据,并重新向dish_flavor表插入数据
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 删除菜品信息，并删除对应的口味信息
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithFlavor(List<Long> ids) {
        //查看菜品状态，确定能否删除
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId,ids);
        queryWrapper.eq(Dish::getStatus,1);
        int count = this.count(queryWrapper);
        if(count > 0) {
            //如果不能删除，抛出一个业务异常
            throw new CustomException("菜品正在售卖中，无法删除");
        }
        //根据菜品id查询套餐表中是否含有该菜品,确定能否删除
        SetmealDto setmealDto = setmealService.getByDishId(ids);
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        String setmealDishName = "";
        if(setmealDishes != null){
            //获取相关套餐中的名称
            for (SetmealDish setmealDish : setmealDishes) {
                String name = setmealDish.getName();
                setmealDishName += name + " ";
            }
            String setmealName = setmealDto.getName();
            throw new CustomException( setmealName + " 套餐中含有{"+ setmealDishName +"}无法删除");
        }

        //删除菜品及菜品相关的口味信息
        this.removeByIds(ids);
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.in(DishFlavor::getDishId,ids);
        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);

    }

}
