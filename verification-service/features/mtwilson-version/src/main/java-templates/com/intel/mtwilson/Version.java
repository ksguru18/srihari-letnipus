/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson;


/**
 * The constants in this class are substituted by Maven using the
 * templating-maven-plugin
 * @author jbuhacoff
 */
public class Version {
    public static final String VERSION = "${project.version}";
    public static final String ARTIFACT_ID = "${project.artifactId}";
    public static final String GROUP_ID = "${project.groupId}";
    public static final String GIT_BRANCH = "${git.branch}";
    public static final String GIT_COMMIT_ID = "${git.commit.id}";
    public static final String GIT_COMMIT_ABBREV = "${git.commit.abbrev}";
    public static final String GIT_COMMIT_TIME = "${git.commit.time}";
    public static final String BUILD_TIMESTAMP = "${timestamp}";

    private static final Version instance = new Version();
    
    public static Version getInstance() { return instance; }
    
    public String getVersion() { return VERSION; }
    public String getBranch() { return GIT_BRANCH; }
    public String getTimestamp() { return BUILD_TIMESTAMP; }

}
