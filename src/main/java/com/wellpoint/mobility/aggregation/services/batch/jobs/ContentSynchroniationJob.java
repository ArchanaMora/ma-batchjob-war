package com.wellpoint.mobility.aggregation.services.batch.jobs;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.log4j.Logger;

import com.wellpoint.mobility.aggregation.content.stellent.ContentFacade;
import com.wellpoint.mobility.aggregation.core.utilities.StopWatch;

@Singleton
@Startup
public class ContentSynchroniationJob {

	private static final Logger logger = Logger
			.getLogger(ContentSynchroniationJob.class);
	private static final boolean DEBUG_ENABLED = logger.isDebugEnabled();

	@EJB
	protected ContentFacade contentFacade;

	/**
	 * Scheduler to Synchronize the content everyday mid-night
	 */
	@Schedule(hour = "0", minute = "0", second = "*")
	public void synchronizeContent() {
		if (DEBUG_ENABLED)
			logger.debug("Synchronize the content via SCHEDULER");
		final StopWatch stopWatch = new StopWatch();

		if (DEBUG_ENABLED) {
			logger.debug("Starting task");
		}

		try {
			contentFacade.task();
		} catch (Exception e) {
			logger.error(e);

		}

		if (DEBUG_ENABLED) {
			logger.debug("End Tasks: EXIT. Runtime=" + stopWatch.stop());
		}

	}

}
