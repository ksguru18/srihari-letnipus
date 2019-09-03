/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.tls.policy.jdbi;

import com.intel.mtwilson.jdbi.util.JdbiUtil;
import com.intel.mtwilson.My;

/**
 *
 * @author jbuhacoff
 */
public class TlsPolicyJdbiFactory {

    public static TlsPolicyDAO tlsPolicyDAO() {
        try {
            return JdbiUtil.getDBI(My.jdbc().connection()).open(TlsPolicyDAO.class);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
