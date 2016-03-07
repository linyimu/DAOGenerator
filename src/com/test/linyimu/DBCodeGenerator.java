package com.test.linyimu;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.lym.annotation.Column;
import com.lym.dao.bean.ColumnModel;
import com.lym.dao.bean.Relation;
import com.lym.dao.bean.TableModel;
import com.sun.xml.internal.bind.v2.model.core.Ref;

/**
 * Android数据库代码生成器
 * 
 * @author xuyao
 * 
 */
public class DBCodeGenerator {
	StringBuilder builder = null;
	List<Class> clazzs = null;
	Map<Integer, List<Relation>> relations = null;
	static Map<Integer, TableModel> tableModels = null;
	private static DBCodeGenerator instence;

	public DBCodeGenerator() {
		clazzs = new ArrayList<Class>();
		relations = new HashMap<Integer, List<Relation>>();
		tableModels = new HashMap<Integer, TableModel>();
	}

	public static DBCodeGenerator getInstence() {
		if (instence == null) {
			instence = new DBCodeGenerator();
			tableModels = new HashMap<Integer, TableModel>();
		}
		return instence;
	}

	public DBCodeGenerator addClass(Class clazz) {
		clazzs.add(clazz);
		return instence;
		// TableModel tableModel = new TableModel();
		// tableModels.put(clazz.hashCode(), tableModel);
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

	/**
	 * 主键的类型（如int、String等）
	 */
	private String primaryKeyType;

	public DBCodeGenerator setPrimaryKeyType(String primaryKeyType) {
		this.primaryKeyType = primaryKeyType;
		return instence;
	}

	public void execute2() {

		// 默认的主键类型
		if (primaryKeyType == null) {
			primaryKeyType = "Integer";
		}
		for (Class cls : clazzs) {
			TableModel tableModel = getTableModel(cls);
			tableModels.put(cls.hashCode(), tableModel);
		}

		int size = clazzs.size();

		ArrayList<Relation> res = new ArrayList<Relation>();
		// 判断两个类的关系
		for (int i = 0; i < size - 1; i++) {
			Class cls1 = clazzs.get(i);
			List<Relation> list = null;
			for (int j = i + 1; j < size; j++) {
				Class cls2 = clazzs.get(j);
				Relation relation = getRelation(cls1, cls2);
				res.add(relation);

				System.out.println(relation.getPrimaryClass().getSimpleName()
						+ ":" + relation.getForeignClass().getSimpleName()
						+ "= " + relation.getType());
			}
		}

		// 遍历关系
		for (Relation re : res) {
			int type = re.getType();
			if (type == Relation.ONE_TO_MORE) {
				addColumnWithTableModel(re.getForeignClass(),
						re.getPrimaryClass(), Relation.MORE_TO_ONE);
			} else {
				addColumnWithTableModel(re.getPrimaryClass(),
						re.getForeignClass(), type);
			}
		}

		generatorDBHelper();
		genaratorIDao();
		genaratorBaseDao();

		Iterator<Entry<Integer, TableModel>> iterator = tableModels.entrySet()
				.iterator();

		while (iterator.hasNext()) {
			Entry<Integer, TableModel> next = iterator.next();
			System.out.println(next.getValue());
			genaratorClassDao(next.getValue());

		}
	}

	public void addColumnWithTableModel(Class primaryClass, Class foreignClass,
			int relationType) {
		if (relationType == Relation.ONE_TO_ONE) {
			TableModel fcModel = tableModels.get(foreignClass.hashCode());
			TableModel pcModel = tableModels.get(primaryClass.hashCode());
			ColumnModel fcPrimaryKey = fcModel.getPrimaryKey();
			ColumnModel pcPrimaryKey = fcModel.getPrimaryKey();

			// 给pcPrimaryKey的TableModel增加一列
			ColumnModel pcColumnModel = new ColumnModel();
			pcColumnModel.setLocal(false);
			pcColumnModel.setColumnName(fcModel.getTableName() + "_"
					+ fcPrimaryKey.getColumnName());

			pcColumnModel.setColumnType(fcPrimaryKey.getColumnType());
			pcColumnModel.setColumnJavaType(fcPrimaryKey.getColumnJavaType());
			pcColumnModel.setRefrenceOther(true);
			pcColumnModel.setRefrenceClass(foreignClass);
			pcColumnModel.setRefrenceType(relationType);
			pcModel.addColumnModel(pcColumnModel);

			// 给fcPrimaryKey的TableModel增加一列
			ColumnModel fcColumnModel = new ColumnModel();
			fcColumnModel.setLocal(false);
			fcColumnModel.setColumnName(pcModel.getTableName() + "_"
					+ pcPrimaryKey.getColumnName());

			fcColumnModel.setColumnType(pcPrimaryKey.getColumnType());
			fcColumnModel.setColumnJavaType(pcPrimaryKey.getColumnJavaType());
			fcColumnModel.setRefrenceOther(true);
			fcColumnModel.setRefrenceClass(primaryClass);
			fcColumnModel.setRefrenceType(relationType);
			fcModel.addColumnModel(pcColumnModel);
		} else if (relationType == Relation.MORE_TO_ONE) {
			// n：1
			TableModel fcModel = tableModels.get(foreignClass.hashCode());
			TableModel pcModel = tableModels.get(primaryClass.hashCode());
			ColumnModel fcPrimaryKey = fcModel.getPrimaryKey();

			System.out.println("fcModel=" + fcModel.getTableName()
					+ ",pcModel=" + pcModel.getTableName());
			;
			// 给pcPrimaryKey的TableModel增加一列
			ColumnModel pcColumnModel = new ColumnModel();

			pcColumnModel.setLocal(false);
			pcColumnModel.setColumnName(fcModel.getTableName() + "_"
					+ fcPrimaryKey.getColumnName());
			pcColumnModel.setColumnType(fcPrimaryKey.getColumnType());
			pcColumnModel.setColumnJavaType(fcPrimaryKey.getColumnJavaType());
			pcColumnModel.setRefrenceOther(true);
			pcColumnModel.setRefrenceClass(foreignClass);
			pcColumnModel.setRefrenceType(relationType);
			pcModel.addColumnModel(pcColumnModel);

		} else if (relationType == Relation.MORE_TO_MORE) {

			TableModel fcModel = tableModels.get(foreignClass.hashCode());
			TableModel pcModel = tableModels.get(primaryClass.hashCode());

			if (tableModels.get(foreignClass.hashCode()
					+ primaryClass.hashCode()) == null) {

				// 再创建一个TableModel,用于连接两个表
				TableModel tm = new TableModel();
				// 为链接表
				tm.setJoinTable(true);

				tm.setTableName(pcModel.getTableName() + "_"
						+ fcModel.getTableName());

				ColumnModel prmaryKey = new ColumnModel();
				prmaryKey.setLocal(true);
				prmaryKey.setColumnName("id");
				prmaryKey.setColumnType("integer");
				prmaryKey.setPrimaryKey(true);

				tm.addColumnModel(prmaryKey);
				tm.setPrimaryKey(prmaryKey);

				ColumnModel pcColumnModel = new ColumnModel();
				pcColumnModel.setLocal(true);
				pcColumnModel.setColumnName(pcModel.getTableName() + "_"
						+ pcModel.getPrimaryKey().getColumnName());
				pcColumnModel.setColumnType(pcModel.getPrimaryKey()
						.getColumnType());
				pcColumnModel.setRefrenceOther(true);
				pcColumnModel.setRefrenceClass(pcModel.getClazz());
				tm.addColumnModel(pcColumnModel);

				ColumnModel fcColumnModel = new ColumnModel();
				fcColumnModel.setLocal(true);
				fcColumnModel.setColumnName(fcModel.getTableName() + "_"
						+ fcModel.getPrimaryKey().getColumnName());
				fcColumnModel.setColumnType(fcModel.getPrimaryKey()
						.getColumnType());

				fcColumnModel.setRefrenceOther(true);
				fcColumnModel.setRefrenceClass(fcModel.getClazz());
				tm.addColumnModel(fcColumnModel);

				tableModels.put(
						foreignClass.hashCode() + primaryClass.hashCode(), tm);
			}

		}
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
				Relation relation = getRelation(cls1, cls2);
				list = relations.get(cls1.hashCode());
				if (list == null) {
					list = new ArrayList<Relation>();
					relations.put(cls1.hashCode(), list);
				}
				list.add(relation);
			}
		}

		// 遍历
		if (relations != null && !relations.isEmpty()) {
			Iterator<Entry<Integer, List<Relation>>> iterator = relations
					.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Integer, List<Relation>> next = iterator.next();
				List<Relation> value = next.getValue();
			}
		}

		if (primaryKeyType == null) {
			primaryKeyType = "String";
		}
		// generatorDBHelper(clazzs);
		genaratorIDao();
		genaratorBaseDao();

		for (Class cls : clazzs) {
			// genaratorClassDao(cls);
			genaratorClassDaoHelper(cls);
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
		tm.setClazz(clazz);
		tm.setTableName(clazz.getSimpleName());

		Field[] fields = clazz.getDeclaredFields();
		List<ColumnModel> cms = new ArrayList<ColumnModel>();
		for (Field field : fields) {
			// 不是静态的
			if (!Modifier.isStatic(field.getModifiers())) {
				ColumnModel columnModel = getColumnModel(clazz, field);
				if (columnModel != null) {
					cms.add(columnModel);
					if (columnModel.isPrimaryKey()) {
						tm.setPrimaryKey(columnModel);
					}
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

		columnModel = new ColumnModel();
		columnModel.setColumnName(field.getName());
		columnModel.setColumnType(fieldTypeName);
		// 是本地字段
		columnModel.setLocal(true);
		if (fieldTypeName != null) {
			columnModel.setColumnJavaType(fieldType.getSimpleName());
			// 不是引用类型
			columnModel.setRefrenceOther(false);
			Column column = field.getAnnotation(Column.class);
			if (column != null) {
				if (column.primaryKey()) {
					// 如果是主键
					columnModel.setPrimaryKey(true);
				}
			}
		} else {
			// 如果是引用类型，则不设置columnJavaType;
			Class refrenceClass = fieldType;
			if (List.class.isAssignableFrom(refrenceClass)) {
				refrenceClass = getListTType(field);
				columnModel.setSet(true);
			}

			if (clazzs.contains(refrenceClass)) {
				columnModel.setRefrenceOther(true);
				columnModel.setRefrenceClass(refrenceClass);
				int type = getRelation(clazz, refrenceClass).getType();
				columnModel.setRefrenceType(type);
			} else {
				// 如果引用类型没有加入集合中（使用addClass方法），则不加入ColumnModel;
				columnModel = null;
			}

			// columnModel.setColumn
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
		List<Relation> list = relations.get(clazz.hashCode());
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
	 * 添加类的说明信息
	 */
	public void insertClassDesc() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 hh:mm:ss");
		String date = sdf.format(new Date(System.currentTimeMillis()));
		append("/**", 0, true);
		append(" *", 0, true);
		append(" * @author linyimu", 0, true);
		append(" * " + date, 0, true);
		append(" *", 0, true);
		append(" */", 0, true);
	}

	/**
	 * 生成DBHelper文件
	 * 
	 * @param clss
	 * @param isAccessParentField
	 */
	private void generatorDBHelper() {
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

		insertClassDesc();
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

		Iterator<Entry<Integer, TableModel>> iterator = tableModels.entrySet()
				.iterator();
		while (iterator.hasNext()) {
			Entry<Integer, TableModel> next = iterator.next();
			TableModel tm = next.getValue();

			// 创建表
			append("//创建" + tm.getTableName() + "表", 2, true);
			String sql = tm.getTableName() + "Sql";
			append("String " + sql + " = \"", 2, false);
			append(getCreateTableStr(tm), 0, false);
			append("\";", 0, true);
			// 执行SQL语句
			append("db.execSQL(" + sql + ");", 2, true);
			appendLine(2);

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
		insertClassDesc();
		append("public interface IDao<T> {", 0, true);
		appendLine(1);
		append("public T get(" + primaryKeyType + " id);", 1, true);
		appendLine(1);
		append("public List<T> getAll();", 1, true);
		appendLine(1);
		append("public long insert(T t);", 1, true);
		appendLine(1);
		append("public int deleteAll();", 1, true);
		appendLine(1);
		append("public int delete(" + primaryKeyType + "  id);", 1, true);
		appendLine(1);
		append("public int update(" + primaryKeyType
				+ " id, Map<String, Object> fields);", 1, true);
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
		insertClassDesc();
		append("public abstract class BaseDao<T> implements IDao<T> {", 0, true);
		appendLine(1);
		append("public DBHelper dbHelper;", 1, true);
		append("public SQLiteDatabase db;", 1, true);
		append("private Class clazz;", 1, true);
		append("public String tableName;", 1, true);
		append("public Context context;", 1, true);
		appendLine(1);
		append("public BaseDao(Context context) {", 1, true);
		append("this(context, null);", 2, true);
		append("}", 1, true);

		append("public BaseDao(Context context,String tablaName) {", 1, true);
		append("this.context = context;", 2, true);
		append("// 获取泛型T的类型", 2, true);
		append("Type genType = this.getClass().getGenericSuperclass();", 2,
				true);
		append("Type[] params = ((ParameterizedType) genType).getActualTypeArguments();",
				2, true);
		append("clazz = (Class<T>) params[0];", 2, true);
		append("this.tableName = tableName;", 2, true);
		append("if (tableName == null) {", 2, true);
		append("this.tableName = clazz.getSimpleName();", 3, true);
		append("}", 2, true);

		append("dbHelper = new DBHelper(context,context.getPackageName());", 2,
				true);

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
		append("public int delete(" + primaryKeyType + " id) {", 1, true);
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

	public String genaratorClassDao(TableModel tableModel) {
		if (!tableModel.isJoinTable()) {
			String tableName = tableModel.getTableName();
			String className = tableModel.getClazz().getSimpleName();
			ColumnModel parmaryKeyModle = tableModel.getPrimaryKey();

			builder = new StringBuilder();
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
			append("import " + tableModel.getClazz().getName() + ";", 0, true);
			appendLine(2);
			insertClassDesc();
			append("public class " + tableModel.getClazz().getSimpleName()
					+ "Dao extends BaseDao<" + className + "> {", 0, true);
			append("public " + className + "Dao(Context context) {", 1, true);
			append("super(context);", 2, true);
			append("}", 1, true);
			appendLine(2);

			// get(id)方法
			append("@Override", 1, true);
			append("public " + className + " get("
					+ parmaryKeyModle.getColumnJavaType() + " "
					+ parmaryKeyModle.getColumnName() + "2) {", 1, true);
			append("db = dbHelper.getReadableDatabase();", 2, true);
			append(className + " " + firstLettersLowercase(className)
					+ " = null;", 2, true);
			append("Cursor cursor = db.query(tableName, null, \""
					+ parmaryKeyModle.getColumnName()
					+ "=?\", new String[] { String.valueOf("
					+ parmaryKeyModle.getColumnName()
					+ "2) }, null, null, null);", 2, true);
			append("if (cursor.moveToNext()) {", 2, true);
			append(firstLettersLowercase(className) + " = new " + className
					+ "();", 3, true);
			appendLine(1);
			CursorToEntry(tableModel);
			append("}", 2, true);
			append("cursor.close();", 2, true);
			append("db.close();", 2, true);
			append("return " + firstLettersLowercase(tableName) + ";", 2, true);
			append("}", 1, true);
			appendLine(2);

			// getAll方法
			append("@Override", 1, true);
			append("public List<" + className + "> getAll() {", 1, true);
			append("db = dbHelper.getReadableDatabase();", 2, true);
			append("List<" + className + "> "
					+ firstLettersLowercase(className) + "s = new ArrayList<"
					+ className + ">();", 2, true);
			append("Cursor cursor = db.query(tableName, null, null,null, null, null, null);",
					2, true);

			append("while (cursor.moveToNext()) {", 2, true);
			append("" + className + " " + firstLettersLowercase(className)
					+ " = new " + className + "();", 3, true);
			CursorToEntry(tableModel);
			append(firstLettersLowercase(className) + "s.add("
					+ firstLettersLowercase(className) + ");", 3, true);
			append("}", 2, true);

			append("return " + firstLettersLowercase(className)
					+ "s.isEmpty()?null:" + firstLettersLowercase(className)
					+ "s;", 2, true);
			append("}", 1, true);
			appendLine(2);

			// insert方法
			append("@Override", 1, true);
			append("public long insert(" + className + " t) {", 1, true);
			append("db = dbHelper.getWritableDatabase();", 2, true);
			append("ContentValues values = new ContentValues();", 2, true);
			List<ColumnModel> columModels = tableModel.getColumModels();
			for (ColumnModel cm : columModels) {
				String columnName = cm.getColumnName();
				String classType = cm.getColumnJavaType();
				if (cm.isLocal()) {
					if (!cm.isRefrenceOther()) {
						if (classType.equalsIgnoreCase("boolean")) {
							append("values.put(\"" + columnName + "\",t.is"
									+ firstLettersUpper(columnName) + "());",
									2, true);
						} else {
							append("values.put(\"" + columnName + "\",t.get"
									+ firstLettersUpper(columnName) + "());",
									2, true);
						}
					} else {

						if (!cm.isSet()) {
							appendLine(1);

							Class refrenceClass = cm.getRefrenceClass();
							TableModel reTableModel = tableModels
									.get(refrenceClass.hashCode());
							ColumnModel rePrimaryKey = reTableModel
									.getPrimaryKey();
							append(refrenceClass.getSimpleName()
									+ " "
									+ firstLettersLowercase(refrenceClass.getSimpleName())
									+ " = t.get"
									+ firstLettersUpper(columnName) + "();", 2,
									true);
							append("if("
									+ firstLettersLowercase(refrenceClass.getSimpleName())
									+ " != null){", 2, true);
							append("values.put(\""
									+ reTableModel.getTableName()
									+ "_"
									+ rePrimaryKey.getColumnName()
									+ "\","
									+ firstLettersLowercase(refrenceClass.getSimpleName())
									+ ".get"
									+ firstLettersUpper(rePrimaryKey.getColumnName())
									+ "());", 3, true);

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
			append("public int update(" + parmaryKeyModle.getColumnJavaType()
					+ " id, Map<String, Object> fields) {", 1, true);
			append("db = dbHelper.getWritableDatabase();", 2, true);
			append("ContentValues values = new ContentValues();", 2, true);

			append("Iterator<Entry<String, Object>> iterator = fields.entrySet().iterator();",
					2, true);
			append("while (iterator.hasNext()) {", 2, true);
			append("Entry<String, Object> next = iterator.next();", 3, true);
			append("Object value = next.getValue();", 3, true);
			append("String key = next.getKey();", 3, true);

			boolean hasRefrence = false;

			for (ColumnModel cm : columModels) {
				if (cm.isLocal() && cm.isRefrenceOther()) {
					if (!cm.isSet()) {
						hasRefrence = true;
						Class refrenceClass = cm.getRefrenceClass();
						append("if(object instanceof "
								+ refrenceClass.getSimpleName() + "){", 3, true);
						TableModel reTableModel = tableModels.get(refrenceClass
								.hashCode());
						ColumnModel rePrimaryKey = reTableModel.getPrimaryKey();

						append(refrenceClass.getSimpleName()
								+ " "
								+ firstLettersLowercase(refrenceClass.getSimpleName()
										+ " = ("
										+ refrenceClass.getSimpleName()
										+ ")value;"), 4, true);

						append("values.put(\""
								+ reTableModel.getTableName()
								+ "_"
								+ rePrimaryKey.getColumnName()
								+ "\","
								+ firstLettersLowercase(refrenceClass.getSimpleName())
								+ ".get"
								+ firstLettersUpper(rePrimaryKey.getColumnName())
								+ "());", 4, true);

						append("} else ", 3, true);
					}
				}
			}
			if (hasRefrence) {
				append("{", 3, true);
				append("values.put(key,String.valueOf(value));", 4, true);
				append("}", 3, true);
			} else {
				append("values.put(key,String.valueOf(value));", 3, true);
			}

			append("}", 2, true);
			append("int n = db.update( tableName, values, \"id=?\", new String[] { String.valueOf(id) });",
					2, true);
			append("db.close();", 2, true);
			append("return n;", 2, true);
			append("}", 1, true);

			// 如果有关联关系，则提供相应的查询方法
			for (ColumnModel cm : columModels) {
				int type = cm.getRefrenceType();

				if (cm.isRefrenceOther()
						&& (type == Relation.MORE_TO_MORE || type == Relation.MORE_TO_ONE)) {
					ColumnModel primaryKey = tableModel.getPrimaryKey();

					Class refrenceClass = cm.getRefrenceClass();
					String refrenceClassName = refrenceClass.getSimpleName();

					String paramsName = tableModel.getClazz().getSimpleName()
							+ "_" + primaryKey.getColumnName();

					append("public List<" + refrenceClassName + "> get"
							+ refrenceClassName + "s("
							+ primaryKey.getColumnJavaType() + " " + paramsName
							+ "){", 1, true);
					append("db = dbHelper.getWritableDatabase();", 2, true);

					TableModel reTableModel = tableModels.get(refrenceClass
							.hashCode());

					ColumnModel rePrimaryKey = reTableModel.getPrimaryKey();
					String queryTable = null;
					if (type == Relation.MORE_TO_ONE) {
						queryTable = reTableModel.getTableName();
					} else {

						System.out.println(refrenceClass.getSimpleName() + ":"
								+ tableModel.getClazz().getSimpleName());
						TableModel tableModel2 = tableModels.get(refrenceClass
								.hashCode() + tableModel.getClazz().hashCode());
						queryTable = tableModel2.getTableName();
					}

					append("List<" + refrenceClassName + "> "
							+ firstLettersLowercase(refrenceClassName)
							+ "s = new ArrayList<" + refrenceClassName + ">();",
							2, true);

					append("Cursor cursor = db.query(\"" + queryTable
							+ "\", new String[]{\""
							+ reTableModel.getClazz().getSimpleName() + "_"
							+ reTableModel.getPrimaryKey().getColumnName()
							+ "\"}, \"" + paramsName
							+ "=?\", new String[]{String.valueOf(" + paramsName
							+ ")}, null, null, null)", 2, true);

					append("while(cursor.moveToNext()){", 2, true);
					append(rePrimaryKey.getColumnJavaType()
							+ " "
							+ firstLettersLowercase(rePrimaryKey.getColumnName())
							+ " = cursor.get"
							+ getDbTypeWithJavaType(rePrimaryKey.getColumnJavaType())
							+ "(cursor.getIndex(\"" + paramsName + "\"));", 3,
							true);
					append(refrenceClassName + " "
							+ firstLettersLowercase(refrenceClassName)
							+ " = new " + refrenceClassName + "();", 3, true);
					append(firstLettersLowercase(refrenceClassName)
							+ "set"
							+ rePrimaryKey.getColumnName()
							+ "("
							+ firstLettersLowercase(rePrimaryKey.getColumnName())
							+ ");", 3, true);

					append(firstLettersLowercase(refrenceClassName) + "s.add("
							+ firstLettersLowercase(refrenceClassName) + ");",
							3, true);
					append("}", 2, true);

					append("cursor.close();", 2, true);
					append("db.close();", 2, true);
					append("return " + firstLettersLowercase(refrenceClassName)
							+ "s.isEmpty()?null:"
							+ firstLettersLowercase(refrenceClassName) + "s;",
							2, true);
					append("}", 1, true);
					// append(firstLettersLowercase(refrenceClassName) + ".set"
					// +rePrimaryKey.getColumnName() + "(" + , tabSize,
					// isLineOther)

				}

			}

			append("}", 0, true);
			System.out.println(builder.toString());

			toFile(builder.toString(), className + "Dao.java");
			return builder.toString();
		} else {

			// 是连接表
		}

		return null;
	}

	private void CursorToEntry(TableModel tableModel) {
		List<ColumnModel> columModels = tableModel.getColumModels();
		for (ColumnModel cm : columModels) {
			if (!cm.isLocal()) {
				continue;
			}
			String columnName = cm.getColumnName();
			String className = tableModel.getClazz().getSimpleName();
			String type = getDbTypeWithJavaType(cm.getColumnJavaType());

			if ("boolean".equalsIgnoreCase(cm.getColumnJavaType())) {
				append(cm.getColumnJavaType() + " " + columnName
						+ " = cursor.getInt(cursor.getColumnIndex(\""
						+ columnName + "\")) == 1;", 3, true);
			} else if (cm.getColumnJavaType() != null) {
				append(cm.getColumnJavaType() + " " + columnName
						+ " = cursor.get" + firstLettersUpper(type)
						+ "(cursor.getColumnIndex(\"" + columnName + "\"));",
						3, true);
			} else {
				// 引用类型
				if (cm.isRefrenceOther()) {// 是本地字段
					Class refrenceClass = cm.getRefrenceClass();
					ColumnModel rePrimaryKey = tableModels.get(
							refrenceClass.hashCode()).getPrimaryKey();
					if (cm.isSet()) {
						// 集合（1：n,n:n)
						// TableModel jionTableModel = tableModels
						// .get(refrenceClass.hashCode()
						// + tableModel.hashCode());
						TableModel reTableModel = tableModels.get(refrenceClass
								.hashCode());
						String reClassName = reTableModel.getClazz()
								.getSimpleName();
						append(reClassName + "Dao "
								+ firstLettersLowercase(reClassName)
								+ "Dao = new " + reClassName + "Dao(context);",
								3, true);
						insertImportPackge(refrenceClass.getName());
						append("List<"
								+ reClassName
								+ "> "
								+ firstLettersLowercase(reClassName)
								+ "= "
								+ firstLettersLowercase(reClassName)
								+ "Dao.get"
								+ reClassName
								+ "s("
								+ firstLettersLowercase(className)
								+ ".get"
								+ firstLettersUpper(rePrimaryKey.getColumnName())
								+ "());", 3, true);

					} else {
						// 不是集合,取出ID，只引用的保存ID
						append(refrenceClass.getSimpleName() + " " + columnName
								+ "= new " + refrenceClass.getSimpleName()
								+ "();", 3, true);

						type = rePrimaryKey.getColumnType();

						if ("boolean".equals(type)) {
							append(rePrimaryKey.getColumnJavaType()
									+ " "
									+ firstLettersLowercase(refrenceClass
											.getSimpleName()
											+ "_"
											+ rePrimaryKey.getColumnName())
									+ " = cursor.getInt(cursor.getColumnIndex(\""
									+ refrenceClass.getSimpleName() + "_"
									+ rePrimaryKey.getColumnName() + "\"))==1;",
									3, true);

						} else {
							append(rePrimaryKey.getColumnJavaType()
									+ " "
									+ firstLettersLowercase(refrenceClass.getSimpleName()
											+ "_"
											+ rePrimaryKey.getColumnName())
									+ " = cursor.get" + firstLettersUpper(type)
									+ "(cursor.getColumnIndex(\""
									+ refrenceClass.getSimpleName() + "_"
									+ rePrimaryKey.getColumnName() + "\"));",
									3, true);
						}

						append(columnName
								+ ".set"
								+ firstLettersUpper(rePrimaryKey.getColumnName())
								+ "("
								+ firstLettersLowercase(refrenceClass.getSimpleName()
										+ "_" + rePrimaryKey.getColumnName())
								+ ");", 3, true);

					}
				}
			}

			append(firstLettersLowercase(className) + ".set"
					+ firstLettersUpper(columnName) + "(" + columnName + ");",
					3, true);

			appendLine(1);
		}

	}

	private String getDbTypeWithJavaType(String javaType) {
		if ("Integer".equalsIgnoreCase(javaType)) {
			return "Int";
		}
		if ("int".equalsIgnoreCase(javaType)) {
			return "Int";
		}
		if ("boolean".equalsIgnoreCase(javaType)) {
			return "Int";
		}

		if ("Long".equalsIgnoreCase(javaType)) {
			return "long";
		}
		if ("short".equalsIgnoreCase(javaType)) {
			return "short";
		}
		if ("float".equalsIgnoreCase(javaType)) {
			return "float";
		}
		if ("double".equalsIgnoreCase(javaType)) {
			return "double";
		}

		if ("char".equalsIgnoreCase(javaType)) {
			return "String";
		}
		if ("Character".equalsIgnoreCase(javaType)) {
			return "String";
		}
		if ("String".equalsIgnoreCase(javaType)) {
			return "String";
		}

		return "int";
	}

	/**
	 * 是否有外键
	 * 
	 * @param clazz
	 * @return
	 */
	public boolean isHasForegnkey(Class clazz) {
		// 关联关系
		List<Relation> list = relations.get(clazz.hashCode());
		boolean has = false;
		// 判断如果没有外键关系，则就不生成该文件
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				has = true;
				return has;
			}
		}
		return has;
	}

	/**
	 * 生成Dao帮助类，实现级联删除更新查询等操作
	 * 
	 * @param clazz
	 * @param true
	 */
	public void genaratorClassDaoHelper(Class clazz) {
		if (!isHasForegnkey(clazz)) {
			return;
		}

		// 关联关系
		List<Relation> list = relations.get(clazz.hashCode());
		List<String> importedPackage = new ArrayList<String>();
		builder = new StringBuilder();

		String tableName = clazz.getSimpleName();

		if (packageName != null) {
			append("package " + packageName + ";", 0, true);
			appendLine(1);
		}
		append("import java.util.Iterator;", 0, true);
		append("import java.util.List;", 0, true);
		append("import java.util.Map;", 0, true);
		append("import java.util.Map.Entry;", 0, true);
		append("import android.content.Context;", 0, true);
		appendLine(2);
		insertClassDesc();
		append("public class " + tableName + "DaoHelper {", 0, true);
		append("//声明变量", 1, true);
		append(tableName + "Dao" + " " + tableName.toLowerCase() + "Dao;", 1,
				true);

		insertImportPackge(clazz.getName());

		// 声明变量
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				Class foreignClass = re.getForeignClass();
				if (!importedPackage.contains(foreignClass.getName())) {
					insertImportPackge(foreignClass.getName());
				}
				String foreignClassName = foreignClass.getSimpleName();
				if (isHasForegnkey(foreignClass)) {
					// 如果有外键就生成DaoHelper
					append(foreignClassName + "DaoHelper" + " "
							+ foreignClassName.toLowerCase() + "DaoHelper;", 1,
							true);
					insertImportPackge(packageName + "." + foreignClassName
							+ "DaoHelper");
					importedPackage.add(packageName + "." + foreignClassName
							+ "DaoHelper");
				} else {
					// 生成Dao
					append(foreignClassName + "Dao" + " "
							+ foreignClassName.toLowerCase() + "Dao;", 1, true);
					insertImportPackge(packageName + "." + foreignClassName
							+ "Dao");
					importedPackage.add(packageName + "." + foreignClassName
							+ "Dao");
				}

			}
		}

		// 构造函数
		append("public " + tableName + "DaoHelper(Context context) {", 1, true);
		append(tableName.toLowerCase() + "Dao = new " + tableName
				+ "Dao(context);", 2, true);
		// 初始化变量
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				Class foreignClass = re.getForeignClass();
				String foreignClassName = foreignClass.getSimpleName();
				String dao = null;
				if (isHasForegnkey(foreignClass)) {
					dao = "DaoHelper";
				} else {
					dao = "Dao";
				}
				append(foreignClassName.toLowerCase() + dao + " = new "
						+ foreignClassName + dao + "(context);", 2, true);
			}
		}

		append("}", 1, true);
		appendLine(2);

		// get方法
		append("public " + tableName + " get(" + primaryKeyType + " id) {", 1,
				true);
		append(tableName + " " + firstLettersLowercase(tableName) + " = "
				+ firstLettersLowercase(tableName) + "Dao.get(id);", 2, true);
		append("if(" + firstLettersLowercase(tableName) + " == null){", 2, true);
		append("return null;", 3, true);
		append("}", 2, true);
		// 查找关联关系
		Field[] fields = clazz.getDeclaredFields();
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				String foreignClassName = re.getForeignClass().getSimpleName();
				String daoName = null;
				if (isHasForegnkey(re.getForeignClass())) {
					daoName = "DaoHelper";
				} else {
					daoName = "Dao";
				}

				int type = re.getType();
				if (type == Relation.MORE_TO_ONE || type == Relation.ONE_TO_ONE) {
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + " =  "
							+ firstLettersLowercase(tableName) + ".get"
							+ foreignClassName + "();", 2, true);

					append("if (" + firstLettersLowercase(foreignClassName)
							+ " != null){", 2, true);

					append(firstLettersLowercase(foreignClassName) + " = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId());", 3, true);
					append(firstLettersLowercase(tableName) + ".set"
							+ firstLettersUpper(re.getFieldName()) + "("
							+ firstLettersLowercase(foreignClassName) + ");",
							3, true);
					append("}", 2, true);
				} else if (type == Relation.MORE_TO_MORE
						|| type == Relation.ONE_TO_MORE) {
					append("List<" + foreignClassName + "> "
							+ firstLettersLowercase(foreignClassName) + "s =  "
							+ firstLettersLowercase(tableName) + ".get"
							+ foreignClassName + "();", 2, true);

					append("if (" + firstLettersLowercase(foreignClassName)
							+ "s != null){", 2, true);
					append("List<" + foreignClassName + "> "
							+ "tempList =  new ArrayList<" + foreignClassName
							+ ">();", 3, true);
					append("for(" + foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + ":"
							+ firstLettersLowercase(foreignClassName) + "s){",
							3, true);
					append(foreignClassName + " t = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId())", 4, true);
					append("if(t != null){", 4, true);
					append("tempList.add(t);", 5, true);
					append("}", 4, true);
					append("}", 3, true);

					append("if(!tempList.isEmpty()){", 3, true);
					append(firstLettersLowercase(tableName) + ".set"
							+ firstLettersUpper(re.getFieldName())
							+ "(tempList);", 4, true);
					append("}", 3, true);
					append("}", 2, true);
				}
			}
		}

		append("return  " + firstLettersLowercase(tableName) + ";", 2, true);
		append("}", 1, true);

		appendLine(2);

		// getAll
		append("public List<" + tableName + "> getAll() {", 1, true);
		append("List<" + tableName + "> all = "
				+ firstLettersLowercase(tableName) + "Dao.getAll();", 2, true);
		append("if(all == null){", 2, true);
		append("return  null;", 3, true);
		append("}", 2, true);
		append("for (" + tableName + " " + firstLettersLowercase(tableName)
				+ " : all) {", 2, true);
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				String foreignClassName = re.getForeignClass().getSimpleName();
				String daoName = null;
				if (isHasForegnkey(re.getForeignClass())) {
					daoName = "DaoHelper";
				} else {
					daoName = "Dao";
				}

				int type = re.getType();
				if (type == Relation.MORE_TO_ONE || type == Relation.ONE_TO_ONE) {
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + " =  "
							+ firstLettersLowercase(tableName) + ".get"
							+ foreignClassName + "();", 3, true);

					append("if (" + firstLettersLowercase(foreignClassName)
							+ " != null){", 3, true);

					append(firstLettersLowercase(foreignClassName) + " = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId());", 4, true);
					append(firstLettersLowercase(tableName) + ".set"
							+ firstLettersUpper(re.getFieldName()) + "("
							+ firstLettersLowercase(foreignClassName) + ");",
							4, true);
					append("}", 2, true);
				} else if (type == Relation.MORE_TO_MORE
						|| type == Relation.ONE_TO_MORE) {
					append("List<" + foreignClassName + "> "
							+ firstLettersLowercase(foreignClassName) + "s =  "
							+ firstLettersLowercase(tableName) + ".get"
							+ foreignClassName + "();", 3, true);

					append("if (" + firstLettersLowercase(foreignClassName)
							+ "s != null){", 3, true);
					append("List<" + foreignClassName + "> "
							+ "tempList =  new ArrayList<" + foreignClassName
							+ ">();", 4, true);
					append("for(" + foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + ":"
							+ firstLettersLowercase(foreignClassName) + "s){",
							4, true);
					append(foreignClassName + " t = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId())", 5, true);
					append("if(t != null){", 5, true);
					append("tempList.add(t);", 6, true);
					append("}", 5, true);
					append("}", 4, true);

					append("if(!tempList.isEmpty()){", 4, true);
					append(firstLettersLowercase(tableName) + ".set"
							+ firstLettersUpper(re.getFieldName())
							+ "(tempList);", 5, true);
					append("}", 4, true);
					append("}", 3, true);
				}
			}

		}
		append("}", 2, true);
		append("return all;", 2, true);
		append("}", 1, true);
		appendLine(2);

		// insert
		append("public long insert(" + tableName + " "
				+ firstLettersLowercase(tableName) + ") {", 1, true);
		append("long insert = " + firstLettersLowercase(tableName)
				+ "Dao.insert(" + firstLettersLowercase(tableName) + ");", 2,
				true);
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				String foreignClassName = re.getForeignClass().getSimpleName();
				String daoName = "Dao";
				if (isHasForegnkey(re.getForeignClass())) {
					daoName = "DaoHelper";
				}
				if (re.getType() == Relation.MORE_TO_ONE
						|| re.getType() == Relation.ONE_TO_ONE) {
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + " = "
							+ firstLettersLowercase(tableName) + ".get"
							+ foreignClassName + "();", 2, true);
					append("if(" + firstLettersLowercase(foreignClassName)
							+ " != null){", 2, true);
					append("//查看数据库中是否已经存在", 3, true);
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + "1 = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId());", 3, true);
					append("if(" + firstLettersLowercase(foreignClassName)
							+ "1 == null){", 3, true);
					append("//数据库中不存在，插入数据库", 4, true);
					append(firstLettersLowercase(foreignClassName) + daoName
							+ ".insert("
							+ firstLettersLowercase(foreignClassName) + ");",
							4, true);
					append("}", 3, true);
					append("}", 2, true);
				} else if (re.getType() == Relation.ONE_TO_MORE) {
					append("List<" + foreignClassName + ">"
							+ firstLettersLowercase(foreignClassName) + "s ="
							+ firstLettersLowercase(tableName) + ".get"
							+ firstLettersUpper(re.getFieldName()) + "();", 2,
							true);
					append("if(" + firstLettersLowercase(foreignClassName)
							+ "s != null){", 2, true);
					append("for(" + foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + ":"
							+ firstLettersLowercase(foreignClassName) + "s){",
							3, true);
					append("//查看数据库中是否已经存在", 4, true);
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + "1 = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId());", 4, true);

					append("if(" + firstLettersLowercase(foreignClassName)
							+ "1 == null){", 4, true);
					append("//数据库中不存在，插入数据库", 5, true);
					append(firstLettersLowercase(foreignClassName) + daoName
							+ ".insert("
							+ firstLettersLowercase(foreignClassName) + ");",
							5, true);

					append("}", 4, true);

					append("}", 3, true);
					append("}", 2, true);
				}
			}
		}
		append("return insert;", 2, true);
		append("}", 1, true);
		appendLine(2);

		// update
		append("/**", 1, true);
		append(" *", 1, true);
		append(" * update方法若存在关联关系，则关联的对象只能做插入操作，不能做更新操作", 1, true);
		append(" *", 1, true);
		append(" */", 1, true);
		append("public int update(int id, Map<String, Object> fields) {", 1,
				true);
		append("int update = " + firstLettersLowercase(tableName)
				+ "Dao.update(id, fields);", 2, true);

		append("Iterator<Entry<String, Object>> iterator = fields.entrySet().iterator();",
				2, true);
		append("while (iterator.hasNext()) {", 2, true);
		append("Entry<String, Object> next = iterator.next();", 3, true);
		append("Object value = next.getValue();", 3, true);

		boolean has1ton = false;
		// 判断是否有1：n的关系
		for (Relation re : list) {
			if (re.getFieldName() != null
					&& re.getType() == Relation.ONE_TO_MORE) {
				has1ton = true;
				break;
			}
		}
		if (has1ton) {
			append("if(value instanceof List<?>){", 3, true);
			append("List ls = (List)value;", 4, true);
			append("if(!ls.isEmpty()){", 4, true);
			append("Object obj = ls.get(0);", 5, true);
			for (Relation re : list) {
				if (re.getFieldName() != null) {
					String foreignClassName = re.getForeignClass()
							.getSimpleName();
					String daoName = "Dao";
					if (isHasForegnkey(re.getForeignClass())) {
						daoName = "DaoHelper";
					}
					if (re.getType() == Relation.ONE_TO_MORE) {
						append("if(obj instanceof " + foreignClassName + "){",
								5, true);
						append("List<" + foreignClassName + "> lss = (List<"
								+ foreignClassName + ">)value;", 6, true);
						append("for(" + foreignClassName + " "
								+ firstLettersLowercase(foreignClassName)
								+ ":lss){", 6, true);

						append(foreignClassName + " "
								+ firstLettersLowercase(foreignClassName)
								+ "1 = "
								+ firstLettersLowercase(foreignClassName)
								+ daoName + ".get("
								+ firstLettersLowercase(foreignClassName)
								+ ".getId());", 7, true);

						append("if(" + firstLettersLowercase(foreignClassName)
								+ "1 == null){", 7, true);
						append("//数据库中不存在，插入数据库", 8, true);
						append(firstLettersLowercase(foreignClassName)
								+ daoName + ".insert("
								+ firstLettersLowercase(foreignClassName)
								+ ");", 8, true);
						append("}", 7, true);
						append("}", 6, true);
						append("}", 5, true);
					}

				}

			}

			append("}", 4, true);
			append("}", 3, true);
		}

		// 判断是否有1:1或者n:1的关系
		boolean has1to1_nto1 = false;
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				if (re.getType() == Relation.MORE_TO_ONE
						|| re.getType() == Relation.ONE_TO_ONE) {
					has1to1_nto1 = true;
					break;
				}
			}
		}
		int baseSpace = 3;
		if (has1ton && has1to1_nto1) {
			append("}else{", 3, true);
			baseSpace = 4;
		}

		for (Relation re : list) {
			if (re.getFieldName() != null) {
				String foreignClassName = re.getForeignClass().getSimpleName();
				String daoName = "Dao";
				if (isHasForegnkey(re.getForeignClass())) {
					daoName = "DaoHelper";
				}

				if (re.getType() == Relation.MORE_TO_ONE
						|| re.getType() == Relation.ONE_TO_ONE) {

					append("if(value instanceof " + foreignClassName + "){",
							baseSpace, true);
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + " = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(((" + foreignClassName
							+ ") value).getId());", baseSpace + 1, true);

					append("if(" + firstLettersLowercase(foreignClassName)
							+ " == null){", baseSpace + 1, true);
					append("//数据库中不存在，插入数据库", baseSpace + 2, true);
					append(firstLettersLowercase(foreignClassName) + daoName
							+ ".insert((" + foreignClassName + ")value" + ");",
							baseSpace + 2, true);
					append("}", baseSpace + 1, true);
					append("}", baseSpace, true);

				}
			}

		}
		if (has1ton && has1to1_nto1) {
			append("}", baseSpace - 1, true);
		}
		append("}", 2, true);
		append("return update;", 2, true);
		append("}", 1, true);
		append("}", 0, true);
		System.out.println(builder.toString());

		toFile(builder.toString(), tableName + "DaoHelper.java");
	}

	/**
	 * 首字母大写
	 * 
	 * @param text
	 * @return
	 */
	public String firstLettersUpper(String text) {
		return text.substring(0, 1).toUpperCase() + text.substring(1);
	}

	/**
	 * 首字母小写
	 * 
	 * @param text
	 * @return
	 */
	public String firstLettersLowercase(String text) {
		return text.substring(0, 1).toLowerCase() + text.substring(1);
	}

	public String createObject(Class cls) {
		String simpleName = cls.getSimpleName();
		return simpleName + " " + firstLettersLowercase(simpleName) + " = new "
				+ simpleName + "();";
	}

	public String createObject(String className) {
		String simpleName = className;
		return simpleName + " " + firstLettersLowercase(simpleName) + " = new "
				+ simpleName + "();";
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
		String path = "src/";
		if (packageName != null) {
			String[] split = packageName.split("\\.");
			for (int i = 0; i < split.length; i++) {
				path = path + split[i] + "/";
			}
		}
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File(path, fileName);
		System.out.println(file.getAbsolutePath());
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(text.getBytes());
			fos.flush();
			fos.close();
		} catch (Exception e) {
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

		int indexOf = builder.indexOf("import");
		if (indexOf == -1) {
			indexOf = builder.indexOf("public class");
		}
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
	 * @param isAccessParentField
	 *            是否使用父类的字段
	 * @return
	 */
	@SuppressWarnings("unused")
	public String getCreateTableStr(TableModel tableModel) {
		// clazz包含的外键集合
		StringBuilder sql = new StringBuilder();

		String tableName = tableModel.getTableName();
		sql.append("create table if not exists  " + tableName + "(");
		List<ColumnModel> columnModels = tableModel.getColumModels();
		for (ColumnModel columnModel : columnModels) {
			String columnType = columnModel.getColumnType();
			if (columnType != null) {
				sql.append(columnModel.getColumnName() + " " + columnType);
				if (columnModel.isPrimaryKey()) {
					sql.append(" primary key");
				}

				if (columnModel.isRefrenceOther()) {
					Class refrenceClass = columnModel.getRefrenceClass();
					TableModel reTableModel = tableModels.get(refrenceClass
							.hashCode());
					sql.append(" FOREIGN KEY REFERENCES "
							+ reTableModel.getTableName() + "("
							+ reTableModel.getPrimaryKey().getColumnName()
							+ ")");

				}
				sql.append(",");
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
	 * @return
	 */
	private Relation getRelation(Class cls1, Class cls2) {
		Relation relationType = null;
		Relation relationType2 = null;

		// 取出cls1和cls2的关联关系
		Map<Integer, Relation> cls1AllRelation = getClassAllRelation(cls1);
		if (cls1AllRelation != null) {
			relationType = cls1AllRelation.get(cls2.hashCode());
		}
		// 取出cls2和cls1的关联关系
		Map<Integer, Relation> cls1AllRelation2 = getClassAllRelation(cls2);
		if (cls1AllRelation2 != null) {
			relationType2 = cls1AllRelation2.get(cls1.hashCode());
		}

		Relation relation = null;
		if (relationType != null) {
			relation = relationType.clone();
		} else {
			relation = new Relation();
			relation.setPrimaryClass(cls1);
			relation.setForeignClass(cls2);
		}
		int type1 = Relation.RELATION_NONE;
		int type2 = Relation.RELATION_NONE;
		if (relationType != null) {
			type1 = relationType.getType();
		}
		if (relationType2 != null) {
			type2 = relationType2.getType();
		}
		if (type1 == Relation.RELATION_NONE) {
			if (type2 == Relation.ONE_TO_MORE) {
				relation.setType(Relation.MORE_TO_ONE);
			} else if (type2 == Relation.MORE_TO_ONE) {
				relation.setType(Relation.ONE_TO_MORE);
			}
		} else if (type1 == Relation.ONE_TO_MORE
				&& type2 == Relation.ONE_TO_MORE) {
			relation.setType(Relation.MORE_TO_MORE);

		} else if (type1 == Relation.ONE_TO_MORE
				&& type2 == Relation.MORE_TO_ONE) {
			relation.setType(Relation.ONE_TO_MORE);

		} else if (type1 == Relation.MORE_TO_ONE
				&& type2 == Relation.ONE_TO_MORE) {
			relation.setType(Relation.MORE_TO_ONE);

		} else if (type1 == Relation.MORE_TO_ONE
				&& type2 == Relation.MORE_TO_ONE) {
			relation.setType(Relation.ONE_TO_ONE);
		}
		return relation;
	}

	/**
	 * 获取一个类的所有关联关系。单项的
	 * 
	 * @param cls1
	 * @return
	 */
	private Map<Integer, Relation> getClassAllRelation(Class cls1) {
		Map<Integer, Relation> relations = new HashMap<Integer, Relation>();
		// List<Relation> relations = new ArrayList<Relation>();
		Field[] fields1 = cls1.getDeclaredFields();
		Relation relation1 = null;
		for (Field field : fields1) {
			Class<?> type = field.getType();
			Class tClass = type;
			if (List.class.isAssignableFrom(type)) {// 如果是集合
				tClass = getListTType(field);
			}
			if (clazzs.contains(tClass)) {
				relation1 = new Relation();
				relation1.setFieldName(field.getName());
				relation1.setForeignClass(tClass);
				relation1.setPrimaryClass(cls1);
				if (List.class.isAssignableFrom(type)) {// 如果是集合
					// 获取泛型
					relation1.setType(Relation.ONE_TO_MORE);
				} else {
					relation1.setType(Relation.MORE_TO_ONE);
				}
				Relation relation = relations.get(tClass.hashCode());
				if (relation == null) {
					relations.put(tClass.hashCode(), relation1);
				}
				// System.out.println("==" + relation1);
			}
		}

		return relations.isEmpty() ? null : relations;

	}
}
