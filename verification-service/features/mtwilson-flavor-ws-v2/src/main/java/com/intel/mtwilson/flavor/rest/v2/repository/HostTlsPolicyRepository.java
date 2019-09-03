/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.flavor.rest.v2.repository;

import com.intel.mtwilson.My;
import com.intel.mtwilson.jaxrs2.server.resource.DocumentRepository;
import com.intel.mtwilson.repository.RepositoryCreateConflictException;
import com.intel.mtwilson.repository.RepositoryCreateException;
import com.intel.mtwilson.repository.RepositoryDeleteException;
import com.intel.mtwilson.repository.RepositoryException;
import com.intel.mtwilson.repository.RepositoryInvalidInputException;
import com.intel.mtwilson.repository.RepositoryRetrieveException;
import com.intel.mtwilson.repository.RepositorySearchException;
import com.intel.mtwilson.repository.RepositoryStoreException;
import com.intel.mtwilson.tls.policy.model.HostTlsPolicy;
import com.intel.mtwilson.tls.policy.model.HostTlsPolicyCollection;
import com.intel.mtwilson.tls.policy.model.HostTlsPolicyFilterCriteria;
import com.intel.mtwilson.tls.policy.model.HostTlsPolicyLocator;
import com.intel.mtwilson.tls.policy.TlsPolicyDescriptor;
import com.intel.mtwilson.tls.policy.jdbi.TlsPolicyDAO;
import com.intel.mtwilson.tls.policy.jdbi.TlsPolicyJdbiFactory;
import com.intel.mtwilson.tls.policy.jdbi.TlsPolicyRecord;
import com.intel.mtwilson.tls.policy.codec.impl.JsonTlsPolicyReader;
import com.intel.mtwilson.tls.policy.codec.impl.JsonTlsPolicyWriter;
import com.intel.mtwilson.tls.policy.exception.TlsPolicyAllowedException;
import com.intel.mtwilson.tls.policy.filter.HostTlsPolicyFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author ssbangal
 */
public class HostTlsPolicyRepository implements DocumentRepository<HostTlsPolicy, HostTlsPolicyCollection, HostTlsPolicyFilterCriteria, HostTlsPolicyLocator> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HostTlsPolicyRepository.class);

    @Override
    public HostTlsPolicyCollection search(HostTlsPolicyFilterCriteria criteria) {
        HostTlsPolicyCollection objCollection = new HostTlsPolicyCollection();
        log.debug("HostTlsPolicy:Search - Got request to search for HostTlsPolicy.");

        // always populate the meta element in the result set with the current server default or global policies so client knows what it will get if it doesn't specify any policy or that a global policy is in effect so specifying a policy would not have an effect other than to store it for when the global policy is removed
        objCollection.getMeta().put("allow", My.configuration().getTlsPolicyAllow());
        objCollection.getMeta().put("default", My.configuration().getDefaultTlsPolicyId());
        objCollection.getMeta().put("global", My.configuration().getGlobalTlsPolicyId());

        try (TlsPolicyDAO dao = TlsPolicyJdbiFactory.tlsPolicyDAO()) {
            List<TlsPolicyRecord> result = new ArrayList<>();
            if (criteria.filter) {
                // id search
                if (criteria.id != null) {
                    String[] ids = criteria.id.split("\\s*,\\s*");
                    log.debug("HostTlsPolicyRepository search ids: {}", (Object[]) ids);
                    HashSet<String> set = new HashSet<>(Arrays.asList(ids));
                    List<TlsPolicyRecord> tlsPolicyRecords = dao.findTlsPolicyByIds(set);
                    if (tlsPolicyRecords != null) {
                        result.addAll(tlsPolicyRecords);
                    }
                } else if (criteria.hostId != null) {
                    TlsPolicyRecord tlsPolicyRecord = dao.findPrivateTlsPolicyByHostId(criteria.hostId);
                    if (tlsPolicyRecord != null) {
                        result.add(tlsPolicyRecord);
                    }
                } else if (criteria.privateEqualTo != null) {
                    List<TlsPolicyRecord> tlsPolicyRecords = dao.findTlsPolicyByPrivateEqualTo(criteria.privateEqualTo.booleanValue());
                    if (tlsPolicyRecords != null) {
                        result.addAll(tlsPolicyRecords);
                    }
                } else if (criteria.nameEqualTo != null) {
                    TlsPolicyRecord tlsPolicyRecord = dao.findTlsPolicyByNameEqualTo(criteria.nameEqualTo);
                    if (tlsPolicyRecord != null) {
                        result.add(tlsPolicyRecord);
                    }
                } else if (criteria.nameContains != null) {
                    List<TlsPolicyRecord> tlsPolicyRecords = dao.findTlsPolicyByNameContains(criteria.nameContains);
                    if (tlsPolicyRecords != null) {
                        result.addAll(tlsPolicyRecords);
                    }
                } else if (criteria.commentEqualTo != null) {
                    TlsPolicyRecord tlsPolicyRecord = dao.findTlsPolicyByCommentEqualTo(criteria.commentEqualTo);
                    if (tlsPolicyRecord != null) {
                        result.add(tlsPolicyRecord);
                    }
                } else if (criteria.commentContains != null) {
                    List<TlsPolicyRecord> tlsPolicyRecords = dao.findTlsPolicyByCommentContains(criteria.commentContains);
                    if (tlsPolicyRecords != null) {
                        result.addAll(tlsPolicyRecords);
                    }
                }
            } else {
                // no filter, get all records
                List<TlsPolicyRecord> tlsPolicyRecords = dao.findAllTlsPolicy();
                if (tlsPolicyRecords != null) {
                    result.addAll(tlsPolicyRecords);
                }
            }
            for (TlsPolicyRecord record : result) {
                HostTlsPolicy policy = convert(record);
                objCollection.getTlsPolicies().add(policy);
            }

        } catch (IOException ex) {
            log.error("HostTlsPolicy:Search - Error during HostTlsPolicy search.", ex);
            throw new RepositorySearchException(ex, criteria);
        }
        log.debug("HostTlsPolicy:Search - Returning back {} of results.", objCollection.getTlsPolicies().size());
        return objCollection;
    }

    @Override
    public HostTlsPolicy retrieve(HostTlsPolicyLocator locator) {
        log.debug("HostTlsPolicy:Retrieve - got request to retrieve host TLS policy");
        if (locator == null || (locator.id == null && locator.pathId == null
                && (locator.name == null || locator.name.isEmpty()))) {
            log.debug("HostTlsPolicy:Retrieve - host TLS policy ID or name must be specified");
            return null;
        }

        try (TlsPolicyDAO dao = TlsPolicyJdbiFactory.tlsPolicyDAO()) {
            if (locator.pathId != null) {
                TlsPolicyRecord policyRecord = dao.findTlsPolicyById(locator.pathId);
                if (policyRecord != null) {
                    return convert(policyRecord);
                }
            } else if (locator.id != null) {
                TlsPolicyRecord policyRecord = dao.findTlsPolicyById(locator.id);
                if (policyRecord != null) {
                    return convert(policyRecord);
                }
            } else if (locator.name != null) {
                TlsPolicyRecord policyRecord = dao.findTlsPolicyByNameEqualTo(locator.name);
                if (policyRecord != null) {
                    return convert(policyRecord);
                }
            }
        } catch (IOException ex) {
            log.error("HostTlsPolicy:Retrieve - Error during HostTlsPolicy retrieval.", ex);
            throw new RepositoryRetrieveException(ex, locator);
        }
        return null;
    }

    @Override
    public void store(HostTlsPolicy item) {
        log.debug("HostTlsPolicy:Store - Got request to update HostTlsPolicy with id {}.", item.getId().toString());
        if (item == null || item.getId() == null) {
            throw new RepositoryInvalidInputException();
        }

        // check if the TLS policy identifier is allowed
        if (!HostTlsPolicyFilter.isTlsPolicyAllowed(item.getDescriptor().getPolicyType())) {
            log.error("HostTlsPolicy:Store - Specified tls policy is not allowed");
            throw new TlsPolicyAllowedException("specified tls policy is not allowed");
        }

        HostTlsPolicyLocator locator = new HostTlsPolicyLocator();
        locator.id = item.getId();
        try (TlsPolicyDAO dao = TlsPolicyJdbiFactory.tlsPolicyDAO()) {
            TlsPolicyRecord record = dao.findTlsPolicyById(locator.id);
            if (record == null) {
                log.error("HostTlsPolicy:Store - Host TLS policy does not exist");
                throw new RepositoryInvalidInputException(locator);
            }

            if (item.getName() != null && !item.getName().isEmpty()) {
                record.setName(item.getName());
            }
            if (item.isPrivate()) {
                record.setPrivate(item.isPrivate());
            }
            if (item.getComment() != null && !item.getComment().isEmpty()) {
                record.setComment(item.getComment());
            }
            if (item.getDescriptor() != null) {
                JsonTlsPolicyWriter writer = new JsonTlsPolicyWriter();
                record.setContentType("application/json"); //  ;charset=utf-8
                record.setContent(writer.write(item.getDescriptor()));
            }
            dao.updateTlsPolicy(record);
        } catch (IOException ex) {
            log.error("HostTlsPolicy:Store - Error during HostTlsPolicy update.", ex);
            throw new RepositoryStoreException(ex, locator);
        }
    }

    @Override
    public void create(HostTlsPolicy item) {
        log.debug("HostTlsPolicy:Create - Got request to create a new HostTlsPolicy.");

        if (item.getId() == null) {
            return;
        }
        HostTlsPolicyLocator locator = new HostTlsPolicyLocator();
        locator.id = item.getId();

        if (item.getName() == null || item.getName().isEmpty()) {
            throw new RepositoryCreateException("Some of the input fields are empty");
        }

        // check if the TLS policy identifier is allowed
        if (!HostTlsPolicyFilter.isTlsPolicyAllowed(item.getDescriptor().getPolicyType())) {
            log.error("HostTlsPolicy:Create - HostTlsPolicy {} is not allowed.", item.getDescriptor().getPolicyType());
            throw new TlsPolicyAllowedException("Specified tls policy is not allowed");
        }

        try (TlsPolicyDAO dao = TlsPolicyJdbiFactory.tlsPolicyDAO()) {
            if ((dao.findTlsPolicyById(item.getId()) != null) || (dao.findTlsPolicyByNameEqualTo(item.getName()) != null)) {
                log.error("HostTlsPolicy:Create - HostTlsPolicy {} will not be created since a duplicate HostTlsPolicy already exists.", item.getId().toString());
                throw new RepositoryCreateConflictException(locator);
            }

            TlsPolicyRecord record = convert(item);
            dao.insertTlsPolicy(record);
        } catch (IOException ex) {
            log.error("HostTlsPolicy:Create - Error during HostTlsPolicy creation.", ex);
            throw new RepositoryCreateException(ex, locator);
        }
    }

    @Override
    public void delete(HostTlsPolicyLocator locator) {
        if (locator == null || (locator.id == null && locator.pathId == null)) {
            return;
        }
        if (locator.id == null) {
            locator.id = locator.pathId;
        }

        log.debug("HostTlsPolicy:Delete - Got request to delete HostTlsPolicy with id {}.", locator.id);

        try (TlsPolicyDAO dao = TlsPolicyJdbiFactory.tlsPolicyDAO()) {
            dao.deleteTlsPolicyById(locator.id);
        } catch (IOException ex) {
            log.error("HostTlsPolicy:Delete - Error during HostTlsPolicy deletion.", ex);
            throw new RepositoryDeleteException(ex, locator);
        }
    }

    @Override
    public void delete(HostTlsPolicyFilterCriteria criteria) {
        log.debug("HostTlsPolicy:Delete - Got request to delete HostTlsPolicy by search criteria.");
        try {
            HostTlsPolicyCollection result = search(criteria);
            for (HostTlsPolicy hostTlsPolicy : result.getTlsPolicies()) {
                HostTlsPolicyLocator locator = new HostTlsPolicyLocator();
                locator.id = hostTlsPolicy.getId();
                delete(locator);
            }
        } catch (RepositoryException re) {
            throw re;
        } catch (Exception ex) {
            log.error("HostTlsPolicy:Delete - Error during HostTlsPolicy deletion.", ex);
            throw new RepositoryDeleteException(ex);
        }
    }

    private HostTlsPolicy convert(TlsPolicyRecord policyRecord) {
        HostTlsPolicy policy = new HostTlsPolicy();
        policy.setId(policyRecord.getId());
        policy.setName(policyRecord.getName());
        policy.setPrivate(policyRecord.isPrivate());
        policy.setComment(policyRecord.getComment());
        JsonTlsPolicyReader reader = new JsonTlsPolicyReader();
        if (reader.accept(policyRecord.getContentType()) && policyRecord.getContent() != null && policyRecord.getContent().length > 0) {
            TlsPolicyDescriptor descriptor = reader.read(policyRecord.getContent());
            policy.setDescriptor(descriptor);
        }
        return policy;
    }

    private TlsPolicyRecord convert(HostTlsPolicy policy) {
        TlsPolicyRecord record = new TlsPolicyRecord();
        record.setId(policy.getId());
        record.setName(policy.getName());
        record.setPrivate(policy.isPrivate());
        record.setComment(policy.getComment());
        JsonTlsPolicyWriter writer = new JsonTlsPolicyWriter();
        record.setContentType("application/json"); //  ;charset=utf-8
        record.setContent(writer.write(policy.getDescriptor()));
        return record;
    }
}
