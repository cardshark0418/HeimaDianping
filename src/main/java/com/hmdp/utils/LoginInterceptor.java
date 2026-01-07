package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.util.WebUtils.getCookie;

public class LoginInterceptor implements HandlerInterceptor{
    private final StringRedisTemplate stringRedisTemplate;
    private final JWTUtils jwtUtils;
    private final CookieUtils cookieUtils;
    public LoginInterceptor(StringRedisTemplate stringRedisTemplate,JWTUtils jwtUtils,CookieUtils cookieUtils){
        this.stringRedisTemplate=stringRedisTemplate;
        this.jwtUtils=jwtUtils;
        this.cookieUtils=cookieUtils;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,@NonNull Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull Object handler) throws Exception {
        Cookie atCookie = getCookie(request, "AT");
        Cookie rtCookie = getCookie(request, "RT");
        //若RT或AT为空 直接拒绝
        if(rtCookie ==null || atCookie ==null){
            response.setStatus(401);
            return false;
        }
        // 1. 从 Cookie 拿到 AT 和 RT
        String at = atCookie.getValue();
        String rt = rtCookie.getValue();
        //若jwt无效 不放行
        boolean signatureValid = jwtUtils.isSignatureValid(at);
        if(!signatureValid){
            response.setStatus(401);
            return false;
        }
        //取出userDTO
        UserDTO userDTO = jwtUtils.getUserDTOFromToken(at);
        //若未过期
        if(!jwtUtils.isExpired(at)){
            //保存到ThreadLocal 放行
            UserHolder.saveUser(userDTO);
            return true;
        }
        else{//若过期了
            //rt是否有效？
            String redisUserId = stringRedisTemplate.opsForValue().get("login:rt:"+rt);
            if(redisUserId!=null && redisUserId.equals(userDTO.getId().toString())){
                issueAccessToken(userDTO,response);
                return true;
            }
        }
        //说明RT无效
        response.setStatus(401);
        return false;
    }
    private void issueAccessToken(UserDTO userDTO,HttpServletResponse response){
        //若有效 颁发新的at
        String newAT = jwtUtils.generateJWT(userDTO);
        cookieUtils.setSecureCookie(response,"AT",newAT,60*60*24*7);
        UserHolder.saveUser(userDTO);
    }
}
