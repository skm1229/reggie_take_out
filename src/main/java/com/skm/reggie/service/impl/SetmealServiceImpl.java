package com.skm.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.skm.reggie.common.CustomException;
import com.skm.reggie.domain.Setmeal;
import com.skm.reggie.domain.SetmealDish;
import com.skm.reggie.dto.SetmealDto;
import com.skm.reggie.mapper.SetmealMapper;
import com.skm.reggie.service.SetmealDishService;
import com.skm.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐的基本信息，操作setmeal表，执行insert操作
        this.save(setmealDto);

        //保存套餐和菜品的关联信息，操作setmeal_dish表，执行insert操作
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }
    /**
     * 删除套餐，同时删除套餐和菜品的关联关系
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查询套餐状态，确定是否可以删除
        //select count(*) from setmeal where id in(1,2,3) and status = 1
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId,ids);
        queryWrapper.eq(Setmeal::getStatus,1);

        int count = this.count(queryWrapper);
        if(count > 0) {
            //如果不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，无法删除~~");
        }


        //如果可以删除，先删除套餐表中的数据 setmeal表
        this.removeByIds(ids);


        //delete from setmeal_dish where setmeal_id in(1,2,3)
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);

        //再删除关系表中的数据 setmeal_dish表
        setmealDishService.remove(lambdaQueryWrapper);

    }
    /**
     * 根据菜品id,查询套餐和菜品的关联关系
     * @return
     */
    @Override
    public SetmealDto getByDishId(List<Long> dishIds) {
        //查询套餐和菜品关系表中是否含有该菜品
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getDishId,dishIds);
        //为了封装到SetmealDto中,此刻依据dishId查询出来的list集合可能因为不同套餐含有同一个菜品而查询出多个结果
        //而一个SetmealDto只能封装一个套餐表，所以需要对其进行去重,再拷贝到SetmealDto中
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        //如果没有在套餐和菜品的关系表中查出对应菜品 则直接返回一个新的SetmealDto
        if(list.size() == 0) {
            return new SetmealDto();
        }

        //得到集合中所有的setmealId(因为此时的list集合中含有多对重复的strmealId)
        List<Long> longOldList = new ArrayList<>();
        for (SetmealDish setmealDish : list) {
            Long setmealId = setmealDish.getSetmealId();
            longOldList.add(setmealId);
        }


        //去除重复的strmealId
        List<Long> longNewList = longOldList.stream().distinct().collect(Collectors.toList());
        log.info("longNewList = {}",longNewList);

        //依据不同的strmealId进行查询(只取出第一个strmealId,并依据该strmealId查询数据库)
        Long aLong = longNewList.get(0);
        queryWrapper.eq(SetmealDish::getSetmealId,aLong);
        List<SetmealDish> newList = setmealDishService.list(queryWrapper);
        log.info("newList = {}",newList);
        //将查询出来的相关套餐菜品关系信息拷贝到Setmeal的SetmealDishes集合中
        SetmealDto setmealDto = new SetmealDto();
        setmealDto.setSetmealDishes(newList);



        if(newList != null) {
            //根据去重后的第一个setmealId查询套餐的基本信息
            Long setmealId = aLong;
            Setmeal setmeal = this.getById(setmealId);
            //对象拷贝(将套餐的基本信息拷贝到SetmealDto中)
            BeanUtils.copyProperties(setmeal,setmealDto);
        }


        return setmealDto;
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        //根据id查询套餐
        Setmeal setmeal = this.getById(id);
        //拷贝Setmeal到SetmealDto
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);
        //构造菜品套餐关联的条件查询
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        //根据套餐id查询关联的菜品
        queryWrapper.eq(SetmealDish::getSetmealId,id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        //将SetmealDto的list集合修改为含有相关菜品信息的list集合
        setmealDto.setSetmealDishes(list);

        return setmealDto;
    }

    /**
     * 更新套餐信息，同时更新对应的菜品信息
     * @param setmealDto
     */
    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        //更新setmeal表的基本信息
        this.updateById(setmealDto);
        //清理当前菜品对应口味数据
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());
        setmealDishService.remove(queryWrapper);
        //添加当前提交过来的套餐数据,并重新向setmeal_dish表插入数据
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);

    }
}
