package com.skm.reggie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.skm.reggie.domain.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {


}
