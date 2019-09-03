/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.tag.rest.v2.repository;

import com.intel.dcsg.cpg.crypto.Sha1Digest;
import com.intel.dcsg.cpg.crypto.Sha256Digest;
import com.intel.dcsg.cpg.io.UUID;
import static com.intel.mtwilson.tag.dao.jooq.generated.Tables.MW_TAG_CERTIFICATE;
import com.intel.mtwilson.tag.dao.jdbi.CertificateDAO;
import com.intel.mtwilson.jaxrs2.server.resource.DocumentRepository;
import com.intel.mtwilson.jooq.util.JooqContainer;
import com.intel.mtwilson.repository.RepositoryCreateConflictException;
import com.intel.mtwilson.repository.RepositoryCreateException;
import com.intel.mtwilson.repository.RepositoryDeleteException;
import com.intel.mtwilson.repository.RepositoryException;
import com.intel.mtwilson.repository.RepositoryRetrieveException;
import com.intel.mtwilson.repository.RepositorySearchException;
import com.intel.mtwilson.tag.dao.TagJdbi;
import com.intel.mtwilson.tag.dao.jdbi.TagCertificateDAO;
import com.intel.mtwilson.core.common.tag.model.TagCertificate;
import com.intel.mtwilson.tag.model.TagCertificateCollection;
import com.intel.mtwilson.tag.model.TagCertificateFilterCriteria;
import com.intel.mtwilson.tag.model.TagCertificateLocator;
import java.sql.Timestamp;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectQuery;

/**
 *
 * @author ssbangal
 */
public class TagCertificateRepository implements DocumentRepository<TagCertificate, TagCertificateCollection, TagCertificateFilterCriteria, TagCertificateLocator> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TagCertificateRepository.class);
    
    @Override
    public TagCertificateCollection search(TagCertificateFilterCriteria criteria) {
        log.debug("Certificate:Search - Got request to search for the Certificates.");        
        TagCertificateCollection objCollection = new TagCertificateCollection();
        
        try(JooqContainer jc = TagJdbi.jooq()) {
            DSLContext jooq = jc.getDslContext();
            
            SelectQuery sql = jooq.select().from(MW_TAG_CERTIFICATE).getQuery();
            if (criteria.filter) {
                if( criteria.id != null ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.ID.equalIgnoreCase(criteria.id.toString())); // when uuid is stored in database as the standard UUID string format (36 chars)
                }
                if( criteria.subjectEqualTo != null  && criteria.subjectEqualTo.length() > 0 ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.SUBJECT.equalIgnoreCase(criteria.subjectEqualTo));
                }
                if( criteria.subjectContains != null  && criteria.subjectContains.length() > 0  ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.SUBJECT.lower().contains(criteria.subjectContains.toLowerCase()));
                }
                if( criteria.issuerEqualTo != null  && criteria.issuerEqualTo.length() > 0 ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.ISSUER.equalIgnoreCase(criteria.issuerEqualTo));
                }
                if( criteria.issuerContains != null  && criteria.issuerContains.length() > 0  ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.ISSUER.lower().contains(criteria.issuerContains.toLowerCase()));
                }
                if( criteria.validOn != null ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.NOTBEFORE.lessOrEqual(new Timestamp(criteria.validOn.getTime())));
                    sql.addConditions(MW_TAG_CERTIFICATE.NOTAFTER.greaterOrEqual(new Timestamp(criteria.validOn.getTime())));
                }
                if( criteria.validBefore != null ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.NOTAFTER.greaterOrEqual(new Timestamp(criteria.validBefore.getTime())));
                }
                if( criteria.validAfter != null ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.NOTBEFORE.lessOrEqual(new Timestamp(criteria.validAfter.getTime())));
                }
                if( criteria.hardwareUuid != null ) {
                    sql.addConditions(MW_TAG_CERTIFICATE.HARDWARE_UUID.equalIgnoreCase(criteria.hardwareUuid.toString()));
                }
            }
            sql.addOrderBy(MW_TAG_CERTIFICATE.SUBJECT);
            Result<Record> result = sql.fetch();
            log.debug("Got {} records", result.size());
            for(Record r : result) {
                TagCertificate certObj = new TagCertificate();
                try {
                    certObj.setId(UUID.valueOf(r.getValue(MW_TAG_CERTIFICATE.ID)));
                    certObj.setCertificate((byte[])r.getValue(MW_TAG_CERTIFICATE.CERTIFICATE));  // unlike other table queries, here we can get all the info from the certificate itself... except for the revoked flag
                    certObj.setIssuer(r.getValue(MW_TAG_CERTIFICATE.ISSUER));
                    certObj.setSubject(r.getValue(MW_TAG_CERTIFICATE.SUBJECT));
                    certObj.setNotBefore(r.getValue(MW_TAG_CERTIFICATE.NOTBEFORE));
                    certObj.setNotAfter(r.getValue(MW_TAG_CERTIFICATE.NOTAFTER));
                    certObj.setHardwareUuid(UUID.valueOf(r.getValue(MW_TAG_CERTIFICATE.HARDWARE_UUID)));
                    log.debug("Certificate:Search - Created certificate record in search result {}", certObj.getId().toString());
                    objCollection.getTagCertificates().add(certObj);
                }
                catch(Exception e) {
                    log.error("Certificate:Search - Cannot load certificate #{}", r.getValue(MW_TAG_CERTIFICATE.ID), e);
                }
            }
            sql.close();            
        } catch (Exception ex) {
            log.error("Certificate:Search - Error during certificate search.", ex);
            throw new RepositorySearchException(ex, criteria);
        } 
        log.debug("Certificate:Search - Returning back {} of results.", objCollection.getTagCertificates().size());                                
        return objCollection;
    }

    @Override
    public TagCertificate retrieve(TagCertificateLocator locator) {
        log.debug("Got request to retrieve tag certificate");
        if (locator == null || ( locator.id == null && locator.pathId == null
                && (locator.subjectEqualTo == null || locator.subjectEqualTo.isEmpty()) )) {
            log.debug("Tag certificate ID or subject must be specified");
            return null;
        }
        
        try (TagCertificateDAO dao = TagJdbi.tagCertificateDao()) {
            if (locator.pathId != null) {
                TagCertificate obj = dao.findById(locator.pathId);
                if (obj != null)
                    return obj;
            } else if (locator.id != null) {
                TagCertificate obj = dao.findById(locator.id);
                if (obj != null)
                    return obj;
            } else if (locator.subjectEqualTo != null && !locator.subjectEqualTo.isEmpty()) {
                TagCertificate obj = dao.findLatestBySubject(locator.subjectEqualTo.toLowerCase());
                if (obj != null)
                    return obj;
            }
        } catch (Exception ex) {
            log.error("Error during tag certificate retrieval", ex);
            throw new RepositoryRetrieveException(ex, locator);
        }
        return null;
    }

    @Override
    public void store(TagCertificate item) {
    }

    @Override
    public void create(TagCertificate item) {
        if (item.getId() == null) {
            item.setId(new UUID());
        }
        log.debug("Certificate:Create - Got request to create a new Certificate {}.", item.getId().toString());
        TagCertificateLocator locator = new TagCertificateLocator();
        locator.id = item.getId();
        try (TagCertificateDAO dao = TagJdbi.tagCertificateDao()) {
            TagCertificate newCert = dao.findById(item.getId());
            if (newCert == null) {
                newCert = TagCertificate.valueOf(item.getCertificate());
                dao.insert(item.getId(), newCert.getCertificate(), newCert.getSubject(), 
                        newCert.getIssuer(), newCert.getNotBefore(), newCert.getNotAfter(), item.getHardwareUuid());                
                log.debug("Certificate:Create - Created the Certificate {} successfully.", item.getId().toString());
            } else {
                log.error("Certificate:Create - Certificate {} will not be created since a duplicate Certificate already exists.", item.getId().toString());                
                throw new RepositoryCreateConflictException(locator);
            }
        } catch (RepositoryException re) {
            throw re;            
        } catch (Exception ex) {
            log.error("Certificate:Create - Error during certificate creation.", ex);
            throw new RepositoryCreateException(ex, locator);
        }        
    }

    @Override
    public void delete(TagCertificateLocator locator) {
        if (locator == null || locator.id == null) { return;}
        log.debug("Certificate:Delete - Got request to delete Certificate with id {}.", locator.id.toString());                
        try (TagCertificateDAO dao = TagJdbi.tagCertificateDao()) {
            TagCertificate obj = dao.findById(locator.id);
            if (obj != null) {
                dao.delete(locator.id);
                log.debug("Certificate:Delete - Deleted the Certificate {} successfully.", locator.id.toString());                
            }else {
                log.info("Certificate:Delete - Certificate does not exist in the system.");                
            }
        } catch (Exception ex) {
            log.error("Certificate:Delete - Error during certificate deletion.", ex);
            throw new RepositoryDeleteException(ex, locator);
        }        
    }
    
    @Override
    public void delete(TagCertificateFilterCriteria criteria) {
        log.debug("Certificate:Delete - Got request to delete certificate by search criteria.");        
        TagCertificateCollection objCollection = search(criteria);
        try { 
            for (TagCertificate obj : objCollection.getTagCertificates()) {
                TagCertificateLocator locator = new TagCertificateLocator();
                locator.id = obj.getId();
                delete(locator);
            }
        } catch(RepositoryException re) {
            throw re;
        } catch (Exception ex) {
            log.error("Certificate:Delete - Error during Certificate deletion.", ex);
            throw new RepositoryDeleteException(ex);
        }
    }
        
}
