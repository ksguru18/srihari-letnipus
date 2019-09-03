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
public class RolePermissionLocator implements Locator<RolePermission> {

    @PathParam("role_id")
    public UUID roleId;

    @Override
    public void copyTo(RolePermission item) {
        if( roleId != null ) {
            item.setRoleId(roleId);
        }
    }
    
}
