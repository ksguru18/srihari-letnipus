/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.audit.data;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 *
 * @author dsmagadx
 */
public class AuditColumnData {
    
    private String name;
    private Object value;
    private Boolean isUpdated;
    
    @JsonProperty("isUpdated")
    public Boolean isUpdated() {
        return isUpdated;
    }
    @JsonProperty("isUpdated")
    public void setIsUpdated(Boolean isUpdated) {
        this.isUpdated = isUpdated;
    }
    @JsonProperty("columnName")
    public String getName() {
        return name;
    }
    @JsonProperty("columnName")
    public void setName(String name) {
        this.name = name;
    }
    @JsonProperty("value")
    public Object getValue() {
        return value;
    }
    @JsonProperty("value")
    public void setValue(Object value) {
        this.value = value;
    }
    
    
}
