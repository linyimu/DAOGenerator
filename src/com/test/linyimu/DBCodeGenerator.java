package com.test.linyimu;

import java.io.File;
import java.io.FileOutputStream;
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

import com.lym.bean.Student;
import com.lym.dao.bean.ColumnModel;
import com.lym.dao.bean.Relation;
import com.lym.dao.bean.TableModel;

/**
 * Android���ݿ����������
 * 
 * @author xuyao
 * 
 */
public class DBCodeGenerator {
	StringBuilder builder = null;
	List<Class> clazzs = null;
	Map<Integer, List<Relation>> relations = null;
	private static DBCodeGenerator instence;

	public DBCodeGenerator() {
		clazzs = new ArrayList<Class>();
		relations = new HashMap<Integer, List<Relation>>();
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

	/**
	 * ���������ͣ���int��String�ȣ�
	 */
	private String primaryKeyType;

	public DBCodeGenerator setPrimaryKeyType(String primaryKeyType) {
		this.primaryKeyType = primaryKeyType;
		return instence;
	}

	public void execute() {
		int size = clazzs.size();
		// ���ж�������Ĺ�ϵ
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

		if (primaryKeyType == null) {
			primaryKeyType = "int";
		}
		generatorDBHelper(clazzs, false);
		genaratorIDao();
		genaratorBaseDao();

		for (Class cls : clazzs) {
			genaratorClassDao(cls);
			genaratorClassDaoHelper(cls);
		}

		// getRelationType(Teacher.class);

	}

	/**
	 * ��ӡ������Ĺ�ϵ
	 * 
	 * @param relation
	 */
	private void printRelation(int relation) {
		if (relation == Relation.ONE_TO_ONE) {
			System.out.println("1:1");
		} else if (relation == Relation.ONE_TO_MORE) {
			System.out.println("1��n");
		} else if (relation == Relation.MORE_TO_ONE) {
			System.out.println("n��1");
		} else if (relation == Relation.MORE_TO_MORE) {
			System.out.println("m��n");
		} else if (relation == Relation.RELATION_NONE) {
			System.out.println("no ralation");
		} else {
			System.out.println("no ralation");
		}
	}

	/**
	 * ��ȡ���ģ��
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
			// ���Ǿ�̬��
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
	 * ��ȡ�е�ģ��
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
	 * ��ȡList���͵�����
	 * 
	 * @param field
	 * @return
	 */
	private Class getListTType(Field field) {
		// ��ȡ���Ϸ���
		Type tType = field.getGenericType();
		if (tType instanceof ParameterizedType) {
			Type[] actualTypeArguments = ((ParameterizedType) tType)
					.getActualTypeArguments();
			tType = actualTypeArguments[0];
		}
		// ���Ϸ�������
		Class tClass = (Class) tType;
		return tClass;
	}

	/**
	 * ��ȡ�������Sql���
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

		// ��ӹ�����ϵ
		List<Relation> list = relations.get(clazz.hashCode());
		if (list != null) {
			for (Relation rela : list) {
				// �й�����ϵ
				int relation = rela.getType();
				if (relation == -1) {

				} else if (relation == Relation.ONE_TO_ONE) {
					sqlBuilder.append(rela.getForeignClass().getSimpleName());
					sqlBuilder.append("_id");
					sqlBuilder.append(" integer");
					sqlBuilder.append(",");
				} else if (relation == Relation.ONE_TO_MORE) {
					// ������
				} else if (relation == Relation.MORE_TO_ONE) {
					sqlBuilder.append(rela.getForeignClass().getSimpleName());
					sqlBuilder.append("_id");
					sqlBuilder.append(" integer");
					sqlBuilder.append(",");
				} else if (relation == Relation.MORE_TO_MORE) {
					// �������ӱ�
					// �����м��
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
	 * ������˵����Ϣ
	 */
	public void insertClassDesc() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy��MM��dd�� hh:mm:ss");
		String date = sdf.format(new Date(System.currentTimeMillis()));
		append("/**", 0, true);
		append(" *", 0, true);
		append(" * @author linyimu", 0, true);
		append(" * " + date, 0, true);
		append(" *", 0, true);
		append(" */", 0, true);
	}

	/**
	 * ����DBHelper�ļ�
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

		// ������
		for (Class cls : clss) {
			append("//����" + cls.getSimpleName() + "��", 2, true);
			String sql = cls.getSimpleName() + "Sql";
			append("String " + sql + " = \"", 2, false);
			append(getCreateTableStr(cls, isAccessParentField), 0, false);
			append("\";", 0, true);
			// ִ��SQL���
			append("db.execSQL(" + sql + ");", 2, true);
			appendLine(2);

		}
		// ���������ı�
		// ִ��SQL���
		Set<Entry<Integer, String>> entrySet = relateSqls.entrySet();
		Iterator<Entry<Integer, String>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {

			Entry<Integer, String> next = iterator.next();
			String value = next.getValue();
			int index = value.indexOf("_");
			String tableName2 = value.substring(index + 1, value.indexOf("("));
			String tableName1 = value.substring(0, index).replace(
					"create table if not exists  ", "");
			append("//����" + tableName1 + "���" + tableName2 + "��֮������ӱ�"
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
	 * ����DAO�ӿ�
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
	 * ����BaseDao
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
		appendLine(1);
		append("public BaseDao(Context context) {", 1, true);
		append("this(context, null);", 2, true);
		append("}", 1, true);

		append("public BaseDao(Context context,String tablaName) {", 1, true);
		append("// ��ȡ����T������", 2, true);
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
		appendLine(2);
		insertClassDesc();
		append("public class " + clazz.getSimpleName() + "Dao extends BaseDao<"
				+ tableName + "> {", 0, true);
		append("public " + tableName + "Dao(Context context) {", 1, true);
		append("super(context);", 2, true);
		append("}", 1, true);
		appendLine(2);

		// get(id)����
		append("@Override", 1, true);
		append("public " + tableName + " get(" + primaryKeyType + " id2) {", 1,
				true);
		append("db = dbHelper.getReadableDatabase();", 2, true);
		append(tableName + " " + firstLettersLowercase(tableName) + " = null;",
				2, true);
		append("Cursor cursor = db.query(tableName, null, \"id=?\", new String[] { String.valueOf(id2) }, null, null, null);",
				2, true);
		append("if (cursor.moveToNext()) {", 2, true);
		append(firstLettersLowercase(tableName) + " = new " + tableName + "();",
				2, true);
		CursorToEntry(tableModel, tableName);
		append("}", 2, true);
		append("cursor.close();", 2, true);
		append("db.close();", 2, true);
		append("return " + firstLettersLowercase(tableName) + ";", 2, true);
		append("}", 1, true);
		appendLine(2);

		// getAll����
		append("@Override", 1, true);
		append("public List<" + tableName + "> getAll() {", 1, true);
		append("db = dbHelper.getReadableDatabase();", 2, true);
		append("List<" + tableName + "> " + firstLettersLowercase(tableName)
				+ "s = new ArrayList<" + tableName + ">();", 2, true);
		append("Cursor cursor = db.query(tableName, null, null,null, null, null, null);",
				2, true);

		append("while (cursor.moveToNext()) {", 2, true);
		append("" + tableName + " " + firstLettersLowercase(tableName)
				+ " = new " + tableName + "();", 3, true);
		CursorToEntry(tableModel, tableName);
		append(firstLettersLowercase(tableName) + "s.add("
				+ firstLettersLowercase(tableName) + ");", 3, true);
		append("}", 2, true);

		append("return " + tableName.toLowerCase() + "s.isEmpty()?null:"
				+ tableName.toLowerCase() + "s;", 2, true);
		append("}", 1, true);
		appendLine(2);

		// insert����
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

		// ������ϵ
		List<Relation> list = relations.get(clazz.hashCode());
		for (Relation re : list) {
			if (re.getType() == Relation.MORE_TO_ONE
					|| re.getType() == Relation.ONE_TO_ONE) {
				String fieldName = re.getFieldName();
				String fieldTypeName = re.getForeignClass().getSimpleName();
				// �����������ֶ�
				if (re.getFieldName() != null) {
					append(fieldTypeName + " " + fieldTypeName.toLowerCase()
							+ " = t.get"
							+ fieldName.substring(0, 1).toUpperCase()
							+ fieldName.substring(1) + "();", 2, true);
					append("if(" + fieldTypeName.toLowerCase() + "!=null){", 2,
							true);
					append("values.put(\"" + fieldTypeName + "_id\","
							+ fieldTypeName.toLowerCase() + ".getId());", 3,
							true);
					append("}", 2, true);
				}
			}
		}

		append("long id = db.insert(tableName, null, values);", 2, true);
		append("db.close();", 2, true);
		append("return id;", 2, true);
		append("}", 1, true);

		appendLine(2);
		// update����
		append("@Override", 1, true);
		append("public int update(" + primaryKeyType
				+ " id, Map<String, Object> fields) {", 1, true);
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

		// ��ȡ������ϵ
		List<Relation> list = relations.get(tableClass.hashCode());
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
							// �������õİ�
							insertImportPackge(importName);
						}
						String fieldName = field.getName();
						append(fieldTypeName + " "
								+ fieldTypeName.toLowerCase() + " = new "
								+ fieldTypeName + "();", 3, true);
						append(fieldTypeName.toLowerCase()
								+ ".setId(cursor.get"
								+ primaryKeyType.substring(0, 1).toUpperCase()
								+ primaryKeyType.substring(1).toLowerCase()
								+ "(cursor.getColumnIndex(\"" + fieldTypeName
								+ "_id\")" + "));", 3, true);

						append(tableName.toLowerCase() + ".set"
								+ fieldName.substring(0, 1).toUpperCase()
								+ fieldName.substring(1) + "("
								+ fieldTypeName.toLowerCase() + ");", 3, true);
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		// DBCodeGenerator instence2 = getInstence();
		// instence.addClass(Cource.class);
		// instence.addClass(Score.class);
		// instence.addClass(Student.class);
		// instence.addClass(Teacher.class);
		//
		// instence.execute();
		// Relation relation = instence.getRelation(Student.class,
		// Teacher.class);
		// System.out.println(relation);
	}

	/**
	 * �Ƿ������
	 * 
	 * @param clazz
	 * @return
	 */
	public boolean isHasForegnkey(Class clazz) {
		// ������ϵ
		List<Relation> list = relations.get(clazz.hashCode());
		boolean has = false;
		// �ж����û�������ϵ����Ͳ����ɸ��ļ�
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				has = true;
				return has;
			}
		}
		return has;
	}

	/**
	 * ����Dao�����࣬ʵ�ּ���ɾ�����²�ѯ�Ȳ���
	 * 
	 * @param clazz
	 * @param true
	 */
	public void genaratorClassDaoHelper(Class clazz) {
		if (!isHasForegnkey(clazz)) {
			return;
		}

		// ������ϵ
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
		append("//��������", 1, true);
		append(tableName + "Dao" + " " + tableName.toLowerCase() + "Dao;", 1,
				true);

		insertImportPackge(clazz.getName());

		// ��������
		for (Relation re : list) {
			if (re.getFieldName() != null) {
				Class foreignClass = re.getForeignClass();
				if (!importedPackage.contains(foreignClass.getName())) {
					insertImportPackge(foreignClass.getName());
				}
				String foreignClassName = foreignClass.getSimpleName();
				if (isHasForegnkey(foreignClass)) {
					// ��������������DaoHelper
					append(foreignClassName + "DaoHelper" + " "
							+ foreignClassName.toLowerCase() + "DaoHelper;", 1,
							true);
					insertImportPackge(packageName + "." + foreignClassName
							+ "DaoHelper");
					importedPackage.add(packageName + "." + foreignClassName
							+ "DaoHelper");
				} else {
					// ����Dao
					append(foreignClassName + "Dao" + " "
							+ foreignClassName.toLowerCase() + "Dao;", 1, true);
					insertImportPackge(packageName + "." + foreignClassName
							+ "Dao");
					importedPackage.add(packageName + "." + foreignClassName
							+ "Dao");
				}

			}
		}

		// ���캯��
		append("public " + tableName + "DaoHelper(Context context) {", 1, true);
		append(tableName.toLowerCase() + "Dao = new " + tableName
				+ "Dao(context);", 2, true);
		// ��ʼ������
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

		// get����
		append("public " + tableName + " get(" + primaryKeyType + " id) {", 1,
				true);
		append(tableName + " " + firstLettersLowercase(tableName) + " = "
				+ firstLettersLowercase(tableName) + "Dao.get(id);", 2, true);
		append("if(" + firstLettersLowercase(tableName) + " == null){", 2, true);
		append("return null;", 3, true);
		append("}", 2, true);
		// ���ҹ�����ϵ
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
					append("//�鿴���ݿ����Ƿ��Ѿ�����", 3, true);
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + "1 = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId());", 3, true);
					append("if(" + firstLettersLowercase(foreignClassName)
							+ "1 == null){", 3, true);
					append("//���ݿ��в����ڣ��������ݿ�", 4, true);
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
					append("//�鿴���ݿ����Ƿ��Ѿ�����", 4, true);
					append(foreignClassName + " "
							+ firstLettersLowercase(foreignClassName) + "1 = "
							+ firstLettersLowercase(foreignClassName) + daoName
							+ ".get(" + firstLettersLowercase(foreignClassName)
							+ ".getId());", 4, true);

					append("if(" + firstLettersLowercase(foreignClassName)
							+ "1 == null){", 4, true);
					append("//���ݿ��в����ڣ��������ݿ�", 5, true);
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
		append(" * update���������ڹ�����ϵ��������Ķ���ֻ����������������������²���", 1, true);
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
		// �ж��Ƿ���1��n�Ĺ�ϵ
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
						append("//���ݿ��в����ڣ��������ݿ�", 8, true);
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

		// �ж��Ƿ���1:1����n:1�Ĺ�ϵ
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
					append("//���ݿ��в����ڣ��������ݿ�", baseSpace + 2, true);
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
	 * ����ĸ��д
	 * 
	 * @param text
	 * @return
	 */
	public String firstLettersUpper(String text) {
		return text.substring(0, 1).toUpperCase() + text.substring(1);
	}

	/**
	 * ����ĸСд
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
	 * д���ļ�
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
	 * ����
	 * 
	 * @param n
	 */
	public void appendLine(int n) {
		for (int i = 0; i < n; i++)
			builder.append("\n");
	}

	/**
	 * ����ַ�
	 * 
	 * @param text
	 *            Ҫ��ӵ��ַ�
	 * @param tabSize
	 *            ����tab�ո�
	 * @param isLineOther
	 *            �Ƿ��ڽ�������
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
	 * �������ð�
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
	 * ��Զ��ϵ������ӱ�
	 */
	Map<Integer, String> relateSqls = new HashMap<Integer, String>();

	/**
	 * ��ȡ�������Sql���
	 * 
	 * @param clazz
	 * @param isAccessParentField
	 *            �Ƿ�ʹ�ø�����ֶ�
	 * @return
	 */
	@SuppressWarnings("unused")
	public String getCreateTableStr(Class clazz, boolean isAccessParentField) {
		// clazz�������������
		List<Class> forenkeyClass = new ArrayList<Class>();
		StringBuilder sql = new StringBuilder();

		String tableName = clazz.getSimpleName();
		sql.append("create table if not exists  " + tableName + "(");
		// �����ֶ�
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
			// ��̬�ֶ�ֱ������
			if (Modifier.isFinal(f.getModifiers())
					|| Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			String fieldName = f.getName();
			Class fieldType = f.getType();
			String typeName = fieldType.getName();
			String fieldTypeName = getFieldType(typeName);

			if (fieldTypeName == null) {
				// ˵������������
				// ����Ǽ���(1�Զ� �� ��Զ� �Ĺ�ϵ)
				if (Collection.class.isAssignableFrom(fieldType)) {
					// �õ����͵�����
					// �жϹ�������
					try {
						Class tClass = getListTType(f);
						boolean isForenkey1 = isForenkey(clazz, tClass);
						boolean isForenkey2 = isForenkey(tClass, clazz);
						// // �����1:n�Ĺ�ϵ
						if (isForenkey1) {
							// ������
							sql.append(tClass.getSimpleName() + "_id ");
							sql.append("integer,");
							forenkeyClass.add(tClass);
						}

						// ����Ƕ�Զ�Ĺ�ϵ
						if (isForenkey1 && isForenkey2) {
							// �����м��
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
					// ���Ǽ��ϣ�ֱ��������
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
		// ����Ƿ������
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
	 * c1�Ƿ���c2�����(˵����c2����c1�����û�����c1����c2�ļ��ϣ�
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	private boolean isForenkey(Class c1, Class c2) {
		// c1����c2������
		Field[] fields2 = c1.getDeclaredFields();
		for (Field f : fields2) {
			if (!Modifier.isStatic(f.getModifiers())) {
				if (c2 == f.getType()) {
					return true;
				}
			}
		}
		// c2����c1�ļ���
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
	 * ��ȡ�ֶε�����
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
	 * ��ȡ������Ĺ�����ϵ
	 * 
	 * @param cls1
	 * @param cls2
	 */
	// public int getRelation(Class cls1, Class cls2) {
	// // 1:1
	// boolean cls1HasCls2 = false;
	// boolean cls2HasCls1 = false;
	// // �Ƿ��������
	// boolean cls1HasCls2s = false;
	// boolean cls2HasCls1s = false;
	// Field[] fields1 = cls1.getDeclaredFields();
	// for (Field field : fields1) {
	// Class<?> type = field.getType();
	// if (type == cls2) {
	// // ˵��cls1����cls2������
	// cls1HasCls2 = true;
	// }
	// }
	//
	// Field[] fields2 = cls2.getDeclaredFields();
	// for (Field field : fields2) {
	// Class<?> type = field.getType();
	// if (type == cls1) {
	// // ˵��cls2����cls1������
	// cls2HasCls1 = true;
	// }
	// }
	//
	// if (cls1HasCls2 && cls2HasCls1) {
	// // 1:1�Ĺ�ϵ
	// return Relation.ONE_TO_ONE;
	// }
	// if (cls1HasCls2) {
	// // n:1�Ĺ�ϵ
	// return Relation.MORE_TO_ONE;
	// }
	//
	// if (cls2HasCls1) {
	// // 1:n�Ĺ�ϵ
	// return Relation.ONE_TO_MORE;
	// }
	//
	// for (Field field : fields1) {
	// Class<?> fieldType = field.getType();
	// if (List.class.isAssignableFrom(fieldType)) {// ����Ǽ���
	// Class tClass = getListTType(field);
	// if (tClass == cls2) {
	// cls1HasCls2s = true;
	// }
	// }
	// }
	//
	// for (Field field : fields2) {
	// Class<?> fieldType = field.getType();
	// if (List.class.isAssignableFrom(fieldType)) {// ����Ǽ���
	// Class tClass = getListTType(field);
	// if (tClass == cls1) {
	// cls2HasCls1s = true;
	// }
	// }
	// }
	//
	// if (cls1HasCls2s && cls2HasCls1s) {
	// // n:m
	// return Relation.MORE_TO_MORE;
	// }
	// if (cls1HasCls2s) {
	// // 1:n
	// return Relation.ONE_TO_MORE;
	// }
	// if (cls2HasCls1s) {
	// // n:1
	// return Relation.MORE_TO_ONE;
	// }
	//
	// return Relation.RELATION_NONE;
	// }

	// public static void main(String[] args) {
	// DBCodeGenerator instence2 = getInstence();
	// instence2.addClass(Student.class, Teacher.class);
	// Relation re = instence2.getRe(Teacher.class, Student.class);
	// System.out.println(re);
	// }

	/**
	 * ��ȡ������Ĺ�����ϵ
	 * 
	 * @param cls1
	 * @param cls2
	 * @return
	 */
	private Relation getRelation(Class cls1, Class cls2) {
		Relation relationType = null;
		Relation relationType2 = null;

		// ȡ��cls1��cls2�Ĺ�����ϵ
		Map<Integer, Relation> cls1AllRelation = getClassAllRelation(cls1);
		if (cls1AllRelation != null) {
			relationType = cls1AllRelation.get(cls2.hashCode());
		}
		// ȡ��cls2��cls1�Ĺ�����ϵ
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
	 * ��ȡһ��������й�����ϵ�������
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
			if (List.class.isAssignableFrom(type)) {// ����Ǽ���
				tClass = getListTType(field);
			}
			if (clazzs.contains(tClass)) {
				relation1 = new Relation();
				relation1.setFieldName(field.getName());
				relation1.setForeignClass(tClass);
				relation1.setPrimaryClass(cls1);
				if (List.class.isAssignableFrom(type)) {// ����Ǽ���
					// ��ȡ����
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
