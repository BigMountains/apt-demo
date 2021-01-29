package com.czl.apt;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表明该Getter注解只能被标注在类上
 */
@Target(value = ElementType.TYPE)
/**
 * 表明该Getter注解将被编译器编译但不会存在JVM运行时
 */
@Retention(RetentionPolicy.CLASS)
public @interface Getter
{

}
