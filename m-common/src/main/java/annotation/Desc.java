package annotation;

import java.lang.annotation.*;

/**
 * @author : qiDing
 * Date: 2020-11-08 12:46
 * @version v1.0.0
 * Description : 由于swagger包整合太多我们用不到的功能，比如我们tcp项目并不需要它的http相关功能，
 * 所以定义此注解凑合使用
 * 本注解目前并无实际意义，只做简单属性说明，方便理解,后期可以加入文档生成相关功能
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD,ElementType.TYPE})
@Documented
public @interface Desc {

    /**
     * 属性解析
     */
    String value() default "";

}
