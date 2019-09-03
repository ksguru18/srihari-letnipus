/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.intel.mtwilson.tag.selection.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.intel.mtwilson.tag.selection.xml.AttributeType;
import com.intel.mtwilson.tag.selection.xml.CacheModeAttribute;
import com.intel.mtwilson.tag.selection.xml.DefaultType;
import com.intel.mtwilson.tag.selection.xml.SelectionType;
import com.intel.mtwilson.tag.selection.xml.SelectionsType;
import com.intel.mtwilson.tag.selection.xml.TextAttributeType;

/**
 *
 * @author jbuhacoff
 */
public class TagSelectionModule extends Module {

    @Override
    public String getModuleName() {
        return "TagSelectionModule";
    }

    @Override
    public Version version() {
        return new Version(1,0,0,"com.intel.mtwilson.integration","mtwilson-tag-selection-json",null);
    }

    @Override
    public void setupModule(SetupContext sc) {
        sc.setMixInAnnotations(CacheModeAttribute.class, CacheModeAttributeMixIn.class);
        sc.setMixInAnnotations(SelectionsType.class, SelectionsTypeMixIn.class);
        sc.setMixInAnnotations(DefaultType.class, DefaultTypeMixIn.class);
        sc.setMixInAnnotations(SelectionType.class, SelectionTypeMixIn.class);
        sc.setMixInAnnotations(AttributeType.class, AttributeTypeMixIn.class);
        sc.setMixInAnnotations(TextAttributeType.class, TextAttributeTypeMixIn.class);
    }
    
}
