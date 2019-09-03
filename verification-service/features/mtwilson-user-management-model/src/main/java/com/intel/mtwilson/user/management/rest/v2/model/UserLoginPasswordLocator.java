/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.user.management.rest.v2.model;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.repository.Locator;
import javax.ws.rs.PathParam;

/**
 *
 * @author ssbangal
 */
public class UserLoginPasswordLocator implements Locator<UserLoginPassword> {

    @PathParam("user_id")
    public UUID userId;
    @PathParam("id")
    public UUID id;

    @Override
    public void copyTo(UserLoginPassword item) {
        if( id != null ) {
            item.setId(id);
        }
        if( userId != null ) {
            item.setUserId(userId);
        }
    }
    
}
