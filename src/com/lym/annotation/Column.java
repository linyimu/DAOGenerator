package com.lym.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
	/** �Զ����� (ֻ�����õ�int��long���͵��ֶ��ϣ� */
	public static int KEY_GENERATOR_INCREASE = 0;
	/** ʹ�ö����HashCode��(ֻ�����õ�int��long���͵��ֶ��ϣ� */
	public static int KEY_GENERATOR_HASHCODE = 1;
	/** ʹ��UUID ,ֻ��������String���͵��ֶ��� */
	public static int KEY_GENERATOR_UUID = 2;
	/** �Զ��� */
	public static int KEY_GENERATOR_CUSTOM = 3;

	/** �Ƿ������� */
	public int primaryKey() default KEY_GENERATOR_INCREASE;

}
