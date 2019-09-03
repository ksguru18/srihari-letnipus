/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.shiro.jdbi;

import com.intel.mtwilson.user.management.rest.v2.model.Status;
import com.intel.mtwilson.user.management.rest.v2.model.UserLoginCertificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.intel.dcsg.cpg.io.UUID;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
  id uuid DEFAULT NULL,
  user_id uuid DEFAULT NULL,
  certificate bytea NOT NULL,
  sha1_hash bytea NOT NULL,
  expires timestamp DEFAULT NULL,
  enabled boolean NOT NULL DEFAULT '0',
  status varchar(128) NOT NULL DEFAULT 'Pending',
  comment text,
 * 
 * @author jbuhacoff
 */
public class UserLoginCertificateResultMapper implements ResultSetMapper<UserLoginCertificate> {

    @Override
    public UserLoginCertificate map(int i, ResultSet rs, StatementContext sc) throws SQLException {
        UserLoginCertificate userLoginCertificate = new UserLoginCertificate();
        userLoginCertificate.setId(UUID.valueOf(rs.getString("id")));
        userLoginCertificate.setUserId(UUID.valueOf(rs.getString("user_id")));
        userLoginCertificate.setCertificate(rs.getBytes("certificate"));
        userLoginCertificate.setSha1Hash(rs.getBytes("sha1_hash"));
        userLoginCertificate.setSha256Hash(rs.getBytes("sha256_hash"));
        userLoginCertificate.setSha384Hash(rs.getBytes("sha384_hash"));
        userLoginCertificate.setExpires(rs.getTimestamp("expires"));
        userLoginCertificate.setEnabled(rs.getBoolean("enabled"));
        userLoginCertificate.setStatus(Status.valueOf(rs.getString("status")));
        userLoginCertificate.setComment(rs.getString("comment"));
        return userLoginCertificate;
    }
    
}
