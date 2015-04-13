/*
* Copyright (c) 2014 www.wellpoint.com.  All rights reserved.
*
* This program contains proprietary and confidential information and trade
* secrets of Wellpoint. This program may not be duplicated, disclosed or
* provided to any third parties without the prior written consent of
* Wellpoint. Disassembling or decompiling of the software and/or reverse
* engineering of the object code are prohibited.
* 
*/

package com.wellpoint.mobility.aggregation.notifications.metricssummary;

import javax.ejb.EJB;

import org.apache.log4j.Logger;

//TODO: Dead code. Logic moved to the BatchService class.


/**
 * On a weekly basis compile and email a summary of the performance and exception data.
 *  
 * Author: frank.garber@wellpoint.com
 * 
 */
//@Stateless
public class WeeklySummaryNotificationKicker {

	@EJB
	NotificationGenerator notificationGenerator;
	
	/**
	 * Logger
	 */
	private Logger logger = Logger.getLogger(WeeklySummaryNotificationKicker.class);

	// Run every Sunday at midnight
//	@Schedule(hour="0", dayOfWeek="Sun")
//	@Schedule(second="*/30")
	public void generateSummary() {
		logger.info("WeeklySummaryNotificationKicker.generateSummary(): ENTER");
		
		notificationGenerator.generateSummary();
		
		logger.info("WeeklySummaryNotificationKicker.generateSummary(): EXIT");
	} // of generateSummary
	

} // of class
