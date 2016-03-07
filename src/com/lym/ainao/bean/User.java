package com.lym.ainao.bean;

import java.util.List;

import com.lym.annotation.Column;

public class User {
	private static final Integer SEX_MAN = 0;
	private static final Integer SEX_WOMAN = 1;
	@Column(primaryKey = true)
	private String objectId;// ID
	private String userName;
	private String password;
	private Integer sex;
	private String phoneNumber;
	private boolean phoneVerification;// �绰�����Ƿ���֤
	private Integer loginType;// ��½��ʽ���û���/qq/΢�ţ�

	List<AlarmClock> alarmClock;
}
