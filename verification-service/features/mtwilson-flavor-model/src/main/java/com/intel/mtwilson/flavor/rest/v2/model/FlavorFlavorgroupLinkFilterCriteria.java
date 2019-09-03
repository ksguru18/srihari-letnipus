/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.flavor.rest.v2.model;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jaxrs2.DefaultFilterCriteria;
import com.intel.mtwilson.repository.FilterCriteria;
import javax.ws.rs.QueryParam;

/**
 *
 * @author hmgowda
 */
public class FlavorFlavorgroupLinkFilterCriteria extends DefaultFilterCriteria implements FilterCriteria<FlavorFlavorgroupLink>{
    @QueryParam("id")
    public UUID id;
    @QueryParam("flavorId")
    public UUID flavorId;
    @QueryParam("flavorgroupId")
    public UUID flavorgroupId;
}
