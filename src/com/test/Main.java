package com.test;

import com.lym.ainao.bean.AlarmClock;
import com.lym.ainao.bean.AlarmClockTheme;
import com.lym.ainao.bean.User;
import com.test.linyimu.DBCodeGenerator;

public class Main {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 DBCodeGenerator instence = DBCodeGenerator.getInstence();
		 instence.addClass(AlarmClock.class, AlarmClockTheme.class,
		 User.class);
		 instence.setPackage("com.lym.ainao.dao");
		 instence.setPrimaryKeyType("String");
		 instence.execute2();
		
//		long currentTimeMillis = System.currentTimeMillis();
//		System.out.println(currentTimeMillis);
//		
//		int n = (int) currentTimeMillis;
//		System.out.println(n);

	}

}
