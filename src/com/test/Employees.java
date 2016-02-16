package com.test;

import java.util.List;

public class Employees {
	private int id;
	private int salary;
	private String name;
	private Department dept;
//	private Job job;
	//private List<Job> jobs;

	public Employees() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Employees(int id, int salary, String name, Department dept) {
		super();
		this.id = id;
		this.salary = salary;
		this.name = name;
		// this.dept = dept;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getSalary() {
		return salary;
	}

	public void setSalary(int salary) {
		this.salary = salary;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// public Department getDept() {
	// return dept;
	// }
	//
	// public void setDept(Department dept) {
	// this.dept = dept;
	// }

	// @Override
	// public String toString() {
	// return "Employees [id=" + id + ", salary=" + salary + ", name=" + name
	// + ", dept=" + dept + "]";
	// }

}
