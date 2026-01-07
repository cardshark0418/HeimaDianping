package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.CookieUtils;
import com.hmdp.utils.JWTUtils;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
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
    @Resource
    private JWTUtils jwtUtils;
    @Resource
    private CookieUtils cookieUtils;
    //发送验证码
    @Override
    public Result sendCode(String phone) {
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不符合则返回错误信息
            return Result.fail("手机号错误！");
        }

        //符合则生成验证码
        String code = RandomUtil.randomNumbers(4);
        //保存验证码和手机号到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set("login:phone:"+phone,phone,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("验证码已发送：{}",code);

        return Result.ok();
    }

    //登录
    @Override
    public Result login(LoginFormDTO loginForm, HttpServletResponse response) {
        //检查手机号是否发生变动
        String phone = loginForm.getPhone();
        if(!(phone.equals(stringRedisTemplate.opsForValue().get("login:phone:"+phone)))){
            return Result.fail("手机号不一致！");
        }
        //检查验证码是否符合
        if(!(loginForm.getCode().equals(stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone)))){
            return Result.fail("验证码错误！");
        }
        //检查数据库中是否有该用户
        User user = query().eq("phone", phone).one();
        //若没有 则注册新用户 保存到数据库
        if(user == null){
             user = creatUserWithPhone(phone);
        }
        // 若有 则登录 并保存用户到Redis 并返回Token
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //随机生成jwtTOKEN
        String token = jwtUtils.generateJWT(userDTO);
        //生成UUID为refreshToken
        String rt = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set("login:rt:"+rt,user.getId().toString(),7,TimeUnit.DAYS);
        cookieUtils.setSecureCookie(response,"AT",token,60*60*24*7);
        cookieUtils.setSecureCookie(response,"RT",rt,60*60*24*7);

        return Result.ok();
    }

    @Override
    public Result sign() {
        return null;
    }

    private User creatUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(8));
        save(user);
        return user;
    }
}
