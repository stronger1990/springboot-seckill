package com.jesper.seckill.mapper;

import com.jesper.seckill.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Created by jiangyunxiong on 2018/5/21.
 * 这里mapper的用法和newbee-mall-master不一样，感觉这种更适合我的风格，比较干练
 */
@Mapper
public interface UserMapper {

    @Select("select * from sk_user where id = #{id}")
    public User getById(@Param("id")long id);

    @Update("update sk_user set password = #{password} where id = #{id}")
    public void update(User toBeUpdate);
}
