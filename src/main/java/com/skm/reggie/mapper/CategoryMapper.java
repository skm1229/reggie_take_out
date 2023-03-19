package com.skm.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skm.reggie.domain.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
