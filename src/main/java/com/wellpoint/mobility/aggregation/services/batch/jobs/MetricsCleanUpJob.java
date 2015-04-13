/**
* Copyright (c) 2014 www.wellpoint.com.  All rights reserved.
*
* This program contains proprietary and confidential information and trade
* secrets of Wellpoint. This program may not be duplicated, disclosed or
* provided to any third parties without the prior written consent of
* Wellpoint. Disassembling or decompiling of the software and/or reverse
* engineering of the object code are prohibited.
* 
* Author: frank.garber@wellpoint.com
* 
*/package com.wellpoint.mobility.aggregation.services.batch.jobs;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.resource.spi.IllegalStateException;

import org.apache.log4j.Logger;

import com.wellpoint.mobility.aggregation.core.configuration.ConfigurationManager;
import com.wellpoint.mobility.aggregation.core.configuration.impl.ConfigImpl;
import com.wellpoint.mobility.aggregation.core.utilities.Utils;

@Stateless
public class MetricsCleanUpJob {

	@EJB
	ConfigurationManager configurationManager;
	
	/**
	 * Entity Manager
	 */
	@PersistenceContext(unitName = "persistenceUnit", type = PersistenceContextType.TRANSACTION)
	private EntityManager entityManager;
	
	/**
	 * Logger
	 */
	private static final Logger logger = Logger.getLogger(MetricsCleanUpJob.class);
//	private static final boolean DEBUG_LOGGER_ENABLED = logger.isDebugEnabled();
	private static final boolean INFO_LOGGER_ENABLED = logger.isInfoEnabled();


	/**
	 * A helper class used to store and load the configuration information
	 */ 
	public static class MetricsCleanUpConfig extends ConfigImpl {

		private static final long serialVersionUID = 1L;
		
		public Long thresholdInMS = Utils.MS_IN_DAY * 30;
		public Integer numberOfRowsToDeleteAtATime = 50;
		
		@Override
		public MetricsCleanUpConfig getDefaultConfig()
		{
			return new MetricsCleanUpConfig();
		} // of getDefaultConfig

		@Override
		public String toString()
		{
			return "MetricsCleanUpConfig [thresholdInMS=" + thresholdInMS + ", numberOfRowsToDeleteAtATime=" + numberOfRowsToDeleteAtATime + "]";
		}
		
	} // of class MetricsCleanUpConfig 
	
	

	/**
	 * Gets the configuration information from the Configuration Manager. If the config isn't found the default is
	 * created, stored and returned.
	 * 
	 * @return a populated config object
	 * @throws IllegalStateException if the config can't be found or initialized
	 */
	protected MetricsCleanUpConfig getConfigInfo() throws IllegalStateException
	{
		// Load the threshold info. If it's not there set it to a default value.
		MetricsCleanUpConfig metricsCleanUpConfig = (MetricsCleanUpConfig) configurationManager.loadConfiguration(new MetricsCleanUpConfig(), true);
		if (INFO_LOGGER_ENABLED)
		{
			logger.info("MetricsCleanUp.getConfigInfo(): metricsCleanUpConfig=" + metricsCleanUpConfig);
		}
		if (metricsCleanUpConfig == null) {
			final String errMsg = "MetricsCleanUp.getConfigInfo(): No config info found!!!!";
			logger.error(errMsg);	
			throw new IllegalStateException(errMsg);
		} else {
			if (INFO_LOGGER_ENABLED)
			{
				logger.info("MetricsCleanUp.getConfigInfo(): metricsCleanUpConfig=" + metricsCleanUpConfig);
			}
		}
			
		return metricsCleanUpConfig;
	}


	/**
	 * This method removes 'old' metrics data from the DB. The tables (and their child tables) include<p>
	 * <ul>
	 * <li>ExceptionLog</li>
	 * <li>MetricMethodParam</li>
	 * <li>MetricsToday</li>
	 * <li>MetricsRecent</li>
	 * <li>MetricsDailyrollup</li>
	 * </ul>
	 * 
	 * The data is removed from the tables in 'chunks' governed by the Config setting in 'MetricsCleanUpConfig'.
	 * 
	 * @return the grand total count of all the rows deleted, or -1 if there was an issue (check logs).
	 * @throws IllegalStateException if an issue with the config object
	 */
	public int cleanupMetricData() throws IllegalStateException { 
		if (INFO_LOGGER_ENABLED)
		{
			logger.info("MetricsCleanUp.cleanupMetricData(): Enter");
		}
		
		
		final MetricsCleanUpConfig metricsCleanUpConfig = getConfigInfo();
		// Calculate the time in the past. 'Now' in MS - the config value (MS)
		final Date thresholdDate = new Date(new Date().getTime() - metricsCleanUpConfig.thresholdInMS);
		final Timestamp thresholdTimestamp = new Timestamp(thresholdDate.getTime());
		
		int rowsDeleted;
		int totalDeleted = 0;
		
		final Integer maxRowsToDeleteAtATime = metricsCleanUpConfig.numberOfRowsToDeleteAtATime;
		
		if (maxRowsToDeleteAtATime == null || maxRowsToDeleteAtATime > 50)
		{
			logger.warn("MetricsCleanUp.cleanupMetricData(): maxRowsToDeleteAtATime=" +maxRowsToDeleteAtATime + ". Returning.");
			return -1;
		}
		do
		{
			rowsDeleted = deleteFromParentChildTable("ExceptionLog", "ExceptionLogMethodParam", "exceptionLog", maxRowsToDeleteAtATime, thresholdTimestamp);
			totalDeleted += rowsDeleted;
		}
		while (rowsDeleted > 0);
		
		do
		{
			rowsDeleted = deleteFromParentChildTable("Metric", "MetricMethodParam", "metric", maxRowsToDeleteAtATime, thresholdTimestamp);
			totalDeleted += rowsDeleted;
		}
		while (rowsDeleted > 0);
		
		do
		{
			deleteFromTable("MetricsToday", maxRowsToDeleteAtATime, "tsDateTime", thresholdTimestamp);
			totalDeleted += rowsDeleted;
		}
		while (rowsDeleted > 0);
		
		do
		{
			deleteFromTable("MetricsRecent", maxRowsToDeleteAtATime, "tsDateTime", thresholdTimestamp);
			totalDeleted += rowsDeleted;
		}
		while (rowsDeleted > 0);
		
		do
		{
			deleteFromTable("MetricsDailyrollup", maxRowsToDeleteAtATime, "tsDate", thresholdTimestamp);
			totalDeleted += rowsDeleted;
		}
		while (rowsDeleted > 0);
		
		
		if (INFO_LOGGER_ENABLED)
		{
			logger.info("MetricsCleanUp.cleanupMetricData(): total delete count=" + totalDeleted);
			logger.info("MetricsCleanUp.cleanupMetricData(): Exit");
		}
		
		return totalDeleted;

	} // of cleanupMetricData
	
	
	/**
	 * Removes rows from both the parent and child tables (child first) garnered by the rowsToDelete and olderThanTS
	 * values.
	 * @param parentTableName
	 * @param childTableName
	 * @param childIdColumnName
	 * @param rowsToDelete
	 * @param olderThanTS
	 * @return the count of rows deleted in both the parent and child tables (may be more than 'rowsToDelete')
	 */
	protected int deleteFromParentChildTable(final String parentTableName, final String childTableName, final String childIdColumnName, 
		final int rowsToDelete, final Timestamp olderThanTS) {
		int totalRowsDelete = 0;
		logger.info("MetricsCleanUp.deleteFromParentChildTable(): parentTableName=" + parentTableName);
		
		// Find the ids from the parent table. Remove all the children, then the parents.
		Query query = entityManager.createQuery("Select id from " + parentTableName + " where updatedDate > :updatedDate");
		query.setParameter("updatedDate", olderThanTS);
		query.setMaxResults(rowsToDelete);
		
		@SuppressWarnings("unchecked")
		final List<Long> parentIdList = (List<Long>)query.getResultList();
		
		if (parentIdList.size() > 0)
		{
			query = entityManager.createQuery("Delete from " + childTableName + " ctn where ctn." + childIdColumnName + ".id in (:parentIds)");
			query.setParameter("parentIds", parentIdList);
			int rowsDeleted = query.executeUpdate();
			logger.info("MetricsCleanUp.deleteFromParentChildTable(): Child rows deleted from " + childTableName + " = " + rowsDeleted);
			totalRowsDelete += rowsDeleted;
			query = entityManager.createQuery("Delete from " + parentTableName + " where id in (:parentIds)");
			query.setParameter("parentIds", parentIdList);
			rowsDeleted = query.executeUpdate();
			logger.info("MetricsCleanUp.deleteFromParentChildTable(): Parent rows deleted from " + parentTableName + " = " + rowsDeleted);
			totalRowsDelete += rowsDeleted;
		}
		
		logger.info("MetricsCleanUp.deleteFromParentChildTable(): Total rows deleted = " + totalRowsDelete);	
		
		return totalRowsDelete;
		
	} // of deleteFromParentChildTable

	
	/**
	 * Removes rows from the table garnered by the rowsToDelete and olderThanTS
	 * values.
	 * @param tableName
	 * @param rowsToDelete
	 * @param dateTimeFieldName
	 * @param olderThanTS
	 * @return the count of rows deleted (will never be more than 'rowsToDelete')
	 */
	protected int deleteFromTable(final String tableName, final int rowsToDelete, final String dateTimeFieldName, final Timestamp olderThanTS) {
			int totalRowsDelete;
			logger.info("MetricsCleanUp.deleteFromTable(): tableName=" + tableName);
			
			final Query query = entityManager.createQuery("Delete from " + tableName + " where " + dateTimeFieldName + " > :updatedDate");
			query.setParameter("updatedDate", olderThanTS);
			query.setMaxResults(rowsToDelete);
			totalRowsDelete = query.executeUpdate();
			
			logger.info("MetricsCleanUp.deleteFromTable(): Rows deleted from " + tableName + " = " + totalRowsDelete);
			
			return totalRowsDelete;
			
		} // of deleteFromTable
		

} // of class
