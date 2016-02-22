#DAOGenerator
	##这是一个工具类，可以通过JavaBean自动生成相应的数据库操作类，实现JavaBean的增删改查操作。
####例如：
*Student.java
```
package com.lym.bean;

import java.util.List;

public class Student {

	private int id;
	private String name;
	private int age;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "Student [id=" + id + ", name=" + name + ", age=" + age + "]";
	}

}
```
*Teacher.java
```
package com.lym.bean;

import java.util.List;

public class Teacher {
	private int id;
	private String name;

	public int getId() {
		
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Teacher [id=" + id + ", name=" + name + "]";
	}

}
```
*Cource.java

```
package com.lym.bean;

/**
 * 课程
 * 
 * @author xuyao
 * 
 */
public class Cource {

	private int id;
	private String name;
	private Teacher teacher;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Teacher getTeacher() {
		// TeacherDao td = new TeacherDao(context);
		return teacher;
	}

	public void setTeacher(Teacher reacher) {
		this.teacher = reacher;
	}

	@Override
	public String toString() {
		return "Cource [id=" + id + ", name=" + name + ", teacher=" + teacher
				+ "]";
	}

}

```

*Score.java
```
package com.lym.bean;

import java.util.List;

/**
 * 选课成绩
 * 
 * @author xuyao
 * 
 */
public class Score {
	private int id;
	private Cource cource;
	private Student student;
	private int score;

	//private List<Teacher> teacher;
	//private Teacher teacher;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Cource getCource() {
		return cource;
	}

	public void setCource(Cource cource) {
		this.cource = cource;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return "Score [id=" + id + ", curce=" + cource + ", student=" + student
				+ ", score=" + score + "]";
	}
}

```
然后使用该工具类：
```
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
	}
}

```

执行即可生成相应的Dao文件和DaoHelper类，这些类实现了数据库相关的操作方法。
