package com.test;

import com.test.linyimu.DBCodeGenerator;

public class Main {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DBCodeGenerator instence = DBCodeGenerator.getInstence();
		instence.addClass(Employees.class);
		instence.addClass(Department.class);
		instence.addClass(Job.class);
		instence.setPackage("com.lym.dao");
		instence.execute();
	}
}
