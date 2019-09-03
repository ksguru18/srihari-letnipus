/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.flavor.rest.v2.rpc;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.My;
import com.intel.mtwilson.flavor.data.MwHostCredential;
import com.intel.mtwilson.launcher.ws.ext.RPC;
import com.intel.mtwilson.repository.RepositoryCreateException;
import java.util.Calendar;
import java.util.List;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 *
 * @author ssbangal
 */
@RPC("store-host-pre-registration-details")
@JacksonXmlRootElement(localName = "store_host_pre_registration_details")
public class StoreHostPreRegistrationDetailsRunnable implements Runnable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreHostPreRegistrationDetailsRunnable.class);
    
    private List<String> hostNames;
    private String userName;
    private String password;

    public List<String> getHostNames() {
        return hostNames;
    }

    public void setHostNames(List<String> hostNames) {
        this.hostNames = hostNames;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    
    @Override
    @RequiresPermissions({"store_host_pre_registration_details:create"})
    public void run() {
        try {
            if (hostNames != null && !hostNames.isEmpty() && userName != null && !userName.isEmpty()) {

                log.debug("About to store the pre-registration data for hosts {}", hostNames.toString());

                for(String hostName : hostNames) {
                    MwHostCredential credential = new MwHostCredential();
                    String id = new UUID().toString();
                    credential.setId(id);
                    credential.setHostName(hostName);
                    credential.setCredential(String.format("u=%s;p=%s", userName, password));
                    credential.setCreatedTs(Calendar.getInstance().getTime());
                    My.jpa().mwHostCredential().create(credential);
                }
            } else {
                throw new Exception("Invalid input specified or input value missing.");
            }
        } catch (Exception ex) {
            log.error("Error during pre-registration of host data.", ex);
            throw new RepositoryCreateException();
        }
    }    
}
