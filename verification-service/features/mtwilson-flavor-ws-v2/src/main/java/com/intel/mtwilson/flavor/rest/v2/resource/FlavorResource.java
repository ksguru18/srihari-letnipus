/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.intel.mtwilson.flavor.rest.v2.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.io.UUID;
import com.intel.dcsg.cpg.validation.ValidationUtil;
import com.intel.mtwilson.core.flavor.PlatformFlavor;
import com.intel.mtwilson.core.flavor.PlatformFlavorFactory;
import com.intel.mtwilson.core.flavor.common.FlavorPart;
import com.intel.mtwilson.core.flavor.common.PlatformFlavorException;
import com.intel.mtwilson.core.flavor.model.Flavor;
import static com.intel.mtwilson.core.flavor.common.FlavorPart.*;
import com.intel.mtwilson.core.flavor.model.Meta;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorCollection;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorCreateCriteria;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorFilterCriteria;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorFlavorgroupLink;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorFlavorgroupLinkCollection;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorFlavorgroupLinkFilterCriteria;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorFlavorgroupLinkLocator;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorHostLink;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorHostLinkCollection;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorHostLinkFilterCriteria;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorHostLinkLocator;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorLocator;
import com.intel.mtwilson.flavor.rest.v2.model.Flavorgroup;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorgroupHostLink;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorgroupHostLinkCollection;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorgroupHostLinkFilterCriteria;
import com.intel.mtwilson.flavor.rest.v2.model.FlavorgroupLocator;
import com.intel.mtwilson.flavor.rest.v2.model.Host;
import com.intel.mtwilson.flavor.rest.v2.model.HostLocator;
import com.intel.mtwilson.flavor.rest.v2.repository.FlavorFlavorgroupLinkRepository;
import com.intel.mtwilson.flavor.rest.v2.repository.FlavorHostLinkRepository;
import com.intel.mtwilson.flavor.rest.v2.repository.FlavorRepository;
import com.intel.mtwilson.flavor.rest.v2.repository.FlavorgroupHostLinkRepository;
import com.intel.mtwilson.flavor.rest.v2.repository.FlavorgroupRepository;
import com.intel.mtwilson.flavor.rest.v2.repository.HostRepository;
import com.intel.mtwilson.flavor.rest.v2.utils.FlavorGroupUtils;
import com.intel.mtwilson.flavor.rest.v2.utils.FlavorUtils;
import com.intel.mtwilson.i18n.HostState;
import com.intel.mtwilson.jaxrs2.mediatype.DataMediaType;
import com.intel.mtwilson.jaxrs2.provider.JacksonObjectMapperProvider;
import com.intel.mtwilson.launcher.ws.ext.V2;
import com.intel.mtwilson.core.common.datatypes.ConnectionString;
import com.intel.mtwilson.core.common.model.HostManifest;
import com.intel.mtwilson.core.common.model.SoftwareFlavorPrefix;
import com.intel.mtwilson.core.common.tag.model.X509AttributeCertificate;
import com.intel.mtwilson.core.common.tag.model.TagCertificate;
import com.intel.mtwilson.flavor.runnable.AddFlavorgroupHostsToFlavorVerifyQueue;
import com.intel.mtwilson.tag.model.TagCertificateLocator;
import com.intel.mtwilson.tag.rest.v2.repository.TagCertificateRepository;
import com.intel.mtwilson.tls.policy.TlsPolicyDescriptor;
import java.io.IOException;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 *
 * @author srege
 */
@V2
@Path("/flavors")
public class FlavorResource {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FlavorResource.class);
    private FlavorRepository repository;
    private static final String DEPRECATED_FLAVOR_PART_BIOS = "BIOS";

    public FlavorResource() {
        repository = new FlavorRepository();
    }

    protected FlavorRepository getRepository() {
        return repository;
    }

    protected FlavorCollection createEmptyCollection() {
        return new FlavorCollection();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, DataMediaType.APPLICATION_YAML, DataMediaType.TEXT_YAML})
    @RequiresPermissions("flavors:search")
    public FlavorCollection searchFlavor(@BeanParam FlavorFilterCriteria criteria, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        ValidationUtil.validate(criteria); 
        log.debug("target: {} - {}", httpServletRequest.getRequestURI(), httpServletRequest.getQueryString());
        return repository.search(criteria);
    }

    @GET
    @Produces({MediaType.APPLICATION_XML})
    @RequiresPermissions("flavors:search")
    public FlavorCollection searchFlavorXML(@BeanParam FlavorFilterCriteria criteria, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        ValidationUtil.validate(criteria);
        log.debug("target: {} - {}", httpServletRequest.getRequestURI(), httpServletRequest.getQueryString());
        FlavorCollection flavorCollection =  repository.search(criteria);
        return FlavorGroupUtils.updatePathSeparatorForXML(flavorCollection);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, DataMediaType.APPLICATION_YAML, DataMediaType.TEXT_YAML})
    @Path("/{id}")
    @RequiresPermissions("flavors:retrieve")
    public Flavor retrieveFlavor(@BeanParam FlavorLocator locator, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        ValidationUtil.validate(locator);
        return repository.retrieve(locator);
    }

    @GET
    @Produces({MediaType.APPLICATION_XML})
    @Path("/{id}")
    @RequiresPermissions("flavors:retrieve")
    public Flavor retrieveFlavorXML(@BeanParam FlavorLocator locator, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        ValidationUtil.validate(locator);
        Flavor flavor = repository.retrieve(locator);
        if (flavor != null && flavor.getMeta().getDescription().getFlavorPart().equals("SOFTWARE")) {
            flavor = FlavorUtils.updatePathSeparatorForXML(flavor);
        }
        return flavor;
    }

    /**
     * Add an item to the collection. Input Content-Type is any of
     * application/json, application/xml, application/yaml, or text/yaml Output
     * Content-Type is any of application/json, application/xml,
     * application/yaml, or text/yaml
     *
     * The input must represent a single item NOT wrapped in a collection.
     *
     * @param item
     * @return
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, DataMediaType.APPLICATION_YAML, DataMediaType.TEXT_YAML})
    @Produces({MediaType.APPLICATION_JSON, DataMediaType.APPLICATION_YAML, DataMediaType.TEXT_YAML})
    public FlavorCollection createFlavors(FlavorCreateCriteria item) throws IOException, Exception {
        ValidationUtil.validate(item);

        if (item == null) {
            throw new WebApplicationException("Flavor create criteria must be specified", 400);
        }

        if (item.getPartialFlavorTypes() == null || item.getPartialFlavorTypes().isEmpty()) {
            return createNonHostUnique(item);
        } else {
            item.setPartialFlavorTypes(replaceBoisToPlatform(item.getPartialFlavorTypes()));
            for (String partialFlavorType : item.getPartialFlavorTypes()) {
                if (partialFlavorType.equalsIgnoreCase(HOST_UNIQUE.getValue())) {
                    return createHostUniqueFlavors(item);
                } else if (partialFlavorType.equalsIgnoreCase(ASSET_TAG.getValue())) {
                    return createAssetTagFlavors(item);
                } else if (partialFlavorType.equalsIgnoreCase(FlavorPart.SOFTWARE.getValue())) {
                    return createSoftwareFlavors(item);
                } else {
                    return createNonHostUnique(item);
                }
            }
        }
        return null;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, DataMediaType.APPLICATION_YAML, DataMediaType.TEXT_YAML})
    @Produces({MediaType.APPLICATION_XML})
    public FlavorCollection createFlavorsXML(FlavorCreateCriteria item) throws IOException, Exception {
        ValidationUtil.validate(item);
        FlavorCollection flavorCollection = createFlavors(item);
        if (flavorCollection != null) {
            flavorCollection = FlavorGroupUtils.updatePathSeparatorForXML(flavorCollection);
        }
        return flavorCollection;
    }

    // TODO: Additional support for backward compatibility
    private List<String> replaceBoisToPlatform(List<String> partialFlavorTypes) {
        List<String> filteredFlavorTypes = new ArrayList<>();
        for(String flavorType :  partialFlavorTypes) {
            if(flavorType.equalsIgnoreCase("BIOS")) {
                filteredFlavorTypes.add(FlavorPart.PLATFORM.getValue());
            } else {
                filteredFlavorTypes.add(flavorType);
            }
        }
        return filteredFlavorTypes;
    }

    @RequiresPermissions("tag_flavors:create")
    private FlavorCollection createAssetTagFlavors(FlavorCreateCriteria item) throws IOException, Exception {
        return createOne(item);
    }
    
    @RequiresPermissions("host_unique_flavors:create")
    private FlavorCollection createHostUniqueFlavors(FlavorCreateCriteria item) throws IOException, Exception {
        return createOne(item);
    }

    @RequiresPermissions("software_flavors:create")
    private FlavorCollection createSoftwareFlavors(FlavorCreateCriteria item) throws IOException, Exception {
        return createOne(item);
    }

    @RequiresPermissions("flavors:create")
    private FlavorCollection createNonHostUnique(FlavorCreateCriteria item) throws IOException, Exception{
        return createOne(item);
    }

    
    private FlavorCollection createOne(FlavorCreateCriteria item) throws IOException, Exception {
        X509AttributeCertificate attributeCertificate = null;
        Map<String, List<Flavor>> flavorPartFlavorMap = new HashMap<>();
        List<String> partialFlavorTypes = new ArrayList();
        // get flavor from host or from input
        PlatformFlavor platformFlavor = null;
        if (item.getConnectionString() != null && !item.getConnectionString().isEmpty()) {
            ConnectionString connectionString = HostRepository.generateConnectionString(item.getConnectionString());
            
            // connect to the host and retrieve the host manifest
            TlsPolicyDescriptor tlsPolicyDescriptor = new HostResource().getTlsPolicy(
                    item.getTlsPolicyId(), connectionString, true);
            HostManifest hostManifest;
            try {
                hostManifest = new HostResource().getHostManifest(
                    tlsPolicyDescriptor, connectionString, null);
            } catch (Exception e) {
                log.debug("Flavors: Exception instance when connecting to host is {}", e.toString());
                HostState hostState = new HostStatusResource().determineHostState(e);
                throw new WebApplicationException(hostState.getHostStateText(), e, 400);
            }
            
            TagCertificateRepository repo = new TagCertificateRepository();
            TagCertificateLocator tagCertificateLocator = new TagCertificateLocator();
            tagCertificateLocator.subjectEqualTo = hostManifest.getHostInfo().getHardwareUuid();
            TagCertificate tagCertificate = repo.retrieve(tagCertificateLocator);
            if (tagCertificate != null) {
                attributeCertificate = X509AttributeCertificate.valueOf(tagCertificate.getCertificate());
                log.debug("X509Attribute Certificate created {}", attributeCertificate);
            }
            
            // cast the host manifest to a platform flavor using the lib-flavor
            PlatformFlavorFactory factory = new PlatformFlavorFactory();
            platformFlavor = factory.getPlatformFlavor(hostManifest, attributeCertificate);
            log.debug("Platform flavor part names: {}", platformFlavor.getFlavorPartNames());
            
            // set user specified partial flavor types
            if (item.getPartialFlavorTypes() != null && !item.getPartialFlavorTypes().isEmpty()) {
                partialFlavorTypes.addAll(item.getPartialFlavorTypes());
            }
        } else if (item.getFlavorCollection() != null && item.getFlavorCollection().getFlavors() != null
                && !item.getFlavorCollection().getFlavors().isEmpty()) {
            for (Flavor flavor : item.getFlavorCollection().getFlavors()) {
                if (flavor != null && flavor.getMeta() != null && flavor.getMeta().getDescription() != null
                        && flavor.getMeta().getDescription().getFlavorPart() != null) {
                    // TODO: Additional support for backward compatibility
                    if(flavor.getMeta().getDescription().getFlavorPart().equalsIgnoreCase(DEPRECATED_FLAVOR_PART_BIOS)) {
                        flavor.getMeta().getDescription().setFlavorPart(FlavorPart.PLATFORM.getValue());
                    }
                    validateFlavorMetaContent(flavor.getMeta());
                    if(flavorPartFlavorMap.containsKey(flavor.getMeta().getDescription().getFlavorPart())) {
                        flavorPartFlavorMap.get(flavor.getMeta().getDescription().getFlavorPart()).add(flavor);
                    } else {
                        List<Flavor> flavorsList = new ArrayList();
                        flavorsList.add(flavor);
                        flavorPartFlavorMap.put(flavor.getMeta().getDescription().getFlavorPart(), flavorsList);
                    }
                    partialFlavorTypes.add(flavor.getMeta().getDescription().getFlavorPart());
                }
            }
            if (flavorPartFlavorMap.isEmpty()) {
                throw new WebApplicationException("Flavor collection or host connection string must be specified", 400);
            }
        } else {
            throw new WebApplicationException("Host connection string or flavor content must be specified", 400);
        }
        
        // when no flavor types are specified for automatic flavor creation, set default automatic flavor types
        // need to also validate that the flavor collection input has not set any partial flavor types
        if ((item.getFlavorgroupName() == null || item.getFlavorgroupName().isEmpty())
                && (item.getPartialFlavorTypes() == null || item.getPartialFlavorTypes().isEmpty())
                && partialFlavorTypes.isEmpty()) {
            partialFlavorTypes.addAll(FlavorPart.getValues());
        }
        
        // determine flavorgroup name
        String flavorgroupName;
        if (item.getFlavorgroupName() != null && !item.getFlavorgroupName().isEmpty()) {
            flavorgroupName = item.getFlavorgroupName();
        } else {
            flavorgroupName = Flavorgroup.AUTOMATIC_FLAVORGROUP;
        }
        
        // look for flavorgroup
        Flavorgroup flavorgroup = FlavorGroupUtils.getFlavorGroupByName(flavorgroupName);
        if(flavorgroup == null) {
            flavorgroup = FlavorGroupUtils.createFlavorGroupByName(flavorgroupName);
        }
        // if host connector retrieved platform flavor, break it into the flavor part flavor map using the flavorgroup id
        if (platformFlavor != null) {
            flavorPartFlavorMap = retrieveFlavorCollection(platformFlavor, flavorgroup.getId().toString(), partialFlavorTypes);
        }
        
        // throw error if no flavors are to be created
        if (flavorPartFlavorMap == null || flavorPartFlavorMap.isEmpty()) {
            throw new WebApplicationException("Cannot create flavors", 400);
        }
        return addFlavorToFlavorgroup(flavorPartFlavorMap, flavorgroup.getId());
    }

    private void validateFlavorMetaContent(Meta flavorMeta) {
        if(flavorMeta.getDescription().getLabel() == null || flavorMeta.getDescription().getLabel().isEmpty()) {
            throw new WebApplicationException("Flavor label missing", 400);
        }
        try {
            FlavorPart.valueOf(flavorMeta.getDescription().getFlavorPart());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException("Invalid flavor part specified", 400);
        }
    }

    // the delete method is on a specific resource id and because we don't return any content it's the same whether its simple object or json api 
    // jersey automatically returns status code 204 No Content (successful) to the client because
    // we have a void return type
    @Path("/{id}")
    @DELETE
    @RequiresPermissions("flavors:delete")
    public void deleteOne(@BeanParam FlavorLocator locator, @Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        ValidationUtil.validate(locator); 
        Flavor item = getRepository().retrieve(locator); // subclass is responsible for validating the id in whatever manner it needs to;  most will return null if !UUID.isValid(id)  but we don't do it here because a resource might want to allow using something other than uuid as the url key, for example uuid OR hostname for hosts
        if (item == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        List<UUID> hostIds = new ArrayList();
        
        // retrieve list of hosts associated with the flavor
        FlavorHostLinkFilterCriteria flavorHostLinkFilterCriteria = new FlavorHostLinkFilterCriteria();
        flavorHostLinkFilterCriteria.flavorId = UUID.valueOf(item.getMeta().getId());
        FlavorHostLinkCollection flavorHostLinkCollection
                = new FlavorHostLinkRepository().search(flavorHostLinkFilterCriteria);
        if (flavorHostLinkCollection != null && flavorHostLinkCollection.getFlavorHostLinks() != null
                && !flavorHostLinkCollection.getFlavorHostLinks().isEmpty()) {
            for (FlavorHostLink flavorHostLink : flavorHostLinkCollection.getFlavorHostLinks()) {
                hostIds.add(flavorHostLink.getHostId());
                
                // Delete the flavor host link
                FlavorHostLinkLocator flavorHostLinkLocator = new FlavorHostLinkLocator(flavorHostLink.getId());
                new FlavorHostLinkRepository().delete(flavorHostLinkLocator);
            }
        }
        
        // retrieve list of flavorgroups associated with the flavor
        FlavorFlavorgroupLinkFilterCriteria flavorFlavorgroupLinkFilterCriteria
                = new FlavorFlavorgroupLinkFilterCriteria();
        flavorFlavorgroupLinkFilterCriteria.flavorId = UUID.valueOf(item.getMeta().getId());
        FlavorFlavorgroupLinkCollection flavorFlavorgroupLinkCollection
                = new FlavorFlavorgroupLinkRepository().search(flavorFlavorgroupLinkFilterCriteria);
        
        // if there are flavorgroups associated with the flavor
        if (flavorFlavorgroupLinkCollection != null
                && flavorFlavorgroupLinkCollection.getFlavorFlavorgroupLinks() != null
                && !flavorFlavorgroupLinkCollection.getFlavorFlavorgroupLinks().isEmpty()) {
            
            // for each flavorgroup associated with the flavor
            for (FlavorFlavorgroupLink flavorFlavorgroupLink : flavorFlavorgroupLinkCollection.getFlavorFlavorgroupLinks()) {
                
                // get hosts associated with flavorgroup
                FlavorgroupHostLinkFilterCriteria flavorgroupHostLinkFilterCriteria
                        = new FlavorgroupHostLinkFilterCriteria();
                flavorgroupHostLinkFilterCriteria.flavorgroupId = flavorFlavorgroupLink.getFlavorgroupId();
                FlavorgroupHostLinkCollection flavorgroupHostLinkCollection
                = new FlavorgroupHostLinkRepository().search(flavorgroupHostLinkFilterCriteria);
                
                // add the hosts to the list of affected host IDs
                if (flavorgroupHostLinkCollection != null && flavorgroupHostLinkCollection.getFlavorgroupHostLinks() != null
                        && !flavorgroupHostLinkCollection.getFlavorgroupHostLinks().isEmpty()) {
                    for (FlavorgroupHostLink flavorgroupHostLink : flavorgroupHostLinkCollection.getFlavorgroupHostLinks()) {
                        if (!hostIds.contains(flavorgroupHostLink.getHostId()))
                            hostIds.add(flavorgroupHostLink.getHostId());
                    }
                }
                
                // Delete the link between the flavor and flavor group
                FlavorFlavorgroupLinkLocator flavorFlavorgroupLinkLocator = new FlavorFlavorgroupLinkLocator();
                flavorFlavorgroupLinkLocator.id = flavorFlavorgroupLink.getId();
                log.debug("Flavors : About to delete the flavor-flavorgroup link with ID - {}", flavorFlavorgroupLinkLocator.id.toString());
                new FlavorFlavorgroupLinkRepository().delete(flavorFlavorgroupLinkLocator);
            }
        }
        
        // finally, delete the flavor
        getRepository().delete(locator);
        
        // add the hosts to the flavor-verify queue
        for (UUID hostId : hostIds) {
            new HostResource().addHostToFlavorVerifyQueue(hostId, false);
        }
        
        // set the response code to 204
        httpServletResponse.setStatus(Status.NO_CONTENT.getStatusCode());
    }
    
    /**
     * This method associates the flavor/flavor parts specified from the PlatformFlavor instance
     * with a particular flavor group.
     *
     * @param flavorObjCollection
     * @param flavorgroupId Flavorgroup ID to which the flavor needs to be
     * associated
     * @return createdFlavor Created flavor
     */
    public FlavorCollection addFlavorToFlavorgroup(Map<String, List<Flavor>> flavorObjCollection, UUID flavorgroupId) {
        FlavorCollection returnFlavors = new FlavorCollection();
        Collection<UUID> flavorIds = new ArrayList<>();

        for (Map.Entry<String, List<Flavor>> flavorObj : flavorObjCollection.entrySet()) {
            for(Flavor flavor : flavorObj.getValue()) {
                Flavor flavorCreated = new FlavorRepository().create(flavor);
                returnFlavors.getFlavors().add(flavorCreated);      
                // If the flavor part is HOST_UNIQUE OR ASSET_TAG, then we associate it with the host_unique group name
                if (flavorObj.getKey().equalsIgnoreCase(ASSET_TAG.getValue())) {
                    addFlavorToUniqueFlavorgroup(flavorCreated, true);
                } else if (flavorObj.getKey().equalsIgnoreCase(HOST_UNIQUE.getValue())) {
                    addFlavorToUniqueFlavorgroup(flavorCreated, false);
                } else if (flavorObj.getKey().equalsIgnoreCase(SOFTWARE.getValue()) && flavor.getMeta().getDescription().getLabel().contains(SoftwareFlavorPrefix.DEFAULT_APPLICATION_FLAVOR_PREFIX.getValue())) {
                    addFlavorToIseclSoftwareFlavorgroup(flavorCreated, Flavorgroup.PLATFORM_SOFTWARE_FLAVORGROUP);
                } else if (flavorObj.getKey().equalsIgnoreCase(SOFTWARE.getValue()) && flavor.getMeta().getDescription().getLabel().contains(SoftwareFlavorPrefix.DEFAULT_WORKLOAD_FLAVOR_PREFIX.getValue())) {
                    addFlavorToIseclSoftwareFlavorgroup(flavorCreated, Flavorgroup.WORKLOAD_SOFTWARE_FLAVORGROUP);
                } else {
                    // For other flavor parts, we just store all the individual flavors first and finally do the association below.
                    // Other flavor parts include OS, PLATFORM, SOFTWARE
                    flavorIds.add(UUID.valueOf(flavorCreated.getMeta().getId()));
                }
            }
        }

        if (flavorgroupId == null || flavorIds.isEmpty()) {
            log.trace("Flavorgroup ID or flavor IDs not specified");
            return returnFlavors;
        }

        // For flavor parts other than HOST_UNIQUE & ASSET_TAG, we associate it with the specified flavor group, which can be either 
        // automatic or a custom group name spcified by the end user.
        for (UUID flavorId : flavorIds) {
            FlavorFlavorgroupLink flavorFlavorgroupLink = new FlavorFlavorgroupLink();
            flavorFlavorgroupLink.setFlavorId(flavorId);
            flavorFlavorgroupLink.setFlavorgroupId(flavorgroupId);
            new FlavorFlavorgroupLinkRepository().create(flavorFlavorgroupLink);
        }

        // Add hosts matching the flavorgroup to flavor verify queue in background
        new Thread(new AddFlavorgroupHostsToFlavorVerifyQueue(flavorgroupId, false)).start();
        return returnFlavors;
    }
    
    /**
     * Associates the flavor with the unique flavor group created during the
     * installation.
     *
     * @param flavor Complete flavor.
     * @param forceUpdate option for flavor-verify operation to force an updated
     * report to be generated from the host directly
     */
    private void addFlavorToUniqueFlavorgroup(Flavor flavor, boolean forceUpdate) {
        // get flavor ID
        UUID flavorId = UUID.valueOf(flavor.getMeta().getId());
        // find unique flavorgroup
        FlavorgroupLocator flavorgroupLocator = new FlavorgroupLocator();
        flavorgroupLocator.name = Flavorgroup.HOST_UNIQUE_FLAVORGROUP;
        Flavorgroup uniqueFlavorgroup = new FlavorgroupRepository().retrieve(flavorgroupLocator);
        if (uniqueFlavorgroup == null) {
            Flavorgroup newFlavorgroup = new Flavorgroup();
            newFlavorgroup.setName(Flavorgroup.HOST_UNIQUE_FLAVORGROUP);
            newFlavorgroup.setFlavorMatchPolicyCollection(null);
            uniqueFlavorgroup = new FlavorgroupRepository().create(newFlavorgroup);
        }
        
        // create the flavor flavorgroup link association
        FlavorFlavorgroupLink flavorFlavorgroupLink = new FlavorFlavorgroupLink();
        flavorFlavorgroupLink.setFlavorId(flavorId);
        flavorFlavorgroupLink.setFlavorgroupId(uniqueFlavorgroup.getId());
        new FlavorFlavorgroupLinkRepository().create(flavorFlavorgroupLink);
        
        // retrieve the host name from the flavor document, if it exists
        String hostName = null;
        if (flavor.getMeta() != null && flavor.getMeta().getDescription() != null
                && flavor.getMeta().getDescription().getLabel() != null
                && !flavor.getMeta().getDescription().getLabel().isEmpty()) {
            hostName = flavor.getMeta().getDescription().getLabel();
        }
        
        // retrieve the hardware uuid from the flavor document, if it exists
        UUID hardwareUuid = null;
        if (flavor.getMeta() != null && flavor.getMeta().getDescription() != null
                && flavor.getMeta().getDescription().getHardwareUuid() != null
                && !flavor.getMeta().getDescription().getHardwareUuid().isEmpty()
                && UUID.isValid(flavor.getMeta().getDescription().getHardwareUuid())) {
            hardwareUuid = UUID.valueOf(flavor.getMeta().getDescription().getHardwareUuid());
        }
        
        if ((hostName == null || hostName.isEmpty()) && hardwareUuid == null) {
            throw new IllegalArgumentException("Host name or hardware UUID must be specified in the flavor document");
        }
        
        // get the host details from the database
        HostLocator hostLocator = new HostLocator();
        if (hostName != null)
            hostLocator.name = hostName;
        if (hardwareUuid != null)
            hostLocator.hardwareUuid =  hardwareUuid;
        Host host = new HostRepository().retrieve(hostLocator);
        if (host == null) {
            log.debug("Host [{}] is not registered, no further processing will be performed", hostName);
            return;
        }
        
        // add host to flavor-verify queue
        new HostResource().addHostToFlavorVerifyQueue(host.getId(), forceUpdate);
    }

    /**
     * Associates the flavor with the default software flavor group created during the
     * installation.
     *
     * @param flavor Complete flavor.
     * report to be generated from the host directly
     */
    private void addFlavorToIseclSoftwareFlavorgroup(Flavor flavor, String flavorgroupName) {
        // get flavor ID
        UUID flavorId = UUID.valueOf(flavor.getMeta().getId());

        // find unique flavorgroup
        Flavorgroup iseclSoftwareFlavorgroup = FlavorGroupUtils.getFlavorGroupByName(flavorgroupName);
        if (iseclSoftwareFlavorgroup == null) {
            Flavorgroup newFlavorgroup = new Flavorgroup();
            newFlavorgroup.setName(flavorgroupName);
            newFlavorgroup.setFlavorMatchPolicyCollection(Flavorgroup.getIseclSoftwareFlavorMatchPolicy());
            iseclSoftwareFlavorgroup = new FlavorgroupRepository().create(newFlavorgroup);
        }

        // create the flavor flavorgroup link association
        FlavorFlavorgroupLink flavorFlavorgroupLink = new FlavorFlavorgroupLink();
        flavorFlavorgroupLink.setFlavorId(flavorId);
        flavorFlavorgroupLink.setFlavorgroupId(iseclSoftwareFlavorgroup.getId());
        new FlavorFlavorgroupLinkRepository().create(flavorFlavorgroupLink);
        
        // Add hosts matching the default flavorgroup to flavor verify queue in background and get updated host manifest
        new Thread(new AddFlavorgroupHostsToFlavorVerifyQueue(iseclSoftwareFlavorgroup.getId(), true)).start();
    }

    /**
     * Retrieves the list of all the flavors requested and returns it back to the caller
     * @param platformFlavor 
     * @param flavorgroupId
     * @param flavorParts
     * @return 
     */
    private Map<String, List<Flavor>> retrieveFlavorCollection(PlatformFlavor platformFlavor, String flavorgroupId, Collection<String> flavorParts) {
        Map<String, List<Flavor>> flavorCollection = new HashMap<>();

        if (platformFlavor == null || flavorgroupId == null) {
            throw new IllegalArgumentException("Platform flavor and flavorgroup ID must be specified");
        }
        
        if (flavorParts.isEmpty()) {
            flavorParts.add(SOFTWARE.name());
        }
        // User has specified the particular flavor part(s)
        for (String flavorPart : flavorParts) {
            try {
                ObjectMapper mapper = JacksonObjectMapperProvider.createDefaultMapper();
                for(String flavorStr : platformFlavor.getFlavorPart(flavorPart)) {
                    Flavor flavor = mapper.readValue(flavorStr, Flavor.class);
                    if(flavorCollection.containsKey(flavorPart)) {
                        flavorCollection.get(flavorPart).add(flavor);                        
                    } else {
                        List<Flavor> flavors = new ArrayList();
                        flavors.add(flavor);
                        flavorCollection.put(flavorPart, flavors);
                    }
                }
            } catch (PlatformFlavorException pe) {
                // This should be changed to warn , but Flavor library is throwing an exception when it does not find all flavor 
                // types in the content provided. Bug# ISECL-2210.
                log.debug("Could not build flavor [{}] from flavor library: {}", flavorPart, pe.getMessage());
            } catch (Exception ex) {
                throw new IllegalStateException("Error during creation of the flavors for the specified flavor parts", ex);
            }
        }
        return flavorCollection;
    }  
}
