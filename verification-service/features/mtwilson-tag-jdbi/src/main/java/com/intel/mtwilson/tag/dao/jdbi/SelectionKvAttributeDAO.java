/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.tag.dao.jdbi;

import com.intel.dcsg.cpg.io.UUID;
import com.intel.mtwilson.jdbi.util.UUIDArgument;
import com.intel.mtwilson.tag.model.SelectionKvAttribute;
import java.io.Closeable;
import java.util.List;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterArgumentFactory;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * References:
 * http://www.jdbi.org/five_minute_intro/
 * http://jdbi.org/sql_object_api_argument_binding/
 * http://skife.org/jdbi/java/library/sql/2011/03/16/jdbi-sql-objects.html  (map result set to object)
 * http://www.cowtowncoder.com/blog/archives/2010/04/entry_391.html
 * http://jdbi.org/sql_object_api_batching/   (batching)
 * 
 * @author jbuhacoff
 */
@RegisterArgumentFactory(UUIDArgument.class)
@RegisterMapper(SelectionKvAttributeResultMapper.class)
public interface SelectionKvAttributeDAO extends Closeable{
    @SqlUpdate("create table mw_tag_selection_kvattribute (id char(36) primary key, selectionId char(36), kvAttributeId char(36))")
    void create();
    
    @SqlUpdate("insert into mw_tag_selection_kvattribute (id, selectionId, kvAttributeId) values (:id, :selectionId, :kvAttributeId)")
    void insert(@Bind("id") UUID id, @Bind("selectionId") UUID selectionId, @Bind("kvAttributeId") UUID kvAttributeId);

    @SqlQuery("select id, selectionId, kvAttributeId from mw_tag_selection_kvattribute where id=:id")
    SelectionKvAttribute findById(@Bind("id") UUID id);

    // this one returns the records but they are purely relational... you'd have to make separate queries to find the tags and tag values being referenced
    @SqlQuery("select id, selectionId, kvAttributeId from mw_tag_selection_kvattribute where selectionId=:selectionId")
    List<SelectionKvAttribute> findBySelectionId(@Bind("selectionId") UUID selectionId);

    // this one returns the records WITH associated information (so tag id, but also tag name and oid, and tag value id, but also tag value text)
    @SqlQuery("select mw_tag_selection_kvattribute.id, "
            + "mw_tag_selection_kvattribute.selectionId, "
            + "mw_tag_selection_kvattribute.kvAttributeId, "
            + "mw_tag_kvattribute.name,"
            + "mw_tag_kvattribute.value "
            + "from mw_tag_selection_kvattribute, mw_tag_kvattribute "
            + "where mw_tag_selection_kvattribute.selectionId =:selectionId and "
            + "mw_tag_kvattribute.id = mw_tag_selection_kvattribute.kvAttributeId")
    List<SelectionKvAttribute> findBySelectionIdWithValues(@Bind("selectionId") UUID selectionId);
    
    @SqlQuery("select id, selectionId, kvAttributeId from mw_tag_selection_kvattribute where kvAttributeId=:kvAttributeId")
    List<SelectionKvAttribute> findByTagId(@Bind("kvAttributeId") UUID kvAttributeId);

    @SqlUpdate("delete from mw_tag_selection_kvattribute where selectionId=:selectionId")
    void deleteAll(@Bind("selectionId") UUID selectionId);

    @SqlUpdate("delete from mw_tag_selection_kvattribute where id=:id")
    void delete(@Bind("id") UUID id); 
    
    @Override
    void close();
}
