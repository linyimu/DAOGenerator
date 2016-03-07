package com.lym.ainao.bean;

import java.util.List;

import com.lym.annotation.Column;

/**
 * ����
 * 
 * @author xuyao
 * 
 */
public class AlarmClock {
	/** �������� */
	public static final int type_normal = 0;
	@Column(primaryKey = true)
	private String objectId;
	private String title;
	private Integer minute;
	private Integer hour;
	private Integer daysOfWeek;
	/** �������� */
	private Integer type;
	private boolean open;
	private boolean vibration;
	/** �������� */
	// private AlarmClockTheme alarmClockTheme;
	// private User user;
	private List<AlarmClockTheme> alarmClockThemes;
}
