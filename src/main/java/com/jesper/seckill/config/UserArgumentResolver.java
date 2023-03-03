package com.jesper.seckill.config;

import com.alibaba.druid.util.StringUtils;
import com.jesper.seckill.bean.User;
import com.jesper.seckill.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by jiangyunxiong on 2018/5/22.
 */
@Service
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    UserService userService;

    /**
     * 访问接口，分三种，
     * 1、实际传入参数(即真正要客户端提供的参数)，
     * 比如LoginController的doLogin接口，参数LoginVo里的参数就是接纳客户端的参数
     * 2、不想在函数内创建实例，而是在参数里实例化空model，然后再在函数内一一实例化，然后用于渲染下个页面上用
     * 比如GoodsController里的list接口，参数Model
     * 3、接口本身也写了输入参数，没有具体实例化的地方，但是函数内直接使用，而且有内容，肯定是在外部哪实例化过了，能在哪呢，只有一个地方就是这里。
     * 比如GoodsController里的list接口，参数User。
     *
     * 这里判断参数如果是User，就进行resolveArgument处理，实例化User
     * 当参数类型为User才做处理
     *
     * @param methodParameter
     * @return
     */
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        //获取参数类型
        Class<?> clazz = methodParameter.getParameterType();
        return clazz == User.class;
    }

    /**
     * 思路：先获取到已有参数HttpServletRequest，从中获取到token，再用token作为key从redis拿到User，而HttpServletResponse作用是为了延迟有效期
     */
    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        HttpServletRequest request = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = nativeWebRequest.getNativeResponse(HttpServletResponse.class);

        // 登录成功后，保存了部分数据到cookie里了
        String cookieToken = getCookieValue(request, UserService.COOKIE_NAME_TOKEN);
        // 访问接口，除了登录接口外都要携带token，如果request没有token，则访问不合法
        String paramToken = request.getParameter(UserService.COOKIE_NAME_TOKEN);
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return null;
        }
        // 浏览器登录有cookieToken，APP登录有paramToken，哪个有就用哪个
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        // 返回的实例，就是supportsParameter支持的model，在访问接口时，先进入此处配置，实例化User后才进入接口内部业务逻辑处理
        return userService.getByToken(response, token);
    }

    //遍历所有cookie，找到需要的那个cookie
    private String getCookieValue(HttpServletRequest request, String cookiName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length <= 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookiName)) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
