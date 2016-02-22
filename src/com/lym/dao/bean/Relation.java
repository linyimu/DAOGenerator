package com.lym.dao.bean;

import java.io.Serializable;

/**
 * ʵ���Ĺ�ϵ
 * 
 * @author xuyao
 * 
 */
public class Relation implements Cloneable {

	public final static int RELATION_NONE = -1;
	public final static int ONE_TO_ONE = 1;
	public final static int ONE_TO_MORE = 2;
	public final static int MORE_TO_ONE = 3;
	public final static int MORE_TO_MORE = 4;

	/**
	 * �������ֶ����ڵ���
	 */
	private Class primaryClass;
	/** ������ֶε����ͻ��߷��� */
	private Class foreignClass;

	private int type;
	/** �������ֶ��� */
	private String fieldName;

	public Class getPrimaryClass() {
		return primaryClass;
	}

	public void setPrimaryClass(Class primaryClass) {
		this.primaryClass = primaryClass;
	}

	public Class getForeignClass() {
		return foreignClass;
	}

	public void setForeignClass(Class foreignClass) {
		this.foreignClass = foreignClass;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public Relation clone() {
		try {
			return (Relation) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		return "Relation [primaryClass=" + primaryClass + ", foreignClass="
				+ foreignClass + ", type=" + type + ", fieldName=" + fieldName
				+ "]";
	}

}
