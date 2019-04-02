package com.mmall.service.impl;

import com.mmall.common.ServerResponse;
import com.mmall.dao.OrderMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.dao.UserMapper;
import com.mmall.service.IStatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: zhuanglj
 * @create: 2019-04-02 14:09
 */
@Service("iStatisticService")
public class StatisticServiceImpl implements IStatisticService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse baseCount() {

        int orderCount = orderMapper.selectOrderCount();
        int userCount = userMapper.selectUserCount();
        int productCount = productMapper.selectProductCount();

        Map<String, Integer> baseCount = new HashMap<>();
        baseCount.put("userCount", userCount);
        baseCount.put("productCount", productCount);
        baseCount.put("orderCount", orderCount);

        return ServerResponse.createBySuccess(baseCount);
    }
}
