package com.lym.dao.bean;

import java.util.ArrayList;
import java.util.List;

public class TableModel {

	/** 表对应的类 */
	private Class clazz;
	private String tableName;
	private List<ColumnModel> columModels;

	private ColumnModel primaryKey;

	public Class getClazz() {
		return clazz;
	}

	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}

	/** 是否是链接表 */
	private boolean joinTable = false;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public boolean isJoinTable() {
		return joinTable;
	}

	public void setJoinTable(boolean joinTable) {
		this.joinTable = joinTable;
	}

	public List<ColumnModel> getColumModels() {
		return columModels;
	}

	public void setColumModels(List<ColumnModel> columModels) {
		this.columModels = columModels;
	}

	public void addColumnModel(ColumnModel model) {
		if (columModels == null) {
			columModels = new ArrayList<ColumnModel>();
		}
		columModels.add(model);
	}

	public ColumnModel getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(ColumnModel primaryKey) {
		this.primaryKey = primaryKey;
	}

	@Override
	public String toString() {
		return "TableModel [tableName=" + tableName + ", columModels="
				+ columModels + ", primaryKey=" + primaryKey + ", joinTable="
				+ joinTable + "]";
	}

}
