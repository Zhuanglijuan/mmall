package com.mmall.service;

import com.mmall.common.ServerResponse;

import java.util.Map;

/**
 * Created by Zhuang on 2018/5/27.
 */
public interface IOrderService {
    public ServerResponse pay(Long orderNo, Integer userId, String path);

    public ServerResponse aliCallback(Map<String,String> params);

    public ServerResponse queryOrderPayStatus(Integer userId,Long orderNo);
}
