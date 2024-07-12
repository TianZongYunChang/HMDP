package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result queryTypeList() {
        //1.先在 Redis 中进行店铺类型的查询
        String key = "shop-type:";
        ListOperations<String, String> listOps = stringRedisTemplate.opsForList();
        List<String> shopType = listOps.range(key, 0, -1);
        List<ShopType> shopTypeList = new ArrayList<>();
        //2.如果存在,直接返回
        if(shopType != null && !shopType.isEmpty()){
            for (String s : shopType) {
                shopTypeList.add(JSONUtil.toBean(s, ShopType.class));
            }
            System.out.println(shopTypeList);
            return Result.ok(shopTypeList);
        }

        //3.如果不存在,在Mysql中查询
        List<ShopType> typeList = query().orderByAsc("sort").list();

        //4.mysql中不存在,报错
        if(typeList == null ){
            return Result.fail("数据相应失败！");
        }

        //5.存在的话 先存在 Redis 中
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        for (int i = 0; i < typeList.size(); i++) {
            //把每一条数据都放在redis
            listOperations.rightPush(key, JSONUtil.toJsonStr(typeList.get(i)));
        }

        //6.返回数据
        return Result.ok(typeList);
    }
}
