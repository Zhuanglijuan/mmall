package com.mmall.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2018/3/25.
 */
public class TokenCache {
    private static Logger logger = LoggerFactory.getLogger(TokenCache.class);

    public static final String TOKEN_FREFIX = "token_";

    //声明一个静态的内存块
    //构建本地cache,设置缓存初始化容量1000,缓存的最大容量10000,当超过最大容量时guaua cache会使用LRU算法来移除缓存项,设置有效期为12个小时
    private static LoadingCache<String,String> localCache = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(10000).expireAfterAccess(12, TimeUnit.HOURS)
            .build(new CacheLoader<String, String>() {//匿名实现
                //默认数据加载实现，当调用get取值的时候，如果Key没有对应的值，就调用这个方法进行加载
                //即当key没有命中时，调用load()
                @Override
                public String load(String s) throws Exception {
                    return null;
                }
            });

    public static void setKey(String key,String value){
        localCache.put(key,value);
    }

    public static String getKey(String key){
        String value = null;
        try{
            value = localCache.get(key);
            if(StringUtils.equals(value,null)){
                return null;
            }
            return value;
        }catch (Exception e){
            //打印异常堆栈
            logger.error("localCache get error",e);
        }
        return null;
    }

}
