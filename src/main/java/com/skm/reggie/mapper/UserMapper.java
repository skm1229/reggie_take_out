package com.skm.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skm.reggie.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
