package com.lym.ainao.bean;

import java.util.List;

import com.lym.annotation.Column;

/**
 * 闹钟
 * 
 * @author xuyao
 * 
 */
public class AlarmClock {
	/** 常规类型 */
	public static final int type_normal = 0;
	@Column(primaryKey = true)
	private String objectId;
	private String title;
	private Integer minute;
	private Integer hour;
	private Integer daysOfWeek;
	/** 闹钟类型 */
	private Integer type;
	private boolean open;
	private boolean vibration;
	/** 闹钟主题 */
	// private AlarmClockTheme alarmClockTheme;
	// private User user;
	private List<AlarmClockTheme> alarmClockThemes;
}
