package com.jesper.seckill.controller;

import com.jesper.seckill.result.Result;
import com.jesper.seckill.service.UserService;
import com.jesper.seckill.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


/**
 * 登录相关
 */
@Controller
@RequestMapping("/login")
public class LoginController {

    private static Logger log = LoggerFactory.getLogger(LoginController.class);

    /// 这里直接写service的实现，而不是分成service和impl，也有好处就是少写一个文件，简单直接，但是不够松耦合，而且实现的方法会直接给人看到，失去"封装性"
    @Autowired
    UserService userService;

    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";
    }

    /// 首先login.html的mobile属性本来就会自己先判断一下输入是否合法，然后再加上输入参数的@Valid判断，基本上输入的应该是手机号码了
    @RequestMapping("/do_login")
    @ResponseBody
    public Result<String> doLogin(HttpServletResponse response, @Valid LoginVo loginVo) {//加入JSR303参数校验
        log.info(loginVo.toString());
        String token = userService.login(response, loginVo); 
        return Result.success(token); // 因为上面@Valid，如果post的参数不符合不会执行到这里直接返回error，所以执行到这里肯定是合法的了
    }

}
