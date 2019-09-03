/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.tag.common;

import com.intel.dcsg.cpg.crypto.RsaUtil;
import com.intel.dcsg.cpg.x509.X509Util;
import com.intel.mtwilson.tag.dao.jdbi.*;
import com.intel.mtwilson.tag.model.*;
import com.intel.dcsg.cpg.tls.policy.TlsPolicy;
import com.intel.mtwilson.tag.dao.TagJdbi;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jbuhacoff
 */
public class Global {
    private static Logger log = LoggerFactory.getLogger(Global.class);
    private static Configuration currentConfiguration = null;
    private static PrivateKey cakey = null; // private key to use for automatically signing new certificates
    private static X509Certificate cakeyCert = null; // the specific certificate corresponding to the private key
    private static List<X509Certificate> cacerts = null; // the list of all approved certificates (including cakeyCert)

    public static void reset() {
        currentConfiguration = null;
        cakey = null;
        cacerts = null;
    }
    
    public static PrivateKey cakey() {
        if( cakey == null ) {
            log.debug("Loading CA key...");
            try(FileDAO fileDao = TagJdbi.fileDao()) {
                File cakeyFile = fileDao.findByName("cakey");
                if( cakeyFile == null ) {
                    log.debug("Cannot find 'cakey' file");
                }
                else {
                    String content = new String(cakeyFile.getContent(), "UTF-8");
                    cakey = RsaUtil.decodePemPrivateKey(content);
                    cakeyCert = X509Util.decodePemCertificate(content);
                }
            }
            catch(Exception e) {
                log.error("Cannot load cakey", e);
                cakey = null;
                cakeyCert = null;
            }
        }
        return cakey;
    }
    
    public static X509Certificate cakeyCert() {
        cakey(); // loads the private key AND the certificate and initializes cakeyCert
        return cakeyCert; // either X509Certificate object or null if there was an error
    }
    
    public static List<X509Certificate> cacerts() {
        if( cacerts == null ) {
            log.debug("Loading CA cert...");
            FileDAO fileDao = null;
            try {
                fileDao = TagJdbi.fileDao();
                File cacertFile = fileDao.findByName("cacerts");
                if( cacertFile == null ) {
                    log.debug("Cannot find 'cacert' file");
                }
                else {
                    cacerts = X509Util.decodePemCertificates(new String(cacertFile.getContent(), "UTF-8"));
                }
            }
            catch(Exception e) {
                log.error("Cannot load cacerts", e);
                cacerts = null;
            }
            finally {
                if( fileDao != null ) { fileDao.close(); }
            }
        }
        return cacerts;
    }
        
}
