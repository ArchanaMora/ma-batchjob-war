/*
 * Copyright (c) 2014 www.wellpoint.com.  All rights reserved.
 *
 * This program contains proprietary and confidential information and trade
 * secrets of Wellpoint. This program may not be duplicated, disclosed or
 * provided to any third parties without the prior written consent of
 * Wellpoint. Disassembling or decompiling of the software and/or reverse
 * engineering of the object code are prohibited.
 */
package com.wellpoint.mobility.aggregation.services.batch;

import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.wellpoint.mobility.aggregation.content.stellent.ContentFacade;
import com.wellpoint.mobility.aggregation.core.composite.response.ApplicationResponse;
import com.wellpoint.mobility.aggregation.core.exceptionhandler.exceptions.ApplicationException;
import com.wellpoint.mobility.aggregation.core.metricsmanager.impl.MetricDataService;
import com.wellpoint.mobility.aggregation.core.utilities.StopWatch;
import com.wellpoint.mobility.aggregation.notifications.metricssummary.NotificationGenerator;
import com.wellpoint.mobility.aggregation.services.batch.jobs.CacheCleanUpJob;
import com.wellpoint.mobility.aggregation.services.batch.jobs.MetricsCleanUpJob;

/**
 * This class acts as a public facing REST interface to back end services.
 * 
 * @author edward.biton@wellpoint.com
 */
@Stateless
@Path("/BatchService")
public class BatchService // extends BaseComposite
{
	@EJB
	protected CacheCleanUpJob cacheCleanUpJob;

	@EJB
	protected MetricsCleanUpJob metricsCleanUpJob;

	@EJB
	protected MetricDataService metricDataService;

	@EJB
	protected NotificationGenerator notificationGenerator;

	@EJB
	protected ContentFacade contentFacade;

	/**
	 * Logger
	 */
	private static final Logger logger = Logger.getLogger(BatchService.class);
	private static final boolean INFO_LOGGER_ENABLED = logger.isInfoEnabled();
	private static final boolean DEBUG_LOGGER_ENABLED = logger.isDebugEnabled();

	/**
	 * Default constructor
	 */
	public BatchService()
	{
	}

	protected ApplicationResponse getNewApplicationResponse(final boolean error, final String errorMessage)
	{
		return getNewApplicationResponse(error, "", errorMessage);
	}

	protected ApplicationResponse getNewApplicationResponse(final boolean error, final String errorCode, final String errorMessage)
	{
		final ApplicationResponse applicationResponse = new ApplicationResponse();
		applicationResponse.setError(error);
		applicationResponse.setErrorCode(errorCode);
		applicationResponse.setErrorMessage(errorMessage);
		return applicationResponse;
	}

	/**
	 * Calls various backend cache clearing methods.
	 * 
	 * @return sets the error value to false if there's an exception.
	 * @throws ApplicationException
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/cacheCleanup")
	public ApplicationResponse cacheCleanup() throws ApplicationException
	{
		final StopWatch stopWatch = new StopWatch();

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.cacheCleanup(): ENTRY");
		}

		try
		{
			cacheCleanUpJob.cleanApplicationCache();
			cacheCleanUpJob.cleanMethodCache();
			cacheCleanUpJob.cleanUserCache();
		}
		catch (Exception e)
		{
			logger.error(e);
			return getNewApplicationResponse(true, e.getMessage());
		}

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.cacheCleanup(): EXIT. Runtime=" + stopWatch.stop());
		}

		return getNewApplicationResponse(false, "");

	} // of cacheCleanUp

	/**
	 * Calls various backend cache clearing methods.
	 * 
	 * @return sets the error value to false if there's an exception.
	 * @throws ApplicationException
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/metricCacheCleanup")
	public ApplicationResponse metricCacheCleanup() throws ApplicationException
	{
		final StopWatch stopWatch = new StopWatch();

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.metricCacheCleanup(): ENTRY");
		}
		final int rowsDeleted;
		try
		{
			rowsDeleted = metricsCleanUpJob.cleanupMetricData();
		}
		catch (Exception e)
		{
			logger.error(e);
			return getNewApplicationResponse(true, e.getMessage());
		}

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.metricCacheCleanup(): EXIT. Runtime=" + stopWatch.stop());
		}

		return getNewApplicationResponse(false, "Runtime=" + stopWatch.stop() + ". rowsDeleted=" + rowsDeleted);

	} // of metricCacheCleanup

	/**
	 * Calls metricDataServiceBean.rollupDailyMetrics(yesterday) to roll up metrics data.
	 * 
	 * @return sets the error value to false if there's an exception.
	 * @throws ApplicationException
	 */
	@SuppressWarnings("deprecation")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/rollupDailyMetrics")
	public ApplicationResponse rollupDailyMetrics() throws ApplicationException
	{
		final StopWatch stopWatch = new StopWatch();

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.rollupDailyMetrics(): ENTRY");
		}

		try
		{
			long currentTime = System.currentTimeMillis();
			Date currentDate = new Date(currentTime);
			Date yesterday = new Date(currentDate.getTime() - (1000 * 60 * 60 * 12)); // TODO: Edward, did you mean to
																						// multiply by 24 hours?????
			yesterday.setHours(0);
			yesterday.setMinutes(0);
			yesterday.setSeconds(0);

			if (INFO_LOGGER_ENABLED)
			{
				logger.info("BatchService.rollupDailyMetrics(): yesterday=" + yesterday + ", currentDate=" + currentDate + ", currentTime=" + currentTime);
			}

			metricDataService.rollupDailyMetrics(yesterday);
		}
		catch (Exception e)
		{
			logger.error(e);
			return getNewApplicationResponse(true, e.getMessage());
		}

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.rollupDailyMetrics(): EXIT. Runtime=" + stopWatch.stop());
		}

		return getNewApplicationResponse(false, "Runtime=" + stopWatch.stop());

	} // of rollupDailyMetrics

	/**
	 * Calls notificationGeneratorBean.generateSummary() to generate the monthly summary. On a monthly basis compile and
	 * email a summary of the performance and exception data.
	 * <p>
	 * Run on the 1st of each month at midnight
	 * 
	 * @return sets the error value to false if there's an exception.
	 * @throws ApplicationException
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/monthlySummaryNotification")
	public ApplicationResponse monthlySummaryNotification() throws ApplicationException
	{
		final StopWatch stopWatch = new StopWatch();

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.monthlySummaryNotification(): ENTRY");
		}

		try
		{
			notificationGenerator.generateSummary();
		}
		catch (Exception e)
		{
			logger.error(e);
			return getNewApplicationResponse(true, e.getMessage());
		}

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.monthlySummaryNotification(): EXIT. Runtime=" + stopWatch.stop());
		}

		return getNewApplicationResponse(false, "Runtime=" + stopWatch.stop());

	} // of monthlySummaryNotification

	/**
	 * Calls notificationGeneratorBean.generateSummary() to generate the weekly summary. On a weekly basis compile and
	 * email a summary of the performance and exception data.
	 * <p>
	 * Run every Sunday at midnight
	 * 
	 * @return sets the error value to false if there's an exception.
	 * @throws ApplicationException
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/propertiesFinderExecuteTask")
	public ApplicationResponse propertiesFinderExecuteTask() throws ApplicationException
	{
		final StopWatch stopWatch = new StopWatch();

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.propertiesFinderExecuteTask(): ENTRY");
		}

		try
		{
			contentFacade.task();
		}
		catch (Exception e)
		{
			logger.error(e);
			return getNewApplicationResponse(true, e.getMessage());
		}

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.propertiesFinderExecuteTask(): EXIT. Runtime=" + stopWatch.stop());
		}

		return getNewApplicationResponse(false, "Runtime=" + stopWatch.stop());

	} // of propertiesFinderExecuteTask

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/weeklySummaryNotification")
	public ApplicationResponse weeklySummaryNotification() throws ApplicationException
	{
		final StopWatch stopWatch = new StopWatch();

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.weeklySummaryNotification(): ENTRY");
		}

		try
		{
			notificationGenerator.generateSummary();
		}
		catch (Exception e)
		{
			logger.error(e);
			return getNewApplicationResponse(true, e.getMessage());
		}

		if (DEBUG_LOGGER_ENABLED)
		{
			logger.debug("BatchService.weeklySummaryNotification(): EXIT. Runtime=" + stopWatch.stop());
		}

		return getNewApplicationResponse(false, "Runtime=" + stopWatch.stop());

	} // of weeklySummaryNotification

} // of class
