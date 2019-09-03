/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.audit.helper;

import com.intel.mtwilson.audit.data.AuditContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dsmagadx
 */
public class AuditJerseyResponseFilter implements ContainerResponseFilter {
    private static Logger log = LoggerFactory.getLogger(AuditJerseyResponseFilter.class);
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {

        long endTime = System.currentTimeMillis();
        AuditContext auditContext = MtWilsonThreadLocal.get();
        if( auditContext != null ) {
        log.trace("AuditJerseyResponseFilter request for {} {} End {} Time {} ", new String[] 
                    { request.getMethod(), request.getUriInfo().getPath(), String.valueOf(endTime),String.valueOf(endTime -auditContext.getStartMilliseconds()) });
        }        
        // Remove the context from thread local
        
        MtWilsonThreadLocal.unset();
        log.trace("Removed the Audit context from thread local.");
//        return response;
    }
    
    
}
