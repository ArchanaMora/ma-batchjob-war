/*
 * Copyright (c) 2014 www.wellpoint.com.  All rights reserved.
 *
 * This program contains proprietary and confidential information and trade
 * secrets of Wellpoint. This program may not be duplicated, disclosed or
 * provided to any third parties without the prior written consent of
 * Wellpoint. Disassembling or decompiling of the software and/or reverse
 * engineering of the object code are prohibited.
 */
package com.wellpoint.mobility.aggregation.services;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Required subclass of the Application to enable stateless bean exposed as REST/JSON service
 * 
 * @author edward.biton@wellpoint.com
 */
@ApplicationPath("/rest")
public class RestApplication extends Application
{
}
