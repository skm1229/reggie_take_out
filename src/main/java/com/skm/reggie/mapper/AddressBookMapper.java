package com.skm.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skm.reggie.domain.AddressBook;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {
}
