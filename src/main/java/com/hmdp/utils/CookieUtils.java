package com.hmdp.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
public class CookieUtils {
    public void setSecureCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(false)        // 禁止 JS 读取，防 XSS
                .secure(false)          // 仅在 HTTPS 下传输 (本地测试可设为 false)
                .path("/")             // 全局生效
                .maxAge(maxAge)        // 过期时间(sec)
                .sameSite("Lax")       // 防范 CSRF
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
