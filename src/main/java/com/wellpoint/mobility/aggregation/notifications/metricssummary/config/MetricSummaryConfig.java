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
package com.wellpoint.mobility.aggregation.notifications.metricssummary.config;

import com.wellpoint.mobility.aggregation.core.configuration.impl.ConfigImpl;

/**
 * Contains all the 'variable' information for the sending of the performance and acceptance 
 * summary information.
 * 
 * @author frank.garber@wellpoint.com
 *
 */
public class MetricSummaryConfig extends ConfigImpl {

	private static final long serialVersionUID = 1L;
	public static final String BODY_DATA_MARKER = "|DATA|";
	public static final String SUBJECT_DATE_MARKER = "|DATE|";
	
	protected String to, cc, from, body, subject;

	public MetricSummaryConfig getDefaultConfig() {
		final MetricSummaryConfig metricSummaryConfig = new MetricSummaryConfig();
		metricSummaryConfig.setBody("This is the body of the message. The data goes here: " + BODY_DATA_MARKER);
		metricSummaryConfig.setCc("james.dean@wellpoint.com, frank.garber@wellpoint.com");
		metricSummaryConfig.setFrom("MetricSummaryGenerator@wellpoint.com");
		metricSummaryConfig.setSubject("This is the report for date: " + SUBJECT_DATE_MARKER);
		metricSummaryConfig.setTo("jim.beam@wellpoint.com, edwin@wellpoint.com");
		return metricSummaryConfig;
	} // of getDefaultConfig
	
	public String getTo()
	{
		return to;
	}

	public void setTo(String to)
	{
		this.to = to;
	}

	public String getCc()
	{
		return cc;
	}

	public void setCc(String cc)
	{
		this.cc = cc;
	}

	public String getFrom()
	{
		return from;
	}

	public void setFrom(String from)
	{
		this.from = from;
	}

	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	@Override
	public String toString()
	{
		return "MetricSummaryConfig [to=" + to + ", cc=" + cc + ", from=" + from + ", body=" + body + ", subject=" + subject + "]";
	}

} // of class
