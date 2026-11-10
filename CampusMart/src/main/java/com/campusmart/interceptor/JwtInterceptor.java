package com.campusmart.interceptor;

import com.campusmart.common.TokenException;
import com.campusmart.common.UserContext;
import com.campusmart.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** Controller 方法执行之前调用 */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 静态资源等非 Controller 请求直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new TokenException("请先登录");
        }

        // 解析失败会抛 TokenException，由 GlobalExceptionHandler 转成 401
        Long userId = jwtUtil.getUserId(auth.substring(7));
        UserContext.set(userId);
        return true;
    }

    /** 请求完全结束后调用，无论成功还是异常 */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}
