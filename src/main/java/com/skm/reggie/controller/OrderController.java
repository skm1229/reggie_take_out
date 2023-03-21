package com.skm.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.skm.reggie.common.BaseContext;
import com.skm.reggie.common.CustomException;
import com.skm.reggie.common.R;
import com.skm.reggie.domain.OrderDetail;
import com.skm.reggie.domain.Orders;
import com.skm.reggie.dto.OrdersDto;
import com.skm.reggie.service.OrderDetailService;
import com.skm.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 根据用户查询订单
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> page(int page,int pageSize) {
        log.info("查询用户订单~~");
        //构造分页构造器对象
        Page<Orders> ordersPage = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();

        //根据用户查询订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        orderService.page(ordersPage,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(ordersPage,ordersDtoPage,"records");
        List<Orders> records = ordersPage.getRecords();
        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item,ordersDto);

            //获取订单号
            String orderId = item.getNumber();
            //根据订单号查询对应的订单细节信息
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId,orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailLambdaQueryWrapper);

            //设置OrdersDto的订单详细信息
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);

        return R.success(ordersDtoPage);
    }

    /**
     * 订单明细
     * @param page
     * @param pageSize
     * @param number
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime, String endTime) {
        //构造分页构造器对象
        Page<Orders> ordersPage = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>();
        log.info("beginTime: {}",beginTime);
        log.info("endTime: {}",endTime);

        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(number != null,Orders::getNumber,number);
        //查询日期在某个区间 ge 大于 le 小于
        queryWrapper.ge(beginTime != null,Orders::getOrderTime,beginTime);
        queryWrapper.le(endTime != null,Orders::getCheckoutTime,endTime);
        //跟前订单创建时间排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //查看所有订单信息
        orderService.page(ordersPage,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(ordersPage,ordersDtoPage,"records");
        List<Orders> records = ordersPage.getRecords();
        List<OrdersDto> list = records.stream().map((item) ->{
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item,ordersDto);

            //获取订单号
            String orderId = item.getNumber();
            //根据订单号查询对应的订单细节信息
            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId,orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailLambdaQueryWrapper);

            //设置OrdersDto的订单详细信息
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);
        return R.success(ordersDtoPage);
    }

    /**
     * 修改订单状态
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Orders orders) {
        log.info("修改订单信息: {}", orders);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //获取订单号
        Long ordersId = orders.getId();
        queryWrapper.eq(Orders::getId,ordersId);
        //获取当前订单状态
        Integer status = orders.getStatus();
        if(status == 3) {
            //订单待派送
            queryWrapper.eq(Orders::getStatus,2);
        }
        if(status == 4) {
            //订单已派送
            queryWrapper.eq(Orders::getStatus,3);
        }


        Orders ordersOne = orderService.getOne(queryWrapper);
        if(ordersOne == null){
            throw new CustomException("订单已完成，无需修改");
        }
        ordersOne.setStatus(orders.getStatus());
        orderService.updateById(ordersOne);
        return R.success("修改订单状态成功~~");
    }
}
