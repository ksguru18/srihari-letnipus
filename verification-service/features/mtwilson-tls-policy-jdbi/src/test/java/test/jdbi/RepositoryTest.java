/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package test.jdbi;

import com.intel.mtwilson.tls.policy.jdbi.*;
import com.intel.dcsg.cpg.io.UUID;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If you are setting up a test environment you should run the unit tests
 * in this order:
 * 
 * testCreateRole
 * testCreateUser
 * 
 * 
 * References:
 * Validation queries: http://stackoverflow.com/questions/3668506/efficient-sql-test-query-or-validation-query-that-will-work-across-all-or-most
 * 
 * @author jbuhacoff
 */
public class RepositoryTest {
    private static Logger log = LoggerFactory.getLogger(RepositoryTest.class);

    @Test
    public void testCreateTlsPolicy() throws Exception {
        try(TlsPolicyDAO dao = TlsPolicyJdbiFactory.tlsPolicyDAO()) {
        
        //create a new record
            TlsPolicyRecord tlsPolicyRecord = new TlsPolicyRecord();
            tlsPolicyRecord.setId(new UUID());
            tlsPolicyRecord.setName("test policy 1");
            tlsPolicyRecord.setContentType("jks");
            tlsPolicyRecord.setContent(null);
            tlsPolicyRecord.setComment(null);
            
        dao.insertTlsPolicy(tlsPolicyRecord);

        log.debug("Created tls policy {} with id {}", tlsPolicyRecord.getName(), tlsPolicyRecord.getId());
        
        }
    }
    
}
