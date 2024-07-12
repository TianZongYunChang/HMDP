package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.先进行手机号的校验
        if(RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合 返回错误信息
            return Result.fail("手机号格式错误!");
        }

        //3.符合的话直接生成验证码
        String code = RandomUtil.randomNumbers(6);

//        //4.把验证码保存到 Session 中
//        session.setAttribute("code", code);

        //4.把验证码保存到 redis 中,2分钟内有效。     并且为了跟别的业务区分开,这里需要加一个 业务前缀
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5.发送验证码返回oK
        log.debug("发送短信验证码成功,验证码:{}",code);

        return Result.ok("验证码发送成功！");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.校验手机号
        String phone = loginForm.getPhone();
        //2.如果不符合 返回错误信息
        if (RegexUtils.isPhoneInvalid(phone)) {
            //3.不一致就报错
            return Result.fail("手机号格式错误");
        }

        //3.5 校验验证码 从 session 中获取
        //Object cacheCode = session.getAttribute("code");

        //3.5 从Redis 中获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.equals(code)) {
            return Result.fail("验证码错误");
        }
        //4.一致 根据手机号查询用户 select * from tb_user where phone = ?
        User user = query().eq("phone", phone).one();
        //5.判断用户是否存在
        if(user == null) {
            //6.不存在,新建用户并且保存
            user = createUserWithPhone(phone);
        }
        //7.保存用户信息到Session中
        //session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        //7.保存用户到 Redis 中,方便做共享 Session 和 做登录状态的校验
        //7.1 随机生成 token 作为登录令牌
        String token = UUID.randomUUID().toString(true);

        //7.2 将 UserDTO 转为 HashMap 进行存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //Map<String,Object> userMap = BeanUtil.beanToMap(userDTO); 这里如果不进行设置,那么会报 java.lang.Long cannot be cast to java.lang.String
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));   //这里就是将 UserDTO 实体类中的属性转换成了 String 类型

        //7.3 将 token 和 userMap 存储到 Redis
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey ,userMap);
        //7.3 设置 token 的有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + phone,LOGIN_USER_TTL, TimeUnit.MINUTES);
        //8.返回 token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
