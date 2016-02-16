package com.lym.dao.bean;

import java.util.List;

public class TableModel {
	private Class tableClass;
	private String tableName;
	private List<ColumnModel> columModels;

	
	
	public Class getTableClass() {
		return tableClass;
	}

	public void setTableClass(Class tableClass) {
		this.tableClass = tableClass;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public List<ColumnModel> getColumModels() {
		return columModels;
	}

	public void setColumModels(List<ColumnModel> columModels) {
		this.columModels = columModels;
	}

}
