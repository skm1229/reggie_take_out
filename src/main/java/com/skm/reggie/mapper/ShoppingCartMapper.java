package com.skm.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skm.reggie.domain.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
}
