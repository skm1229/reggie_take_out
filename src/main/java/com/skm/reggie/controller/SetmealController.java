package com.skm.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skm.reggie.common.R;
import com.skm.reggie.domain.Category;
import com.skm.reggie.domain.Dish;
import com.skm.reggie.domain.Setmeal;
import com.skm.reggie.domain.SetmealDish;
import com.skm.reggie.dto.DishDto;
import com.skm.reggie.dto.SetmealDto;
import com.skm.reggie.service.CategoryService;
import com.skm.reggie.service.DishService;
import com.skm.reggie.service.SetmealDishService;
import com.skm.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    /**
     * 新增套餐
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息为： {}",setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功~~");
    }

    /**
     * 查询套餐信息
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name) {
        log.info("page: {},pageSize: {},name: {}",page,pageSize,name);
        //分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>();

        //添加查询条件，根据name，进行like模糊查询
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null,Setmeal::getName,name);
        //添加排序条件，根据更新世界降序排列
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(pageInfo,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map((item) ->{
            SetmealDto setmealDto = new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(item,setmealDto);
            //分类id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null) {
                //分类名称
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());

        setmealDtoPage.setRecords(list);


        return R.success(setmealDtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功~~");
    }

    /**
     * 更据iid查询套餐信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Cacheable(value = "setmealCache",key = "#id")
    public R<SetmealDto> get(@PathVariable("id") Long id) {
        log.info("id = {}",id);
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    /**
     * 更新套餐状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = {"setmealCache","setmealDishCache"} , allEntries = true)
    public R<String> switchStatus(@PathVariable("status") int status,@RequestParam List<Long> ids) {
        //条件构造器
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Setmeal::getId,ids);
        //如果菜品为起售状态,将菜品更改为停售状态
        if(status == 0) {
            updateWrapper.set(Setmeal::getStatus,0);
            setmealService.update(updateWrapper);
        }
        //如果菜品为停售状态,将菜品更改为起售状态
        if(status == 1) {
            updateWrapper.set(Setmeal::getStatus,1);
            setmealService.update(updateWrapper);
        }
        log.info("status = {}, ids = {}",status,ids);
        return R.success("更新状态成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
   @GetMapping("/list")
   @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_' + #setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 根据套餐id查询相应菜品表
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @Cacheable(value = "setmealDishCache",key = "#id")
    @Transactional
    public R<List<DishDto>> getDishBySetmeal(@PathVariable("id")Long id) {
        //条件构造器
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        //根据套餐id查询套餐菜品相关信息
        queryWrapper.eq(SetmealDish::getSetmealId,id);
        queryWrapper.orderByDesc(SetmealDish::getUpdateTime);
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
        log.info("菜品 = {}",setmealDishList);
        //根据每一个dishId查询套餐

//        List<DishDto> dishDtoList= setmealDishList.stream().map((item) -> {
//            DishDto dishDto = new DishDto();
//
//            //查询每一份菜品的价格并封装到DishDto
//            dishDto.setCopies(item.getCopies());
//
//            //根据套餐中的菜品id查询菜品的详细信息
//            Long dishId = item.getDishId();
//            Dish dish = dishService.getById(dishId);
//
//
//            //对象拷贝
//            BeanUtils.copyProperties(dish,dishDto);
//            return dishDto;
//        }).collect(Collectors.toList());


        //根据每一个dishId查询套餐
        List<DishDto> dishDtoList = setmealDishList.stream().map((item) ->{
            //根据套餐中的菜品id查询菜品的详细信息
            Long dishId = item.getDishId();
            //调用dishService的方法返回一个DishDto对象
            DishDto dishDto = dishService.getByIdWithFlavor(dishId);
            //查询每一份菜品的价格并封装到DishDto
            dishDto.setCopies(item.getCopies());
            return dishDto;
        }).collect(Collectors.toList());
        return R.success(dishDtoList);
    }

    /**
     * 修改套餐
     * @param setmealDto
     * @return
     */
    @PutMapping
    @CacheEvict(value = {"setmealCache","setmealDishCache"},allEntries = true)
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        //先判断是否接收到数据
        if(setmealDto==null) {
            return  R.error("请求异常");
        }
        //判断套餐下面是否还有关联菜品
        if(setmealDto.getSetmealDishes()==null)
        {
            return R.error("套餐没有菜品，请添加");
        }
        //更新相关信息
        setmealService.updateWithDish(setmealDto);

        return R.success("修改套餐信息成功~~");


    }

}
