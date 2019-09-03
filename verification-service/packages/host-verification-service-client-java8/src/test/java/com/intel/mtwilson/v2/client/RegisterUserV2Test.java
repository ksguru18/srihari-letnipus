/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.v2.client;

import com.intel.dcsg.cpg.crypto.CryptographyException;
import com.intel.dcsg.cpg.crypto.SimpleKeystore;
import com.intel.dcsg.cpg.io.ByteArrayResource;
import com.intel.dcsg.cpg.io.FileResource;
import com.intel.mtwilson.My;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.junit.Test;

/**
 *
 * @author ssbangal
 */
public class RegisterUserV2Test {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegisterUserV2Test.class);
   
    @Test
    public void testRegisterUserV2() throws MalformedURLException, IOException, CryptographyException {
        
        String userName = "TestAdmin999";
        String password = "password";
        URL server = new URL("https://192.168.0.1:8181/mtwilson/v2/");
        
        ByteArrayResource certResource = new ByteArrayResource();

        // TODO: use MyConfiguration to select an appropriate path for local platform
        FileResource resource = new FileResource(new java.io.File("c:\\intel\\mtwilson\\"+userName+".jks"));
        Properties properties = My.configuration().getClientProperties();
        SimpleKeystore keystore = MwClientUtil.createUserInResourceV2(resource, userName, password, server, properties, "Admin role needed", null, "TLS");

    }
     
}
