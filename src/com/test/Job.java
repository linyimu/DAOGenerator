package com.test;

import java.lang.reflect.Field;
import java.util.List;

public class Job {
	private int id;
	private Department dept;
	private Double salary;
	private String name;
	private Employees emp;
	//private List<Employees> emps;

	public static void main(String[] args) throws NoSuchFieldException, SecurityException {
//		Field declaredField = Job.class.getDeclaredField("dept");
//		System.out.println(declaredField.getType());
		
		StringBuffer sb = new StringBuffer();
		sb.append("package com.lym.test\n");
		sb.append("import android.content.Context;\n");
		sb.append("import android.database.DatabaseErrorHandler;\n");
		sb.append("import android.database.sqlite.SQLiteDatabase;\n");
		sb.append("import android.database.sqlite.SQLiteDatabase.CursorFactory;\n");
		
		int indexOf = sb.indexOf("\n");
		sb.insert(indexOf,"\nimport com.lym.person\n");
		System.out.println(sb.toString());
		
		System.out.println(indexOf);
	}
}
