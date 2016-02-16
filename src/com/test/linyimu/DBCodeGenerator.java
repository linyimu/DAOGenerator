package com.test.linyimu;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.lym.dao.bean.ColumnModel;
import com.lym.dao.bean.Relation;
import com.lym.dao.bean.TableModel;
import com.test.Department;
import com.test.Employees;
import com.test.Job;

/**
 * Android数据库代码生成器
 * 
 * @author xuyao
 * 
 */
public class DBCodeGenerator {
	StringBuilder builder = null;
	List<Class> clazzs = null;
	Map<Integer, List<Relation>> relationTypes = null;
	private static DBCodeGenerator instence;

	public DBCodeGenerator() {
		clazzs = new ArrayList<Class>();
		relationTypes = new HashMap<Integer, List<Relation>>();
	}

	public static void main(String[] args) {
		instence = getInstence();
		instence.addClass(Employees.class);
		instence.addClass(Department.class);
		instence.addClass(Job.class);
		instence.setPackage("com.lym.test");
		instence.execute();
	}

	public static DBCodeGenerator getInstence() {
		if (instence == null) {
			instence = new DBCodeGenerator();
		}
		return instence;
	}

	public DBCodeGenerator addClass(Class clazz) {
		clazzs.add(clazz);
		return instence;
	}

	public DBCodeGenerator addClass(Class... clss) {
		for (Class cls : clss) {
			clazzs.add(cls);
		}
		return instence;
	}

	private String packageName;

	public DBCodeGenerator setPackage(String packageName) {
		this.packageName = packageName;
		return instence;
	}

	public void execute() {
		int size = clazzs.size();
		// 先判断两个类的关系
		for (int i = 0; i < size; i++) {
			Class cls1 = clazzs.get(i);
			List<Relation> list = null;
			for (int j = 0; j < size; j++) {
				if (i == j)
					continue;
				Class cls2 = clazzs.get(j);
				int relationType = getRelation(cls1, cls2);
				Relation relation = new Relation();
				relation.setType(relationType);
				relation.setPrimaryClass(cls1);
				relation.setForeignClass(cls2);
				list = relationTypes.get(cls1.hashCode());
				if (list == null) {
					list = new ArrayList<Relation>();
					relationTypes.put(cls1.hashCode(), list);
				}
				list.add(relation);
			}
		}

		// for (Class clz : clazzs) {
		// String sql = getTableSql(clz);
		// }
		//
		//
		//
		// Set<Entry<Integer, String>> entrySet = relateSqls.entrySet();
		// Iterator<Entry<Integer, String>> iterator = entrySet.iterator();
		// while (iterator.hasNext()) {
		// Entry<Integer, String> next = iterator.next();
		// System.out.println(next.getValue());
		// }

		generatorDBHelper(clazzs, false);
		genaratorIDao();
		genaratorBaseDao();
		for (Class cls : clazzs) {
			genaratorClassDao(cls);
		}

	}

	/**
	 * 打印两个类的关系
	 * 
	 * @param relation
	 */
	private void printRelation(int relation) {
		if (relation == Relation.ONE_TO_ONE) {
			System.out.println("1:1");
		} else if (relation == Relation.ONE_TO_MORE) {
			System.out.println("1：n");
		} else if (relation == Relation.MORE_TO_ONE) {
			System.out.println("n：1");
		} else if (relation == Relation.MORE_TO_MORE) {
			System.out.println("m：n");
		} else if (relation == Relation.RELATION_NONE) {
			System.out.println("no ralation");
		} else {
			System.out.println("no ralation");
		}
	}

	/**
	 * 获取表的模型
	 * 
	 * @param clazz
	 * @return
	 */
	public TableModel getTableModel(Class clazz) {
		TableModel tm = new TableModel();
		tm.setTableClass(clazz);
		tm.setTableName(clazz.getSimpleName());

		Field[] fields = clazz.getDeclaredFields();
		List<ColumnModel> cms = new ArrayList<ColumnModel>();
		for (Field field : fields) {
			// 不是静态的
			if (!Modifier.isStatic(field.getModifiers())) {
				ColumnModel columnModel = getColumnModel(clazz, field);
				if (columnModel != null) {
					cms.add(columnModel);
				}
			}
		}
		tm.setColumModels(cms);
		return tm;
	}

	/**
	 * 获取列的模型
	 * 
	 * @param clazz
	 * @param field
	 * @return
	 */
	public ColumnModel getColumnModel(Class clazz, Field field) {
		ColumnModel columnModel = null;
		Class<?> fieldType = field.getType();
		String fieldName = fieldType.getName();
		String fieldTypeName = getFieldType(fieldName);
		if (fieldTypeName != null) {
			columnModel = new ColumnModel();
			columnModel.setColumnName(field.getName());
			columnModel.setColumnType(fieldTypeName);
			columnModel.setClassType(fieldType);
		}
		return columnModel;
	}

	/**
	 * 获取List泛型的类型
	 * 
	 * @param field
	 * @return
	 */
	private Class getListTType(Field field) {
		// 获取集合泛型
		Type tType = field.getGenericType();
		if (tType instanceof ParameterizedType) {
			Type[] actualTypeArguments = ((ParameterizedType) tType)
					.getActualTypeArguments();
			tType = actualTypeArguments[0];
		}
		// 集合泛型类型
		Class tClass = (Class) tType;
		return tClass;
	}

	/**
	 * 获取创建表的Sql语句
	 * 
	 * @param clazz
	 * @return
	 */
	public String getTableSql(Class clazz) {
		StringBuilder sqlBuilder = new StringBuilder();
		TableModel tableModel = getTableModel(clazz);
		sqlBuilder.append("create table if not exists  "
				+ tableModel.getTableName() + "(");
		List<ColumnModel> columModels = tableModel.getColumModels();

		for (ColumnModel model : columModels) {
			if (model.getColumnName() != null) {
				sqlBuilder.append(model.getColumnName());
				sqlBuilder.append(" ");
				sqlBuilder.append(model.getColumnType());
				sqlBuilder.append(",");
			}
		}

		// 添加关联关系
		List<Relation> list = relationTypes.get(clazz.hashCode());
		if (list != null) {
			for (Relation rela : list) {
				// 有关联关系
				int relation = rela.getType();
				if (relation == -1) {

				} else if (relation == Relation.ONE_TO_ONE) {
					sqlBuilder.append(rela.getForeignClass().getSimpleName());
					sqlBuilder.append("_id");
					sqlBuilder.append(" integer");
					sqlBuilder.append(",");
				} else if (relation == Relation.ONE_TO_MORE) {
					// 不处理
				} else if (relation == Relation.MORE_TO_ONE) {
					sqlBuilder.append(rela.getForeignClass().getSimpleName());
					sqlBuilder.append("_id");
					sqlBuilder.append(" integer");
					sqlBuilder.append(",");
				} else if (relation == Relation.MORE_TO_MORE) {
					// 增加链接表
					// 增加中间表
					String relateSql = "create table if not exists  "
							+ clazz.getSimpleName() + "_"
							+ rela.getForeignClass().getSimpleName() + "("
							+ clazz.getSimpleName() + "_id integer,"
							+ rela.getForeignClass().getSimpleName()
							+ "_id integer" + ")";

					if (!relateSqls.containsKey(clazz.hashCode()
							+ rela.getForeignClass().hashCode())) {
						relateSqls.put(clazz.hashCode()
								+ rela.getForeignClass().hashCode(), relateSql);
					}
				}
			}
		}
		sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);
		sqlBuilder.append(")");
		System.out.println(sqlBuilder.toString());
		return sqlBuilder.toString();
	}

	/**
	 * 生成DBHelper文件
	 * 
	 * @param clss
	 * @param isAccessParentField
	 */
	private void generatorDBHelper(List<Class> clss, boolean isAccessParentField) {
		builder = new StringBuilder();
		if (packageName != null) {
			append("package " + packageName + ";", 0, true);
			appendLine(1);
		}

		append("import android.content.Context;", 0, true);
		append("import android.database.DatabaseErrorHandler;", 0, true);
		append("import android.database.sqlite.SQLiteDatabase;", 0, true);
		append("import android.database.sqlite.SQLiteDatabase.CursorFactory;",
				0, true);
		append("import android.database.sqlite.SQLiteOpenHelper;", 0, true);
		appendLine(2);

		append("public class DBHelper extends SQLiteOpenHelper {", 0, true);

		append("public DBHelper(Context context, String name, CursorFactory factory,int version, DatabaseErrorHandler errorHandler) {",
				1, true);
		append("super(context, name, factory, version, errorHandler);", 2, true);
		append("// TODO Auto-generated constructor stub", 2, true);
		append("}", 1, true);
		appendLine(2);

		append("public DBHelper(Context context, String name, CursorFactory factory,int version) {",
				1, true);
		append("super(context, name, factory, version);", 2, true);
		append("// TODO Auto-generated constructor stub", 2, true);
		append("}", 1, true);
		appendLine(2);

		append("public DBHelper(Context context, String name, CursorFactory factory) {",
				1, true);
		append("this(context, name, factory, 1);", 2, true);
		append("// TODO Auto-generated constructor stub", 2, true);
		append("}", 1, true);
		appendLine(2);

		append("public DBHelper(Context context, String dbName) {", 1, true);
		append("this(context, dbName, null);", 2, true);
		append("// TODO Auto-generated constructor stub", 2, true);
		append("}", 1, true);
		appendLine(2);

		append("@Override", 1, true);
		append("public void onCreate(SQLiteDatabase db) {", 1, true);
		append("// TODO Auto-generated method stub", 2, true);

		// 创建表
		for (Class cls : clss) {
			append("//创建" + cls.getSimpleName() + "表", 2, true);
			String sql = cls.getSimpleName() + "Sql";
			append("String " + sql + " = \"", 2, false);
			append(getCreateTableStr(cls, isAccessParentField), 0, false);
			append("\";", 0, true);
			// 执行SQL语句
			append("db.execSQL(" + sql + ");", 2, true);
			appendLine(2);

		}
		// 创建关联的表
		// 执行SQL语句
		Set<Entry<Integer, String>> entrySet = relateSqls.entrySet();
		Iterator<Entry<Integer, String>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {

			Entry<Integer, String> next = iterator.next();
			String value = next.getValue();
			int index = value.indexOf("_");
			String tableName2 = value.substring(index + 1, value.indexOf("("));
			String tableName1 = value.substring(0, index).replace(
					"create table if not exists  ", "");
			append("//创建" + tableName1 + "表和" + tableName2 + "表之间的连接表"
					+ tableName1 + "_" + tableName2, 2, true);
			append("db.execSQL(\"" + next.getValue() + "\");", 2, true);
		}

		append("}", 1, true);
		appendLine(2);
		append("@Override", 1, true);
		append("public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {",
				1, true);
		append("// TODO Auto-generated method stub", 2, true);
		append("}", 1, true);
		append("}", 0, true);

		System.out.println(builder.toString());
		toFile(builder.toString(), "DBHelper.java");

	}

	/**
	 * 生成DAO接口
	 */
	public void genaratorIDao() {
		builder = new StringBuilder();
		if (packageName != null) {
			append("package " + packageName + ";", 0, true);
			appendLine(1);
		}

		append("import java.util.List;", 0, true);
		append("import java.util.Map;", 0, true);
		appendLine(2);

		append("public interface IDao<T> {", 0, true);
		appendLine(1);
		append("public T get(int id);", 1, true);
		appendLine(1);
		append("public List<T> getAll();", 1, true);
		appendLine(1);
		append("public long insert(T t);", 1, true);
		appendLine(1);
		append("public int deleteAll();", 1, true);
		appendLine(1);
		append("public int delete(int  id);", 1, true);
		appendLine(1);
		append("public int update(int id, Map<String, Object> fields);", 1,
				true);
		appendLine(1);
		append("}", 0, true);

		System.out.println(builder.toString());

		toFile(builder.toString(), "IDao.java");
	}

	/**
	 * 生成BaseDao
	 */
	public void genaratorBaseDao() {
		builder = new StringBuilder();

		if (packageName != null) {
			append("package " + packageName + ";", 0, true);
			appendLine(1);
		}

		append("import java.lang.reflect.ParameterizedType;", 0, true);
		append("import java.lang.reflect.Type;", 0, true);
		append("import android.content.Context;", 0, true);
		append("import android.database.sqlite.SQLiteDatabase;", 0, true);

		append("public abstract class BaseDao<T> implements IDao<T> {", 0, true);
		appendLine(1);
		append("public DBHelper dbHelper;", 1, true);
		append("public SQLiteDatabase db;", 1, true);
		append("private Class clazz;", 1, true);
		append("public String tableName;", 1, true);
		appendLine(1);
		append("public BaseDao(Context context) {", 1, true);
		append("this(context, null);", 2, true);
		append("}", 1, true);

		append("public BaseDao(Context context,String tablaName) {", 1, true);
		append("// 获取泛型T的类型", 2, true);
		append("Type genType = this.getClass().getGenericSuperclass();", 2,
				true);
		append("Type[] params = ((ParameterizedType) genType).getActualTypeArguments();",
				2, true);
		append("clazz = (Class<T>) params[0];", 2, true);
		append("if (tableName == null) {", 2, true);
		append("this.tableName = clazz.getSimpleName();", 3, true);
		append("}", 2, true);
		append("this.tableName = tableName;", 2, true);

		append("dbHelper = new DBHelper(context, tableName);", 2, true);

		append("}", 1, true);
		appendLine(1);
		append("@Override", 1, true);
		append("public int deleteAll() {", 1, true);
		append("db = dbHelper.getWritableDatabase();", 2, true);
		append("int n = db.delete(clazz.getSimpleName(), null, null);", 2, true);
		append("db.close();", 2, true);
		append("return n;", 2, true);
		append("}", 1, true);

		appendLine(1);

		append("@Override", 1, true);
		append("public int delete(int id) {", 1, true);
		append("db = dbHelper.getWritableDatabase();", 2, true);
		append("int n = db.delete(clazz.getSimpleName(),\"id=?\",new String[] { String.valueOf(id) });",
				2, true);
		append("db.close();", 2, true);
		append("return n;", 2, true);
		append("}", 1, true);

		append("}", 0, true);
		appendLine(1);

		System.out.println(builder.toString());
		toFile(builder.toString(), "BaseDao.java");
	}

	public String genaratorClassDao(Class clazz) {
		TableModel tableModel = getTableModel(clazz);
		builder = new StringBuilder();

		String tableName = tableModel.getTableName();

		if (packageName != null) {
			append("package " + packageName + ";", 0, true);
			appendLine(1);
		}
		append("import java.util.ArrayList;", 0, true);
		append("import java.util.Iterator;", 0, true);
		append("import java.util.List;", 0, true);
		append("import java.util.Map;", 0, true);
		append("import java.util.Map.Entry;", 0, true);
		append("import android.content.ContentValues;", 0, true);
		append("import android.content.Context;", 0, true);
		append("import android.database.Cursor;", 0, true);
		append("import " + clazz.getName() + ";", 0, true);

		append("public class " + clazz.getSimpleName() + "Dao extends BaseDao<"
				+ tableName + "> {", 0, true);
		append("public " + tableName + "Dao(Context context) {", 1, true);
		append("super(context);", 2, true);
		append("}", 1, true);
		appendLine(2);

		// get(id)方法
		append("@Override", 1, true);
		append("public " + tableName + " get(int id) {", 1, true);
		append("db = dbHelper.getReadableDatabase();", 2, true);
		append(tableName + " " + tableName.toLowerCase() + " = new "
				+ tableName + "();", 2, true);
		append("Cursor cursor = db.query(tableName, null, \"id=?\", new String[] { String.valueOf(id) }, null, null, null);",
				2, true);
		append("if (cursor.moveToNext()) {", 2, true);
		CursorToEntry(tableModel, tableName);
		append("}", 2, true);
		append("cursor.close();", 2, true);
		append("db.close();", 2, true);
		append("return " + tableName.toLowerCase() + ";", 2, true);
		append("}", 1, true);
		appendLine(2);

		// getAll方法
		append("@Override", 1, true);
		append("public List<" + tableName + "> getAll() {", 1, true);
		append("db = dbHelper.getReadableDatabase();", 2, true);
		append("List<" + tableName + "> " + tableName.toLowerCase()
				+ "s = new ArrayList<" + tableName + ">();", 2, true);
		append("Cursor cursor = db.query(tableName, null, null,null, null, null, null);",
				2, true);
		append("while (cursor.moveToNext()) {", 2, true);
		append("" + tableName + " " + tableName.toLowerCase() + " = new "
				+ tableName + "();", 3, true);
		CursorToEntry(tableModel, tableName);
		append(tableName.toLowerCase() + "s.add(" + tableName.toLowerCase()
				+ ");", 3, true);
		append("}", 2, true);
		append("return " + tableName.toLowerCase() + "s;", 2, true);
		append("}", 1, true);
		appendLine(2);

		// insert方法
		append("@Override", 1, true);
		append("public long insert(" + tableName + " t) {", 1, true);
		append("db = dbHelper.getWritableDatabase();", 2, true);
		append("ContentValues values = new ContentValues();", 2, true);
		List<ColumnModel> columModels = tableModel.getColumModels();
		for (ColumnModel cm : columModels) {
			String columnName = cm.getColumnName();
			String fieldType = cm.getClassType().getName();
			String classType = getClassType(fieldType);
			if (classType.equals("boolean")) {
				append("values.put(\"" + columnName + "\",t.is"
						+ columnName.substring(0, 1).toUpperCase()
						+ columnName.substring(1) + "());", 2, true);
			} else {
				append("values.put(\"" + columnName + "\",t.get"
						+ columnName.substring(0, 1).toUpperCase()
						+ columnName.substring(1) + "());", 2, true);
			}
		}

		// 关联关系
		List<Relation> list = relationTypes.get(clazz.hashCode());
		for (Relation re : list) {
			if (re.getType() == Relation.MORE_TO_ONE
					|| re.getType() == Relation.ONE_TO_ONE) {
				Class foreignClass = re.getForeignClass();
				Field[] declaredFields = clazz.getDeclaredFields();
				for (Field field : declaredFields) {
					String fieldTypeName = field.getType().getSimpleName();
					if (field.getType() == foreignClass) {
						String fieldName = field.getName();
						append(fieldTypeName + " "
								+ fieldTypeName.toLowerCase() + " = t.get"
								+ fieldName.substring(0, 1).toUpperCase()
								+ fieldName.substring(1) + "();", 2, true);
						append("if(" + fieldTypeName.toLowerCase() + "!=null){",
								2, true);
						append("values.put(\"" + fieldTypeName + "_id\","
								+ fieldTypeName.toLowerCase() + ".getId());",
								3, true);
						append("}", 2, true);
					}
				}
			}
		}

		append("long id = db.insert(tableName, null, values);", 2, true);
		append("db.close();", 2, true);
		append("return id;", 2, true);
		append("}", 1, true);

		appendLine(2);
		// update方法
		append("@Override", 1, true);
		append("public int update(int id, Map<String, Object> fields) {", 1,
				true);
		append("db = dbHelper.getWritableDatabase();", 2, true);
		append("ContentValues values = new ContentValues();", 2, true);
		append("Iterator<Entry<String, Object>> iterator = fields.entrySet().iterator();",
				2, true);
		append("while (iterator.hasNext()) {", 2, true);
		append("Entry<String, Object> next = iterator.next();", 3, true);
		append("Object value = next.getValue();", 3, true);

		// list = relationTypes.get(clazz.hashCode());

		boolean first = true;
		for (Relation re : list) {
			if (re.getType() == Relation.MORE_TO_ONE
					|| re.getType() == Relation.ONE_TO_ONE) {
				append("if(value instanceof "
						+ re.getForeignClass().getSimpleName() + "){",
						first ? 3 : 0, true);
				append("values.put(\"" + re.getForeignClass().getSimpleName()
						+ "_id\", ((" + re.getForeignClass().getSimpleName()
						+ ") value).getId());", 4, true);
				append("}else ", 3, false);
				first = false;
			}

		}
		if (!first) {
			append("{", 0, true);
		}
		append("values.put(next.getKey(), String.valueOf(value));", first ? 3
				: 4, true);
		if (!first) {
			append("}", 3, true);
		}

		append("}", 2, true);

		append("int n = db.update( tableName, values, \"id=?\", new String[] { String.valueOf(id) });",
				2, true);
		append("db.close();", 2, true);
		append("return n;", 2, true);
		append("}", 1, true);

		append("}", 0, true);
		System.out.println(builder.toString());

		toFile(builder.toString(), clazz.getSimpleName() + "Dao.java");
		return builder.toString();
	}

	private void CursorToEntry(TableModel tableModel, String tableName) {
		List<ColumnModel> columModels = tableModel.getColumModels();
		for (ColumnModel cm : columModels) {
			String columnName = cm.getColumnName();
			String fieldType = cm.getClassType().getName();
			String type = getClassType(fieldType);

			append(type + " " + columnName + " = cursor.get"
					+ type.substring(0, 1).toUpperCase() + type.substring(1)
					+ "(cursor.getColumnIndex(\"" + columnName + "\"));", 3,
					true);
			append(tableName.toLowerCase() + ".set"
					+ columnName.substring(0, 1).toUpperCase()
					+ columnName.substring(1) + "(" + columnName + ");", 3,
					true);
		}

		Class tableClass = tableModel.getTableClass();

		// 获取关联关系
		List<Relation> list = relationTypes.get(tableClass.hashCode());
		for (Relation re : list) {
			if (re.getType() == Relation.MORE_TO_ONE
					|| re.getType() == Relation.ONE_TO_ONE) {
				Class foreignClass = re.getForeignClass();
				Field[] declaredFields = tableClass.getDeclaredFields();
				for (Field field : declaredFields) {
					String fieldTypeName = field.getType().getSimpleName();
					if (field.getType() == foreignClass) {
						String importName = foreignClass.getName();
						if (builder.indexOf("import " + importName) == -1) {
							// 插入引用的包
							insertImportPackge(importName);
						}
						String fieldName = field.getName();
						append(fieldTypeName + " "
								+ fieldTypeName.toLowerCase() + " = new "
								+ fieldTypeName + "();", 3, true);
						append(fieldTypeName.toLowerCase()
								+ ".setId(cursor.getInt(cursor.getColumnIndex(\""
								+ fieldTypeName + "_id\")" + "));", 3, true);

						append(tableName.toLowerCase() + ".set"
								+ fieldName.substring(0, 1).toUpperCase()
								+ fieldName.substring(1) + "("
								+ fieldTypeName.toLowerCase() + ");", 3, true);
					}
				}
			}
		}
	}

	private String getClassType(String fieldType) {
		String type = "int";
		if (fieldType.equals("int") || fieldType.equals("java.lang.Integer")) {
			type = "int";
		}
		if (fieldType.equals("long") || fieldType.equals("java.lang.Long")) {
			type = "long";
		}
		if (fieldType.equals("short") || fieldType.equals("java.lang.Short")) {
			type = "short";
		}
		if (fieldType.equals("boolean")
				|| fieldType.equals("java.lang.Boolean")) {
			type = "boolean";
		}

		if (fieldType.equals("float") || fieldType.equals("java.lang.Float")) {
			type = "float";
		}
		if (fieldType.equals("double") || fieldType.equals("java.lang.Double")) {
			type = "double";
		}

		if (fieldType.equals("java.lang.String")) {
			type = "String";
		}
		return type;
	}

	/**
	 * 写入文件
	 * 
	 * @param text
	 * @param fileName
	 */
	private void toFile(String text, String fileName) {
		File file = new File("src/" + fileName);
		System.out.println(file.getAbsolutePath());
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(text.getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 换行
	 * 
	 * @param n
	 */
	public void appendLine(int n) {
		for (int i = 0; i < n; i++)
			builder.append("\n");
	}

	/**
	 * 添加字符
	 * 
	 * @param text
	 *            要添加的字符
	 * @param tabSize
	 *            几个tab空格
	 * @param isLineOther
	 *            是否在结束后换行
	 */
	public void append(String text, int tabSize, boolean isLineOther) {
		for (int i = 0; i < tabSize; i++) {
			builder.append("\t");
		}

		builder.append(text);
		if (isLineOther) {
			builder.append("\n");
		}
	}

	/**
	 * 插入引用包
	 * 
	 * @param text
	 */
	public void insertImportPackge(String packge) {
		int indexOf = builder.indexOf("public class");
		if (indexOf != -1) {
			builder.insert(indexOf, "import " + packge + ";\n");
		} else {
			indexOf = builder.indexOf("\n");
			if (indexOf != -1) {
				builder.insert(indexOf, "\nimport " + packge + ";\n");
			} else {
				builder.insert(0, "\nimport " + packge + ";\n");
			}
		}
	}

	/**
	 * 多对多关系间的链接表
	 */
	Map<Integer, String> relateSqls = new HashMap<Integer, String>();

	/**
	 * 获取创建表的Sql语句
	 * 
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unused")
	public String getCreateTableStr(Class clazz, boolean isAccessParentField) {
		// clazz包含的外键集合
		List<Class> forenkeyClass = new ArrayList<Class>();
		StringBuilder sql = new StringBuilder();

		String tableName = clazz.getSimpleName();
		sql.append("create table if not exists  " + tableName + "(");
		// 遍历字段
		Field[] fields = clazz.getDeclaredFields();
		if (isAccessParentField) {
			Field[] parentField = clazz.getSuperclass().getDeclaredFields();
			Field[] allFields = new Field[fields.length + parentField.length];
			for (int i = 0; i < allFields.length; i++) {
				if (i < fields.length) {
					allFields[i] = fields[i];
				} else {
					allFields[i] = allFields[i - fields.length];
				}
			}

			fields = allFields;
		}
		for (Field f : fields) {
			// 静态字段直接跳过
			if (Modifier.isFinal(f.getModifiers())
					|| Modifier.isStatic(f.getModifiers())) {
				continue;
			}

			String fieldName = f.getName();
			Class fieldType = f.getType();

			String typeName = fieldType.getName();

			String fieldTypeName = getFieldType(typeName);

			if (fieldTypeName == null) {
				// 说明是引用类型
				// 如果是集合(1对多 或 多对多 的关系)
				if (Collection.class.isAssignableFrom(fieldType)) {
					// 得到泛型的类型
					// 判断关联类型
					try {
						Class tClass = getListTType(f);

						boolean isForenkey1 = isForenkey(clazz, tClass);
						boolean isForenkey2 = isForenkey(tClass, clazz);

						// // 如果是1:n的关系
						if (isForenkey1) {
							// 添加外键
							sql.append(tClass.getSimpleName() + "_id ");
							sql.append("integer,");
							forenkeyClass.add(tClass);
						}

						// 如果是多对多的关系
						if (isForenkey1 && isForenkey2) {
							// 增加中间表
							String relateSql = "create table if not exists  "
									+ clazz.getSimpleName() + "_"
									+ tClass.getSimpleName() + "("
									+ clazz.getSimpleName() + "_id integer,"
									+ tClass.getSimpleName() + "_id integer"
									+ ")";
							if (!relateSqls.containsKey(clazz.hashCode()
									+ tClass.hashCode())) {
								relateSqls.put(
										clazz.hashCode() + tClass.hashCode(),
										relateSql);
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}

				} else {
					// 不是集合，直接添加外键
					sql.append(fieldType.getSimpleName() + "_id ");
					sql.append("integer,");
					forenkeyClass.add(fieldType);
				}
			} else {
				sql.append(fieldName);
				sql.append(" ");
				sql.append(fieldTypeName);
				sql.append(",");

			}

		}
		// 检查是否还有外键
		for (Class cls : clazzs) {
			if (isForenkey(clazz, cls) && !forenkeyClass.contains(cls)) {
				sql.append(cls.getSimpleName() + "_id integer,");
			}
		}
		sql.deleteCharAt(sql.length() - 1);
		sql.append(")");
		return sql.toString();
	}

	/**
	 * c1是否是c2的外键(说明在c2中有c1的引用或者是c1中有c2的集合）
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	private boolean isForenkey(Class c1, Class c2) {
		// c1中有c2的引用
		Field[] fields2 = c1.getDeclaredFields();
		for (Field f : fields2) {
			if (!Modifier.isStatic(f.getModifiers())) {
				if (c2 == f.getType()) {
					return true;
				}
			}
		}
		// c2中有c1的集合
		Field[] fields1 = c2.getDeclaredFields();

		for (Field f : fields1) {
			if (!Modifier.isStatic(f.getModifiers())) {
				if (Collection.class.isAssignableFrom(f.getType())) {
					Class tClass = getListTType(f);
					if (tClass == c1) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 获取字段的类型
	 * 
	 * @param fieldType
	 * @return
	 */
	public String getFieldType(String fieldType) {
		if (fieldType != null) {
			if (fieldType.equals("int")
					|| fieldType.equals("java.lang.Integer")) {
				return "integer";
			}
			if (fieldType.equals("long") || fieldType.equals("java.lang.Long")) {
				return "integer";
			}
			if (fieldType.equals("short")
					|| fieldType.equals("java.lang.Short")) {
				return "integer";
			}
			if (fieldType.equals("boolean")
					|| fieldType.equals("java.lang.Boolean")) {
				return "integer";
			}

			if (fieldType.equals("java.util.Date")) {
				return "integer";
			}

			if (fieldType.equals("float")
					|| fieldType.equals("java.lang.Float")) {
				return "real";
			}
			if (fieldType.equals("double")
					|| fieldType.equals("java.lang.Double")) {
				return "real";
			}

			if (fieldType.equals("char")
					|| fieldType.equals("java.lang.Character")) {
				return "text";
			}
			if (fieldType.equals("java.lang.String")) {
				return "text";
			}
		}
		return null;
	}

	/**
	 * 获取两个类的关联关系
	 * 
	 * @param cls1
	 * @param cls2
	 */
	public int getRelation(Class cls1, Class cls2) {
		// 1:1
		boolean cls1HasCls2 = false;
		boolean cls2HasCls1 = false;
		// 是否包含集合
		boolean cls1HasCls2s = false;
		boolean cls2HasCls1s = false;
		Field[] fields1 = cls1.getDeclaredFields();
		for (Field field : fields1) {
			Class<?> type = field.getType();
			if (type == cls2) {
				// 说明cls1中有cls2的引用
				cls1HasCls2 = true;
			}
		}

		Field[] fields2 = cls2.getDeclaredFields();
		for (Field field : fields2) {
			Class<?> type = field.getType();
			if (type == cls1) {
				// 说明cls2中有cls1的引用
				cls2HasCls1 = true;
			}
		}

		if (cls1HasCls2 && cls2HasCls1) {
			// 1:1的关系
			return Relation.ONE_TO_ONE;
		}
		if (cls1HasCls2) {
			// n:1的关系
			return Relation.MORE_TO_ONE;
		}

		if (cls2HasCls1) {
			// 1:n的关系
			return Relation.ONE_TO_MORE;
		}

		for (Field field : fields1) {
			Class<?> fieldType = field.getType();
			if (List.class.isAssignableFrom(fieldType)) {// 如果是集合
				Class tClass = getListTType(field);
				if (tClass == cls2) {
					cls1HasCls2s = true;
				}
			}
		}

		for (Field field : fields2) {
			Class<?> fieldType = field.getType();
			if (List.class.isAssignableFrom(fieldType)) {// 如果是集合
				Class tClass = getListTType(field);
				if (tClass == cls1) {
					cls2HasCls1s = true;
				}
			}
		}

		if (cls1HasCls2s && cls2HasCls1s) {
			// n:m
			return Relation.MORE_TO_MORE;
		}
		if (cls1HasCls2s) {
			// 1:n
			return Relation.ONE_TO_MORE;
		}
		if (cls2HasCls1s) {
			// n:1
			return Relation.MORE_TO_ONE;
		}

		return Relation.RELATION_NONE;
	}
}
