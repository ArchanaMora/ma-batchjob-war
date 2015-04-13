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

import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.wellpoint.mobility.aggregation.notifications.metricssummary.config.MetricSummaryConfig;
import com.wellpoint.mobility.aggregation.core.configuration.ConfigurationManager;
import com.wellpoint.mobility.aggregation.core.utilities.EmailClient;

/**
 * On a weekly basis compile and email a summary of the performance and exception data.
 *  
 * Author: frank.garber@wellpoint.com
 * 
 */
@Stateless
public class NotificationGenerator {

	@EJB
	ConfigurationManager configurationManager;
	
	@PersistenceContext(unitName = "persistenceUnit", type = PersistenceContextType.TRANSACTION)
	private EntityManager entityManager;
	
	@EJB
	EmailClient emailClient;
	
	private Logger logger = Logger.getLogger(NotificationGenerator.class);

	
	/**
	 * Perform the necessary queries and email the data.
	 */
	public void generateSummary() {

		logger.info("NotificationGenerator.generateSummary(): ENTER");
		
		final MetricSummaryConfig config = getConfiguration();
		
		// Collect the data for the email message
		final Long rowCount = (Long)entityManager.createQuery("select count(*) from ExceptionLog").getSingleResult();
		
		String subject = config.getSubject();
		if (!StringUtils.isEmpty(subject) && subject.indexOf(MetricSummaryConfig.SUBJECT_DATE_MARKER) != -1)
		{
			subject = StringUtils.replace(subject, MetricSummaryConfig.SUBJECT_DATE_MARKER, new Date().toString());
		}
		
		String body = config.getBody();
		if (!StringUtils.isEmpty(body) && body.indexOf(MetricSummaryConfig.BODY_DATA_MARKER) != -1)
		{
			body = StringUtils.replace(body, MetricSummaryConfig.BODY_DATA_MARKER, "Row count=" + rowCount);
		}
		
		logger.info("NotificationGenerator.generateSummary(): body='" + body + "', subject='" + subject + "'");
		
		try
		{
			emailClient.sendMessage(config.getTo(), config.getCc(), config.getFrom(), subject, body);
		}
		catch (Exception e)
		{
			logger.error("NotificationGenerator.generateSummary(): Unable to send email message. Exception message=" + e.getMessage());
		}
		
		logger.info("NotificationGenerator.generateSummary(): EXIT");
		
	} // of generateSummary
	
	
	/**
	 * Loads the configuration object. If not present, a default will be created.
	 * 
	 * @return a populated config object
	 */
	public MetricSummaryConfig getConfiguration() {
		MetricSummaryConfig metricSummaryConfig = new MetricSummaryConfig();
		metricSummaryConfig = (MetricSummaryConfig) configurationManager.loadConfiguration(metricSummaryConfig, true);
		if (metricSummaryConfig == null)
		{
			throw new IllegalStateException("There should ALWAYS be a default available! Did someone modify the MetricSummaryConfig class?");
		} // of if
		
		return metricSummaryConfig;
		
	} // of getConfiguration
	
} // of class
