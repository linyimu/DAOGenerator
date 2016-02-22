package com.lym.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	/** 自动增长 (只能作用的int，long类型的字段上） */
	public static int KEY_GENERATOR_INCREASE = 0;
	/** 使用对象的HashCode码(只能作用的int，long类型的字段上） */
	public static int KEY_GENERATOR_HASHCODE = 1;
	/** 使用UUID ,只能作用在String类型的字段上 */
	public static int KEY_GENERATOR_UUID = 2;
	/** 自定义 */
	public static int KEY_GENERATOR_CUSTOM = 3;

	/** 是否是主键 */
	public int primaryKey() default KEY_GENERATOR_INCREASE;

}
