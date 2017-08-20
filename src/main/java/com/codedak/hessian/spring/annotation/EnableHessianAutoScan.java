package com.codedak.hessian.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.codedak.hessian.spring.HessianAutoScanRegistrar;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(HessianAutoScanRegistrar.class)
public @interface EnableHessianAutoScan {
	  /**
	   * 扫描的包名
	   * @return
	   */
	  String[] basePackages() default {};
      /**
       * 扫描的包名 类名集合
       * @return
       */
	  Class<?>[] basePackageClasses() default {};
      /**
       * 客户端类集合
       * @return
       */
	  Class<?>[] clients() default {};
	  
	  /**
	   * 服务端集合
	   * @return
	   */
      Class<?>[] services() default {};
}
