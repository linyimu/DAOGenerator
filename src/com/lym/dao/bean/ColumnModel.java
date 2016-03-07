/*
 * Copyright (C)  Tony Green, LitePal Framework Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lym.dao.bean;

/**
 * This is a model class for columns. It stores column name, column type, and
 * column constraints information.
 * 
 * @author Tony Green
 * @since 1.3
 */
public class ColumnModel {

	/**
	 * Name of column.
	 */
	private String columnName;

	/**
	 * 列对应的java类型
	 */
	private String columnJavaType;

	/**
	 * 列对应的数据库类型
	 */
	private String columnType;

	// 是否要引用其他类
	private boolean isRefrenceOther;

	// 引用的类
	private Class refrenceClass;

	// 引用的类型（1：1，1：n，n:1,n:n)
	private int refrenceType;

	// 是否是集合
	private boolean isSet;

	// 是否就是所在类的字段
	private boolean isLocal;

	/**
	 * 是否是主键
	 */
	private boolean isPrimaryKey;

	// private

	/**
	 * Nullable constraint.
	 */
	private boolean isNullable = true;

	/**
	 * Unique constraint.
	 */
	private boolean isUnique = false;

	/**
	 * Default constraint.
	 */
	private String defaultValue = "";

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getColumnType() {
		return columnType;
	}

	public void setColumnType(String columnType) {
		this.columnType = columnType;
	}

	public boolean isLocal() {
		return isLocal;
	}

	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}

	public int getRefrenceType() {
		return refrenceType;
	}

	public void setRefrenceType(int refrenceType) {
		this.refrenceType = refrenceType;
	}

	public boolean isSet() {
		return isSet;
	}

	public void setSet(boolean isSet) {
		this.isSet = isSet;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public void setIsNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}

	public boolean isUnique() {
		return isUnique;
	}

	public void setIsUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}

	public void setPrimaryKey(boolean isPrimaryKey) {
		this.isPrimaryKey = isPrimaryKey;
	}

	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}

	public void setUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}

	public String getColumnJavaType() {
		return columnJavaType;
	}

	public void setColumnJavaType(String columnJavaType) {
		this.columnJavaType = columnJavaType;
	}

	public boolean isRefrenceOther() {
		return isRefrenceOther;
	}

	public void setRefrenceOther(boolean isRefrenceOther) {
		this.isRefrenceOther = isRefrenceOther;
	}

	public Class getRefrenceClass() {
		return refrenceClass;
	}

	public void setRefrenceClass(Class refrenceClass) {
		this.refrenceClass = refrenceClass;
	}

	public void setDefaultValue(String defaultValue) {
		if ("text".equalsIgnoreCase(columnType)) {
			if (defaultValue != null && "".equals(defaultValue)) {
				this.defaultValue = "'" + defaultValue + "'";
			}
		} else {
			this.defaultValue = defaultValue;
		}
	}

	@Override
	public String toString() {
		return "ColumnModel [columnName=" + columnName + ", columnJavaType="
				+ columnJavaType + ", columnType=" + columnType
				+ ", isRefrenceOther=" + isRefrenceOther + ", refrenceClass="
				+ refrenceClass + ", isSet=" + isSet + ", isLocal=" + isLocal
				+ ", isPrimaryKey=" + isPrimaryKey + ", isNullable="
				+ isNullable + ", isUnique=" + isUnique + ", defaultValue="
				+ defaultValue + "]";
	}

}
