/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.tag.setup;

import com.intel.dcsg.cpg.console.Command;
import com.intel.dcsg.cpg.console.input.Input;
import java.io.IOException;
import org.apache.commons.configuration.Configuration;
import com.intel.mtwilson.crypto.password.GuardedPassword;
import java.security.GeneralSecurityException;

/**
 * @author jbuhacoff
 */
public abstract class TagCommand implements Command {
    private Configuration options = null;

    @Override
    public void setOptions(Configuration options) {
        this.options = options;
    }
    
    public Configuration getOptions() { 
        return options; 
    }

    @Override
    public abstract void execute(String[] args) throws Exception;
    
    // convenience methods
    
    /**
     * Use this method when you need the user to set a password for a new key. 
     * If an environment variable is provided as an option, its value is used.
     * Otherwise, the user is prompted for the password twice (to confirm).
     * 
     * If an environment variable is provided but is empty, the user is prompted.
     * 
     * @param label human-readable text to incorporate into the prompt, for example "the Data Encryption Key"
     * @param optName the name of the command-line option that can be used to name an environment variable containing the password (option value never used as the password itself)
     * @throws IOException 
     */
    public String getNewPassword(String label, String optName) throws IOException {
        GuardedPassword guardedPassword = new GuardedPassword();
        try {
           guardedPassword.setPassword("");
           if( options != null && options.containsKey(optName) ) {
               String passwordVar = options.getString(optName);
               guardedPassword.setPassword(System.getenv(passwordVar));
               if( !guardedPassword.isPasswordValid() ) {
                   System.err.println(String.format("Cannot get password from environment variable '%s' specified by option '%s'", passwordVar, optName));
               }
           }
           if( !guardedPassword.isPasswordValid() ) {
               guardedPassword.setPassword(Input.getConfirmedPasswordWithPrompt(String.format("You must protect %s with a password.", label))); // throws IOException, or always returns value or expression
           }
        } catch (GeneralSecurityException ex) {
             System.out.println("GeneralSecurityException:" + ex);
        }
        return guardedPassword.getInsPassword();
    }
    
    /**
     * Use this method when you need the user to provide a password for an existing key. 
     * If an environment variable is provided as an option, its value is used.
     * Otherwise, the user is prompted for the password just once.
     * 
     * If an environment variable is provided but is empty, the user is prompted.
     * 
     * @param label human-readable text to incorporate into the prompt, for example "the Data Encryption Key"
     * @param optName the name of the command-line option that can be used to name an environment variable containing the password (option value never used as the password itself)
     * @return
     * @throws IOException 
     */
    public String getExistingPassword(String label, String optName) throws IOException {
        String password = null;
        if( options != null && options.containsKey(optName) ) {
            String passwordVar = options.getString(optName);
            password = System.getenv(passwordVar);
            if( password == null ) {
                System.err.println(String.format("Cannot get password from environment variable '%s' specified by option '%s'", passwordVar, optName));
            }
        }
        if( password == null ) {
            password = Input.getRequiredPasswordWithPrompt(String.format("A password is required to unlock %s.", label)); // throws IOException, or always returns value or expression
        }
        return password;        
    }
}
