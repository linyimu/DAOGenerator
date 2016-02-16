package com.lym.dao.bean;

import java.io.Serializable;

/**
 * 实体间的关系
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
	 * 主键
	 */
	private Class primaryClass;
	/** 外键 */
	private Class foreignClass;

	private int type;

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

	@Override
	public Relation clone() {
		try {
			return (Relation) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
