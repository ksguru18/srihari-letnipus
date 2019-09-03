/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.shiro.jdbi;

import com.intel.mtwilson.My;
import com.intel.mtwilson.launcher.ext.annotations.Background;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;

/**
 *
 * @author jbuhacoff
 */
@Background
public class ArchiveRequestLog implements Runnable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ArchiveRequestLog.class);

    @Override
    public void run() {
        try (LoginDAO dao = MyJdbi.authz()) {
            // get the configured window size (in time) 
            int expiresAfter = My.configuration().getAntiReplayProtectionWindowMilliseconds(); 
            Calendar expirationTime = Calendar.getInstance();
            expirationTime.add(Calendar.MILLISECOND, -expiresAfter);
            // delete requests older than the expiration time
            dao.deleteRequestLogEntriesEarlierThan(expirationTime.getTime());
        } catch (IOException | SQLException e) {
            log.error("Error while archiving old requests", e);
        }
    }
    
    
}
