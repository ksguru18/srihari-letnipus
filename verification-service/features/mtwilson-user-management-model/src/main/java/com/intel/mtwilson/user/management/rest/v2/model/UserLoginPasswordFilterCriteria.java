/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.user.management.rest.v2.model;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.repository.FilterCriteria;
import com.intel.mtwilson.jaxrs2.DefaultFilterCriteria;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 *
 * @author ssbangal
 */
public class UserLoginPasswordFilterCriteria extends DefaultFilterCriteria implements FilterCriteria<UserLoginPassword> {

    @PathParam("user_id")
    public UUID userUuid;
    @QueryParam("id")
    public UUID id;
    @QueryParam("status")
    public Status status;
    @QueryParam("enabled")
    public Boolean enabled;
    
}
