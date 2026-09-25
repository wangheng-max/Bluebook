package com.wangheng.interceptors;

import com.wangheng.utils.JwtUtil;
import com.wangheng.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 商城公开读接口（商城团购功能 v1.1）：匿名可访问。
     * 携带有效 token 时照常解析 ThreadLocal——同路径下的商家写操作（如 DELETE /product/{id}）
     * 由 @RequireMerchant 切面二次鉴权，避免白名单放行导致登录态丢失。
     */
    private static final Set<String> PUBLIC_MALL_EXACT_PATHS = Set.of(
            "/product", "/group-buy",
            "/product/search", "/product/category/list", "/product/category/tree",
            "/coupon/stock/list", "/coupon/type/list",
            "/comment/list");

    /** 数字ID路径：/product/{id}、/group-buy/{id}、/coupon/stock/{id} 的 GET */
    private static final Pattern PUBLIC_MALL_ID_PATH = Pattern.compile("^/(product|group-buy|coupon/stock)/\\d+$");

    /** 商品 SKU 列表（商城团购功能：详情页规格选择公开可读） */
    private static final Pattern PUBLIC_MALL_SKU_PATH = Pattern.compile("^/product/\\d+/sku$");

    /** 评论回复列表（评论模块：商品详情页匿名可读，登录态照常解析以标记点赞状态） */
    private static final Pattern PUBLIC_COMMENT_REPLY_PATH = Pattern.compile("^/comment/\\d+/replies$");

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token=request.getHeader("Authorization");

        //商城公开读接口：匿名放行；有效 token 照常解析（现有接口路径不受影响）
        if (isPublicMallRequest(request)) {
            if (StringUtils.hasText(token)) {
                try {
                    if (stringRedisTemplate.opsForValue().get(token) != null) {
                        ThreadLocalUtil.set(JwtUtil.parseToken(token));
                    }
                } catch (Exception ignored) {
                    // token 无效按匿名处理
                }
            }
            return true;
        }

        //令牌验证
        //验证token
        try {
            //从redis中获取相同的token
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if (redisToken==null){
                //token已经失效了
                throw new RuntimeException();
            }
            Map<String, Object> claims = JwtUtil.parseToken(token);

            //把业务数据存储到ThreadLocal中
            ThreadLocalUtil.set(claims);
            //放行
            return true;
        } catch (Exception e) {
            //http响应状态码为401
            response.setStatus(401);
            //不放行
            return false;
        }
    }

    /**
     * 是否商城公开读接口：仅 GET 方法 + 精确路径或数字ID路径
     */
    private boolean isPublicMallRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasLength(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return PUBLIC_MALL_EXACT_PATHS.contains(path)
                || PUBLIC_MALL_ID_PATH.matcher(path).matches()
                || PUBLIC_MALL_SKU_PATH.matcher(path).matches()
                || PUBLIC_COMMENT_REPLY_PATH.matcher(path).matches();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清空ThreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
