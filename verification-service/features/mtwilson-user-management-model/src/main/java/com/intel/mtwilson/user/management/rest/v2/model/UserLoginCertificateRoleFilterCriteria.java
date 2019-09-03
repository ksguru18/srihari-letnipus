/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.user.management.rest.v2.model;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.repository.FilterCriteria;
import javax.ws.rs.QueryParam;

/**
 *
 * @author ssbangal
 */
public class UserLoginCertificateRoleFilterCriteria implements FilterCriteria<UserLoginCertificateRole> {

    @QueryParam("loginCertificateIdEqualTo")
    public UUID loginCertificateIdEqualTo;
    @QueryParam("roleIdEqualTo")
    public UUID roleIdEqualTo;
}
