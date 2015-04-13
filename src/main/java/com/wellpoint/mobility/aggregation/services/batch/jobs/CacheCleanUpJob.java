/*
 * Copyright (c) 2014 www.wellpoint.com.  All rights reserved.
 *
 * This program contains proprietary and confidential information and trade
 * secrets of Wellpoint. This program may not be duplicated, disclosed or
 * provided to any third parties without the prior written consent of
 * Wellpoint. Disassembling or decompiling of the software and/or reverse
 * engineering of the object code are prohibited.
 */
package com.wellpoint.mobility.aggregation.services.batch.jobs;

import java.util.Date;
import java.util.List;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;

import org.apache.log4j.Logger;

/**
 * This is a background job that cleans up expired cache data from the database
 * 
 * @author edward.biton@wellpoint.com
 */
@Singleton
@Startup
public class CacheCleanUpJob
{
	/**
	 * Entity Manager
	 */
	@PersistenceContext(unitName = "persistenceUnit", type = PersistenceContextType.TRANSACTION)
	private EntityManager entityManager;
	/**
	 * Logger
	 */
	private Logger logger = Logger.getLogger(CacheCleanUpJob.class);

	/**
	 * Constructor
	 */
	public CacheCleanUpJob()
	{
		logger.debug("CacheCleanUp()");
	}

	@Schedule(hour = "*", minute = "*", second = "0")
	public void clearExpiredCacheData()
	{
		logger.debug("TESTING THE TRANSACTION via SCHEDULER");
		long currentTime = System.currentTimeMillis();
		Date currentDate = new Date(currentTime);
		logger.info("currentDate=" + currentDate + ", currentTime=" + currentTime);
		cleanApplicationCache();
		cleanMethodCache();
		cleanUserCache();
		logger.debug("TESTING THE TRANSACTION via SCHEDULER - DONE");
	}

	/**
	 * A method to clean the application cache table
	 */
	public void cleanApplicationCache()
	{
		logger.debug("ENTRY");
		logger.info("Cleaning up ApplicationCache data from the database");
		long currentTime = System.currentTimeMillis();
		try
		{
			Query query = entityManager.createQuery("DELETE FROM ApplicationCache ac WHERE ac.expiresOn < :currentTime");
			query.setParameter("currentTime", currentTime);
			int deletedRows = query.executeUpdate();
			logger.info("Deleted " + deletedRows + " from ApplicationCache");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.error("An error occurred cleaning up the ApplicationCache data from the database");
		}
		logger.info("Cleaning up ApplicationCache data from the database completed");
		logger.debug("EXIT");
	}

	/**
	 * A method to clean the method cache table
	 */
	@SuppressWarnings("unchecked")
	public void cleanMethodCache()
	{
		logger.debug("ENTRY");
		logger.info("Cleaning up MethodCache/MethodCacheMethodParam data from the database");
		long currentTime = System.currentTimeMillis();
		boolean moreResults = true;
		Query query = entityManager.createQuery("SELECT mc.id FROM MethodCache mc WHERE mc.expiresOn < :currentTime", Long.class);
		query.setParameter("currentTime", currentTime);
		query.setMaxResults(50);
		int totalDeletedMethodCacheRows = 0;
		int totalDeletedMethodCacheMethodParamRows = 0;
		while (moreResults)
		{
			try
			{
				// first do a query on the ids that needs to be deleted
				List<Long> methodCacheIds = (List<Long>) query.getResultList();
				if (methodCacheIds.size() > 0)
				{
					Query deleteMethodCacheMethodParamQuery = entityManager.createQuery("DELETE FROM MethodCacheMethodParam mcmp WHERE mcmp.methodCache.id IN (:methodCacheIds)");
					deleteMethodCacheMethodParamQuery.setParameter("methodCacheIds", methodCacheIds);
					int deletedRows = deleteMethodCacheMethodParamQuery.executeUpdate();
					logger.info("Deleted " + deletedRows + " from MethodCacheMethodParam");
					totalDeletedMethodCacheMethodParamRows += deletedRows;
					
					Query deleteMethodCacheQuery = entityManager.createQuery("DELETE FROM MethodCache mc WHERE mc.id IN (:methodCacheIds)");
					deleteMethodCacheQuery.setParameter("methodCacheIds", methodCacheIds);
					deletedRows = deleteMethodCacheQuery.executeUpdate();
					logger.info("Deleted " + deletedRows + " from MethodCache");
					totalDeletedMethodCacheRows += deletedRows;
				}
				else
				{
					moreResults = false;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				logger.error("An error occurred cleaning up the MethodCache/MethodCacheMethodParam data from the database");
			}
		}
		logger.info("Deleted a total of " + totalDeletedMethodCacheMethodParamRows + " rows from MethodCacheMethodParam");
		logger.info("Deleted a total of " + totalDeletedMethodCacheRows + " rows from MethodCache");
		logger.info("Cleaning up MethodCache/MethodCacheMethodParam data from the database completed");
		logger.debug("EXIT");
	}

	/**
	 * A method to clean the user cache table
	 */
	public void cleanUserCache()
	{
		logger.debug("ENTRY");
		logger.info("Cleaning up UserCache data from the database");
		long currentTime = System.currentTimeMillis();
		try
		{
			Query query = entityManager.createQuery("DELETE FROM UserCache uc WHERE uc.expiresOn < :currentTime");
			query.setParameter("currentTime", currentTime);
			int deletedRows = query.executeUpdate();
			logger.info("Deleted " + deletedRows + " from UserCache");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.error("An error occurred cleaning up the UserCache data from the database");
		}
		logger.info("Cleaning up UserCache data from the database completed");
		logger.debug("EXIT");
	}

}
