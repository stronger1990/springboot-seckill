package com.jesper.seckill.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Created by jiangyunxiong on 2018/5/22.
 * <p>
 * 自定义手机格式校验注解
 */
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = { IsMobileValidator.class }) // 引进校验器
public @interface IsMobile {
	// 下面的参数可以理解成提供给IsMobileValidator.class使用的变量
	boolean required() default true;// 默认不能为空

	String message() default "手机号码格式错误";// 校验不通过输出信息

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
