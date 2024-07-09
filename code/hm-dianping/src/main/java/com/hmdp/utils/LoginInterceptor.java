package com.hmdp.utils;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import io.netty.util.internal.StringUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 由于这个类是我们自己进行创建的类,那么我们在 注入对象 的时候只能使用构造方法进行注入
     *
     */

    private StringRedisTemplate stringRedisTemplate;
    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取 session
        //HttpSession session = request.getSession();

        //1.从请求头中获取 token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            //请求头中不存在,说明用户还没有登录,直接进行拦截
            response.setStatus(401);
            return false;
        }

        //2.获取 session 中的用户
       // Object user = session.getAttribute("user");

        // 2.基于 token 获取 redis 中的用户
        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        //3.判断当前用户是否在 session 中存在
        if(userMap.isEmpty()) {
            //4.不存在就直接拦截
            response.setStatus(401);
            return false;
        }

        //5. 将查询到的 userMap 转化为 UserDTO 对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //6. 将转化后的 UserDTO 保存在 ThreadLocal 中 方便各个服务去进行使用
        UserHolder.saveUser(userDTO);

        //7.刷新 token 的有效期
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        //8.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
