package com.jesper.seckill.service;

import com.alibaba.druid.util.StringUtils;
import com.jesper.seckill.bean.User;
import com.jesper.seckill.exception.GlobalException;
import com.jesper.seckill.mapper.UserMapper;
import com.jesper.seckill.redis.RedisService;
import com.jesper.seckill.redis.UserKey;
import com.jesper.seckill.result.CodeMsg;
import com.jesper.seckill.util.MD5Util;
import com.jesper.seckill.util.UUIDUtil;
import com.jesper.seckill.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by jiangyunxiong on 2018/5/22.
 */
@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisService redisService;

    public static final String COOKIE_NAME_TOKEN = "token";

    /**
     * 根据手机号查找用户信息，先查Redis是否有，没有则查数据库，然后将数据库的用户信息保存到Redis，节省数据库资源，毕竟数据库不是随时读写，是有锁的
     * @param id
     * @return
     */
    public User getById(long id) {
        //对象缓存	从Redis取出该key对应的缓存信息
        User user = redisService.get(UserKey.getById, "" + id, User.class);
        if (user != null) {
            return user;
        }
        //取数据库
        user = userMapper.getById(id);
        //再存入缓存
        if (user != null) {
            redisService.set(UserKey.getById, "" + id, user);
        }
        return user;
    }

    /**
     * 典型缓存同步场景：更新密码
     */
    public boolean updatePassword(String token, long id, String formPass) {
        //取user
        User user = getById(id);
        if(user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //更新数据库
        User toBeUpdate = new User();
        toBeUpdate.setId(id);
        toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));
        userMapper.update(toBeUpdate);
        //更新缓存：先删除再插入
        redisService.delete(UserKey.getById, ""+id);
        user.setPassword(toBeUpdate.getPassword());
        redisService.set(UserKey.token, token, user);
        return true;
    }

    public String login(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        // 用户登录输入的手机号
        String mobile = loginVo.getMobile();
        // 用户登录输入的密码，经过客户端固定salt md5的加密得到的一串字符串
        String formPass = loginVo.getPassword();
        //判断手机号是否存在
        User user = getById(Long.parseLong(mobile));
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //验证密码
        String dbPass = user.getPassword();
        String saltDB = user.getSalt();
        // 注册的时候，会将客户端密码A经过MD5加密成密码B，再随机生成一个salt保存到数据库并用其再次加密密码B得到密码C，最终保存到数据库
        // 所以登录的时候要这样校验密码
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
        if (!calcPass.equals(dbPass)) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        //生成唯一id作为token
        String token = UUIDUtil.uuid();
        // 上面getById已经保存id:phone - UserModel到Redis了，还addCookie干啥呢？
        // 有两个作用，
        // 1、addCookie里面有保存token:token - UserModel，方便token直接校验合法性访问各大其它接口，而getById只是保存了登录校验。
        // 2、将token保存到Cookie，这个只适用于浏览器，如果是APP，不需要保存token到Cookie，只需要访问其它接口时头参数带token即可。
        // 而浏览器访问接口，从Cookie拿token访问接口更方便
        addCookie(response, token, user);
        return token;
    }

    /**
     * 将token做为key，用户信息做为value 存入redis模拟session
     * 同时将token存入cookie，保存登录状态
     */
    public void addCookie(HttpServletResponse response, String token, User user) {
        redisService.set(UserKey.token, token, user);
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        cookie.setMaxAge(UserKey.token.expireSeconds());
        cookie.setPath("/");//设置为网站根目录
        response.addCookie(cookie);
    }

    /**
     * 根据token获取用户信息
     */
    public User getByToken(HttpServletResponse response, String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        User user = redisService.get(UserKey.token, token, User.class);
        //延长有效期，有效期等于最后一次操作+有效期
        if (user != null) {
            addCookie(response, token, user);
        }
        return user;
    }

}
