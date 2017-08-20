package com.codedak.hessian.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface HessianService {
	  /**
	   * 注解实现的接口
	   * @return
	   */
	  Class<?> interfaces() default Class.class;
	  /**
	   * 发布的hessian服务名
	   * @return
	   */
	  String name() default "";
}
