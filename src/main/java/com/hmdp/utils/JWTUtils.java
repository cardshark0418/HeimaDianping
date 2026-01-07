package com.hmdp.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import com.hmdp.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JWTUtils {
    @Value("${jwt.secret}")
    private String secret;

    //生成JWT令牌
    public String generateJWT(UserDTO userDTO){
        Map<String, Object> userMap = new HashMap<>();
        //
        userMap.put("uid",userDTO.getId());
        userMap.put("nickName",userDTO.getNickName());
        userMap.put("icon",userDTO.getIcon());
        //以上都是USERDTO中的内容
        userMap.put("exp", DateUtil.offsetSecond(new Date(),20*60));//令牌有效期
        return JWTUtil.createToken(userMap, getKeyBytes());
    }

    //jwt令牌是否有效？
    public boolean isSignatureValid(String token) {
        try {
            //使用 verify() 只校验加密签名，不校验 exp 时间
            return JWT.of(token).setKey(getKeyBytes()).verify();
        } catch (Exception e) {
            log.error("JWT被篡改或格式错误: {}", e.getMessage());
            return false;
        }
    }
    //是否过期？
    public boolean isExpired(String token) {
        try {
            JWTValidator.of(token).validateDate();
            return false;
        } catch (Exception e) {
            log.warn("JWT已过期: {}", e.getMessage());
            return true;
        }
    }
    //在JWT令牌中取出USER_DTO
    public UserDTO getUserDTOFromToken(String token) {
        try {
            // 改为使用 Hutool 的 JWT 解析
            JWT jwt = JWTUtil.parseToken(token);
            UserDTO userDTO = new UserDTO();
            // 从 Hutool JWT 中获取数据
            Object uidObj = jwt.getPayload("uid");
            Long userId = convertToLong(uidObj);  // 需要添加这个转换方法

            if (userId == null) {
                log.error("无法从token解析用户ID");
                return null;
            }

            userDTO.setId(userId);
            userDTO.setNickName((String) jwt.getPayload("nickName"));
            userDTO.setIcon((String) jwt.getPayload("icon"));

            return userDTO;

        } catch (Exception e) {
            log.error("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }
    // 添加这个辅助方法
    private Long convertToLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return ((Integer) obj).longValue();
        if (obj instanceof Long) return (Long) obj;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.valueOf(obj.toString());
        } catch (NumberFormatException e) {
            log.error("转换失败: {} 无法转为Long", obj);
            return null;
        }
    }
    private byte[] getKeyBytes(){
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}
