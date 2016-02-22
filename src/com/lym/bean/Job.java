package com.lym.bean;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

public class Job {
	private int id;
	private Department dept;
	private Double salary;
	private String name;
	private Employees emp;
	//private List<Employees> emps;

	public static void main(String[] args) throws NoSuchFieldException, SecurityException {
		String s =  java.util.UUID.randomUUID().toString();  
		System.out.println();
	
	}
}
