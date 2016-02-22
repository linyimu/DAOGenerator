package com.test;

import java.util.ArrayList;
import java.util.List;

import com.lym.bean.Cource;
import com.lym.bean.Score;
import com.lym.bean.Student;
import com.lym.bean.Teacher;
import com.test.linyimu.DBCodeGenerator;

public class Main {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DBCodeGenerator instence = DBCodeGenerator.getInstence();
		instence.addClass(Cource.class);
		instence.addClass(Score.class);
		instence.addClass(Student.class);
		instence.addClass(Teacher.class);
		instence.setPackage("com.lym.dao");
		instence.setPrimaryKeyType("int");
		instence.execute();

		Object ts = new ArrayList<Student>();
		List tts = (List) ts;
		tts.add(new Student());
		Object object = tts.get(0);
		System.out.println(object instanceof Student);
		// if (ts instanceof List<?>) {
		// System.out.println(true);
		// } else {
		// System.out.println(false);
		// }
		// for (Student stu : ts) {
		//
		// }
	}
}
