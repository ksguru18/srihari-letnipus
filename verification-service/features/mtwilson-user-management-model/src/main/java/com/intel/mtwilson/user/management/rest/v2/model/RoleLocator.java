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
public class RoleLocator implements Locator<Role> {

    @PathParam("id")
    public UUID id;

    @Override
    public void copyTo(Role item) {
        if( id != null ) {
            item.setId(id);
        }
    }
    
}
