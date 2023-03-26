package com.skm.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skm.reggie.common.R;
import com.skm.reggie.domain.Category;
import com.skm.reggie.domain.Dish;
import com.skm.reggie.domain.DishFlavor;
import com.skm.reggie.dto.DishDto;
import com.skm.reggie.service.CategoryService;
import com.skm.reggie.service.DishFlavorService;
import com.skm.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "dishCache",allEntries = true)
    public R<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        //清理某个分类下的菜品数据
        //String key = "dish_" + dishDto.getCategoryId() + "_1";
        //redisTemplate.delete(key);

        return R.success("新增菜品成功~~");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name) {
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
        //排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //对象拷贝 除了封装数据的records(封装着Dish集合的每一条数据)
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        //单独将Dish集合拿出来，转成DishDto集合
        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) ->{
            //将Dish集合拷贝到DishDto集合
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);
            //分类id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象,将对象中的categoryName赋值给DishDto类，最后封装成集合
            Category category = categoryService.getById(categoryId);
            if(category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        //将封装好后的List<DishDto> list 赋值给dishDtoPage的records
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Cacheable(value = "dishCache",key = "#id")
    public R<DishDto> get(@PathVariable("id") Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    @CacheEvict(value = "dishCache",key = "#dishDto.categoryId + '_' + #dishDto.status")
    public R<String> update(@RequestBody DishDto dishDto) {
        //先判断是否接收到数据
        if(dishDto == null) {
            return  R.error("请求异常");
        }
        dishService.updateWithFlavor(dishDto);

        //清理所有菜品的缓存数据
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);
        //清理某个分类下的菜品数据
        //String key = "dish_" + dishDto.getCategoryId() + "_1";
        //redisTemplate.delete(key);
        return R.success("更新菜品成功~~");
    }

    /**
     * 根据条件查询对应的菜品信息
     * @param dish
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "dishCache",key = "#dish.categoryId + '_' + #dish.status")
    public R<List<DishDto>> list(Dish dish) {
//        List<DishDto> dishDtoList = null;
//        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();  //dish_138924232332_1
//        //先从Redis中获取缓存数据
//        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
//
//        if(dishDtoList != null) {
//            //如果存在，直接返回，无需查询数据库
//            return R.success(dishDtoList);
//        }
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //状态为1(起售状态)
        queryWrapper.eq(Dish::getStatus,1);
        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        List<DishDto> dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item,dishDto);
            //分类id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象,将对象中的categoryName赋值给DishDto类，最后封装成集合
            Category category = categoryService.getById(categoryId);
            if(category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //当前菜品的id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //如果不存在，将数据存入Redis
        //redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);
        return R.success(dishDtoList);
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        dishService.removeWithFlavor(ids);
        return R.success("删除菜品成功~~");
    }

    /**
     * 更新菜品状态
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "dishCache",allEntries = true)
    public R<String> switchStatus(@PathVariable("status") int status,@RequestParam List<Long> ids) {
        //条件构造器
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId,ids);
        //如果菜品为起售状态,将菜品更改为停售状态
        if(status == 0) {
            updateWrapper.set(Dish::getStatus,0);
            dishService.update(updateWrapper);
        }
        //如果菜品为停售状态,将菜品更改为起售状态
        if(status == 1) {
            updateWrapper.set(Dish::getStatus,1);
            dishService.update(updateWrapper);
        }


        log.info("status = {}, ids = {}",status,ids);
        return R.success("更新状态成功");
    }
}
