/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package test.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;
import org.junit.Test;
import com.intel.mtwilson.tag.selection.xml.*;
import org.apache.commons.io.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.dcsg.cpg.xml.JAXB;
import com.intel.mtwilson.tag.selection.SelectionBuilder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.junit.BeforeClass;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 *
 * @author jbuhacoff
 */
public class ReadXmlTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReadXmlTest.class);
    private static Validator validator;

    @BeforeClass
    public static void createValidator() throws Exception {
        try (InputStream xsd = ReadXmlTest.class.getResourceAsStream("/jaxb/mtwilson-tag-selection/mtwilson-tag-selection.xsd")) {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsd));
            validator = schema.newValidator();
        }
    }
    
    /**
     *
     * @param xmlfile a classpath resource name for example
     * "/selection-xml-examples/invalid2.xml"
     * @throws Exception
     */
    private void validateXML(String xmlfile) throws Exception {
        try (InputStream xml = getClass().getResourceAsStream(xmlfile)) {

            validator.validate(new StreamSource(xml));
        }
    }

    private void mapXmlToJson(String xmlfile) throws Exception {
        JAXB jaxb = new JAXB();
        try (InputStream in = getClass().getResourceAsStream(xmlfile)) {
            String xml = IOUtils.toString(in);
            SelectionsType selections = jaxb.read(xml, SelectionsType.class);
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY); // omit empty attributes, for example {"selection":[{"subject":[],"attribute":[],"id":"8ed9140b-e6a1-41b2-a8d4-258948633153","name":null,"notBefore":null,"notAfter":null}]}  becomes   {"selection":[{"id":"8ed9140b-e6a1-41b2-a8d4-258948633153"}]}
            log.debug("{}: {}",xmlfile, mapper.writeValueAsString(selections));
        }
    }
    
    /**
     * Sample output for selection 1:
     *     * 
{"selection":[{"attribute":[{"value":"Country=US","oid":"2.5.4.789.1"},{"value":"State=CA","oid":"2.5.4.789.1"},{"value":"State=TX","oid":"2.5.4.789.1"},{"value":"City=Folsom","oid":"2.5.4.789.1"},{"value":"City=El
     * Paso","oid":"2.5.4.789.1"}],"id":null}]}
     *
     * Sample output for selection 2:
     *     * 
{"selection":[{"attribute":[],"id":"0b52784b-4588-4c73-900d-a1bac622dde1"}]}
     *
     * Sample output for selection 3:
     *     * 
{"selection":[{"attribute":[{"value":"US","oid":"2.5.4.6"},{"value":"CA","oid":"2.5.4.8"},{"value":"TX","oid":"2.5.4.8"},{"value":"Folsom","oid":"2.5.4.7"},{"value":"El
     * Paso","oid":"2.5.4.7"}],"id":null}]}
     *
     * Sample output for Selection 4:
     *     * 
{"selection":[{"attribute":[{"value":"Country=US","oid":null},{"value":"State=CA","oid":null},{"value":"State=TX","oid":null},{"value":"City=Folsom","oid":null},{"value":"City=El
     * Paso","oid":null}],"id":null}]}
     *
     * Sample output for selection 5:
     *     * 
{"selection":[{"attribute":[{"value":"Country=US","oid":"2.5.4.789.1"},{"value":"State=CA","oid":"2.5.4.789.1"},{"value":"City=Folsom","oid":"2.5.4.789.1"}],"id":null},{"attribute":[{"value":"Country=US","oid":"2.5.4.789.1"},{"value":"State=TX","oid":"2.5.4.789.1"},{"value":"City=El
     * Paso","oid":"2.5.4.789.1"}],"id":null}]}
     *
     * Sample output for selection 6:
     *     * 
{"selection":[{"attribute":[],"id":"0b52784b-4588-4c73-900d-a1bac622dde1","name":null},{"attribute":[],"id":"bbbe1c68-4792-454c-9295-1a1234e1aa9f","name":null}]}
     *
     * Sample output for selection 7:
     *     * 
{"selection":[{"attribute":[{"value":"Country=US","oid":"2.5.4.789.1"},{"value":"State=CA","oid":"2.5.4.789.1"},{"value":"City=Folsom","oid":"2.5.4.789.1"}],"id":"8ed9140b-e6a1-41b2-a8d4-258948633153","name":"California"},{"attribute":[{"value":"Country=US","oid":"2.5.4.789.1"},{"value":"State=TX","oid":"2.5.4.789.1"},{"value":"City=El
     * Paso","oid":"2.5.4.789.1"}],"id":"24e7d7be-f337-47f7-a1af-9a4dbdaeb69d","name":null},{"attribute":[{"value":"Country=CA","oid":"2.5.4.789.1"},{"value":"Province=Quebec","oid":"2.5.4.789.1"},{"value":"City=Quebec
     * City","oid":"2.5.4.789.1"}],"id":null,"name":"Canada"}]}
     *
     *
     * @throws Exception
     */
    @Test
    public void testReadSelections() throws Exception {
        mapXmlToJson("/selection-xml-examples/selection1.xml");
        mapXmlToJson("/selection-xml-examples/selection2.xml");
        mapXmlToJson("/selection-xml-examples/selection3.xml");
        mapXmlToJson("/selection-xml-examples/selection4.xml");
        mapXmlToJson("/selection-xml-examples/selection5.xml");
        mapXmlToJson("/selection-xml-examples/selection6.xml");
        mapXmlToJson("/selection-xml-examples/selection7.xml");
        mapXmlToJson("/selection-xml-examples/selection8.xml");
        mapXmlToJson("/selection-xml-examples/selection9.xml");
        mapXmlToJson("/selection-xml-examples/selection10.xml");
        mapXmlToJson("/selection-xml-examples/selection11.xml");
        mapXmlToJson("/selection-xml-examples/selection12.xml");
        mapXmlToJson("/selection-xml-examples/selection13.xml");
    }

    @Test
    public void testValidateGoodExamples() throws Exception {
        validateXML("/selection-xml-examples/selection10.xml");

    }

    @Test(expected = Exception.class)
    public void testValidateBadExamples() throws Exception {
        validateXML("/selection-xml-examples/invalid1.xml");
        validateXML("/selection-xml-examples/invalid2.xml");
        validateXML("/selection-xml-examples/invalid4.xml");
        validateXML("/selection-xml-examples/invalid6.xml");
    }

    public class TagSelection {

        public List<tag> tagList;
        public String name;
        public String id;
    }

    public class tag {

        private String name;
        private String value;
        private String oid;

        tag(String name, String value, String oid) {
            this.name = name;
            this.value = value;
            this.oid = oid;
        }

        String getName() {
            return this.name;
        }

        String getValue() {
            return this.value;
        }

        String getOid() {
            return this.oid;
        }
    };

    public TagSelection getTagSelectionFromXml(String xml) throws ParserConfigurationException, SAXException, IOException {
        TagSelection ret = new TagSelection();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = builder.parse(is);
        ArrayList<tag> tagList = new ArrayList<tag>();
        int cnt = 0;
        NodeList nodeList = doc.getElementsByTagName("attribute");
        for (int s = 0; s < nodeList.getLength(); s++) {
            Node fstNode = nodeList.item(s);
            if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                Element fstElmnt = (Element) fstNode;
                String idValue = fstElmnt.getAttribute("oid");
                Element lstNmElmnt = (Element) nodeList.item(cnt++);
                NodeList lstNm = lstNmElmnt.getChildNodes();
                String currentAction = ((Node) lstNm.item(0)).getNodeValue();
                if (currentAction != null) {
                    tagList.add(new tag("", idValue, currentAction));
                }

            }
        }

        nodeList = doc.getElementsByTagName("selection");
        Node fstNode = nodeList.item(0);
        Element e = (Element) fstNode;
        ret.id = e.getAttribute("id");
        ret.name = e.getAttribute("name");
        ret.tagList = tagList;

        return ret;
    }

    @Test
    public void testParseXmlSelection() throws ParserConfigurationException, SAXException, IOException {
        String xml = "<selections xmlns=\"urn:mtwilson-tag-selection\">\n"
                + "<selection id=\"1\" name=\"default\">\n"
                + "<attribute oid=\"1.3.6.1.4.1.99999.1\">US</attribute>\n"
                + "<attribute oid=\"1.3.6.1.4.1.99999.2\">CA</attribute>\n"
                + "<attribute oid=\"1.3.6.1.4.1.99999.3\">Folsom</attribute>\n"
                + "<attribute oid=\"1.3.6.1.4.1.99999.3\">Santa Clara</attribute>\n"
                + "</selection>\n"
                + "</selections>";

        TagSelection selection = getTagSelectionFromXml(xml);
        System.out.println("got selection with name " + selection.name + " and id of " + selection.id);
    }
    
    private void printSelections(SelectionsType selections) {
        List<SelectionType> selectionList = selections.getSelection();
        for(SelectionType selection : selectionList) {
            log.debug("selection id {} name {} notBefore {} notAfter {}", selection.getId(), selection.getName(), selection.getNotBefore(), selection.getNotAfter());
            List<SubjectType> subjectList = selection.getSubject();
            for(SubjectType subject : subjectList) {
                log.debug("subject uuid {} name {} ip {}", subject.getUuid(), subject.getName(), subject.getIp()); // only one will appear 
            }
            List<AttributeType> attributeList = selection.getAttribute();
            for(AttributeType attribute : attributeList) {
                log.debug("attribute oid {} text {}", attribute.getOid(), attribute.getText().getValue());
            }
        }
    }
    
    @Test
    public void testParseJsonSelection() throws Exception {
        String selection1 = "{\"selection\":[{\"attribute\":[{\"text\":{\"value\":\"Country=US\"},\"oid\":\"2.5.4.789.1\"},{\"text\":{\"value\":\"State=CA\"},\"oid\":\"2.5.4.789.1\"},{\"text\":{\"value\":\"State=TX\"},\"oid\":\"2.5.4.789.1\"},{\"text\":{\"value\":\"City=Folsom\"},\"oid\":\"2.5.4.789.1\"},{\"text\":{\"value\":\"City=El Paso\"},\"oid\":\"2.5.4.789.1\"}]}]}";
        ObjectMapper mapper = new ObjectMapper();
        SelectionsType selections = mapper.readValue(selection1, SelectionsType.class);
        printSelections(selections);
    }

    @Test
    public void testSelectionBuilder() throws Exception {
        SelectionsType selections = SelectionBuilder.factory().selection().textAttributeKV("Country", "US").build();
        printSelections(selections);
    }
}
