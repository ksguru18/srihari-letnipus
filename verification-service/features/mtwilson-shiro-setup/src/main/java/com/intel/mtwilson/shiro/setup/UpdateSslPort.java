/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.shiro.setup;

import com.intel.mtwilson.Folders;
import com.intel.mtwilson.My;
import com.intel.mtwilson.setup.LocalSetupTask;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.config.Ini;

/**
 *
 * @author jbuhacoff
 */
public class UpdateSslPort extends LocalSetupTask {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UpdateSslPort.class);
    private File shiroIniFile;
    private int port = 443;
    
    @Override
    protected void configure() throws Exception {
        shiroIniFile = new File(Folders.configuration() + File.separator + "shiro.ini");
        if (!shiroIniFile.exists()) {
            configuration("File not found: shiro.ini");
        }
        URL url = My.configuration().getMtWilsonURL();
        if (url != null) {
            port = url.getPort();
            if (port == -1) {
                port = url.getDefaultPort();
                if (port == -1) {
                    log.debug("Using default port 443");
                    port = 443;
                }
            }
        }
    }
    
    @Override
    protected void validate() throws Exception {
        Ini shiroIni = new Ini();
        shiroIni.load(FileUtils.readFileToString(shiroIniFile));
        String portNumber = shiroIni.getSectionProperty("main","ssl.port",null);
        if( portNumber == null || Integer.valueOf(portNumber) != port ) {
            validation("shiro.ini [main] ssl.port is not up to date");
        }
        String enabledBoolean = shiroIni.getSectionProperty("main","ssl.enabled",null);
        if( enabledBoolean == null || Boolean.valueOf(enabledBoolean) != true ) {
            validation("shiro.ini [main] ssl.enabled is not up to date");
        }
    }
    
    @Override
    protected void execute() throws Exception {
        Ini shiroIni = new Ini();
        shiroIni.load(FileUtils.readFileToString(shiroIniFile));
        shiroIni.setSectionProperty("main","ssl.port",String.valueOf(port));
        shiroIni.setSectionProperty("main","ssl.enabled",String.valueOf(true));
        StringBuilder newShiroConfig = new StringBuilder();
        Collection<Ini.Section> sections = shiroIni.getSections();
        for (Ini.Section section : sections) {
            newShiroConfig.append(String.format("[%s]\r\n",section.getName()));
            for (String sectionKey : section.keySet()) {
                newShiroConfig.append(String.format("%s = %s\r\n",sectionKey,section.get(sectionKey)));
            }
        }
        
        FileUtils.writeStringToFile(shiroIniFile, newShiroConfig.toString());
    }
}
