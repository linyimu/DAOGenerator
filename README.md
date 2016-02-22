#DAOGenerator
## 这是一个工具类，可以通过JavaBean自动生成相应的数据库操作类，实现JavaBean的增删改查操作。
#### 使用示例：
* Student.java
```Java
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
* Teacher.java
```Java
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
* Cource.java

```Java
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

* Score.java
```Java
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
```Java
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
		instence.setPrimaryKeyType("String");
		instence.execute();
	}
}

```

执行即可生成相应的Dao文件和DaoHelper类，这些类实现了数据库相关的操作方法。
## 注意事项
* 该工具可以根据JavaBean创建关联关系
* 在类的关联关系中，两个类中只能存在一种关联关系
    例如：在Cource中只能存在Teacher t;或者List<Teacher> ts;不能两者都存在。
* 在使用该工具的时候，关联关系涉及到的类都要使用addClass(clazz)加入，否者在编译时可能会报错，并且执行查找时不能得到没有加入的类的关联对象，即会认为它们没有关系。
    例如：如果在没有instence.addClass(Teacher.class);语句，则在查找Cource时，不能得到Teacher的信息，并且也不能保存。
* instence.setPackage("com.lym.dao");是指定生成的Dao文件所在的包。生成的文件自动在这个包下。
* instence.setPrimaryKeyType("int");是指定类的主键的类型，默认为int型。
* 该工具生成的数据库操作类，是不能自动生成主键的，所以在插入、更新时需要指定主键值。
* 该工具会更具类的关联关系自动生成Dao类和DaoHelper类，Dao类插入、更新、查找都不是级联的，获取得到的关联对象只有关联对象的Id的信息，而DaoHelper类是级联的，可以根据关联关系直接查找到所有的关联对象。
* 对于使用DaoHelper级联更新的操作，只能级联的插入数据库中还没有的数据，不能更新已有的数据。
    例如：使用CourceDaoHelper执行update(cource),Teacher t = cource.getTeacher(),如果t在数据库中不存在，则CourceDaoHelper会将t插入数据库，否则不做任何操作。
<br><br>最重要的一点<br>
##这个工具类生成的是java文件，也就是我们平时手动去写的Dao类，如果对自动生成的类有不完全符合业务逻辑的，可以随意的去更改.可以大大的节省时间成本。
<br><br>附上一个Dao类和DaoHelper类：
*CourceDao.java
```Java
package com.lym.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.lym.bean.Cource;
import com.lym.bean.Teacher;
public class CourceDao extends BaseDao<Cource> {
	public CourceDao(Context context) {
		super(context);
	}


	@Override
	public Cource get(int id2) {
		db = dbHelper.getReadableDatabase();
		Cource cource = null;
		Cursor cursor = db.query(tableName, null, "id=?", new String[] { String.valueOf(id2) }, null, null, null);
		if (cursor.moveToNext()) {
		cource = new Cource();
			int id = cursor.getInt(cursor.getColumnIndex("id"));
			cource.setId(id);
			String name = cursor.getString(cursor.getColumnIndex("name"));
			cource.setName(name);
			Teacher teacher = new Teacher();
			teacher.setId(cursor.getInt(cursor.getColumnIndex("Teacher_id")));
			cource.setTeacher(teacher);
		}
		cursor.close();
		db.close();
		return cource;
	}


	@Override
	public List<Cource> getAll() {
		db = dbHelper.getReadableDatabase();
		List<Cource> cources = new ArrayList<Cource>();
		Cursor cursor = db.query(tableName, null, null,null, null, null, null);
		while (cursor.moveToNext()) {
			Cource cource = new Cource();
			int id = cursor.getInt(cursor.getColumnIndex("id"));
			cource.setId(id);
			String name = cursor.getString(cursor.getColumnIndex("name"));
			cource.setName(name);
			Teacher teacher = new Teacher();
			teacher.setId(cursor.getInt(cursor.getColumnIndex("Teacher_id")));
			cource.setTeacher(teacher);
			cources.add(cource);
		}
		return cources.isEmpty()?null:cources;
	}


	@Override
	public long insert(Cource t) {
		db = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("id",t.getId());
		values.put("name",t.getName());
		Teacher teacher = t.getTeacher();
		if(teacher!=null){
			values.put("Teacher_id",teacher.getId());
		}
		long id = db.insert(tableName, null, values);
		db.close();
		return id;
	}


	@Override
	public int update(int id, Map<String, Object> fields) {
		db = dbHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		Iterator<Entry<String, Object>> iterator = fields.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> next = iterator.next();
			Object value = next.getValue();
			if(value instanceof Teacher){
				values.put("Teacher_id", ((Teacher) value).getId());
			}else {
				values.put(next.getKey(), String.valueOf(value));
			}
		}
		int n = db.update( tableName, values, "id=?", new String[] { String.valueOf(id) });
		db.close();
		return n;
	}
}
```
CourceDaoHelper.java

```Java
package com.lym.dao;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import android.content.Context;


import com.lym.bean.Cource;
import com.lym.bean.Teacher;
import com.lym.dao.TeacherDao;
public class CourceDaoHelper {
	//声明变量
	CourceDao courceDao;
	TeacherDao teacherDao;
	public CourceDaoHelper(Context context) {
		courceDao = new CourceDao(context);
		teacherDao = new TeacherDao(context);
	}


	public Cource get(int id) {
		Cource cource = courceDao.get(id);
		if(cource == null){
			return null;
		}
		Teacher teacher =  cource.getTeacher();
		if (teacher != null){
			teacher = teacherDao.get(teacher.getId());
			cource.setTeacher(teacher);
		}
		return  cource;
	}


	public List<Cource> getAll() {
		List<Cource> all = courceDao.getAll();
		if(all == null){
			return  null;
		}
		for (Cource cource : all) {
			Teacher teacher =  cource.getTeacher();
			if (teacher != null){
				teacher = teacherDao.get(teacher.getId());
				cource.setTeacher(teacher);
		}
		}
		return all;
	}


	public long insert(Cource cource) {
		long insert = courceDao.insert(cource);
		Teacher teacher = cource.getTeacher();
		if(teacher != null){
			//查看数据库中是否已经存在
			Teacher teacher1 = teacherDao.get(teacher.getId());
			if(teacher1 == null){
				//数据库中不存在，插入数据库
				teacherDao.insert(teacher);
			}
		}
		return insert;
	}


	public int update(int id, Map<String, Object> fields) {
		int update = courceDao.update(id, fields);
		Iterator<Entry<String, Object>> iterator = fields.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> next = iterator.next();
			Object value = next.getValue();
			if(value instanceof Teacher){
				Teacher teacher = teacherDao.get(((Teacher) value).getId());
				if(teacher == null){
					//数据库中不存在，插入数据库
					teacherDao.insert((Teacher)value);
				}
			}
		}
		return update;
	}
}


```

