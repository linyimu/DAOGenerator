package com.lym.ainao.bean;

import java.util.List;

import com.lym.annotation.Column;

/**
 * ÄÖÖÓÖ÷Ìâ
 * 
 * @author xuyao
 * 
 */
public class AlarmClockTheme {
	@Column(primaryKey = true)
	private String objectId;
	private String title;
	private String musicUrl;
	private String bgUrl;
	private Integer type;
	private String message;
	private String answer;

	private List<AlarmClock> clocks;

}