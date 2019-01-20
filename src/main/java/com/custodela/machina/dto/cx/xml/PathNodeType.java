package com.custodela.machina.dto.cx.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for PathNodeType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PathNodeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="FileName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Line" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Column" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="NodeId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Type" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Length" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Snippet" type="{}SnippetType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PathNodeType", propOrder = {
    "fileName",
    "line",
    "column",
    "nodeId",
    "name",
    "type",
    "length",
    "snippet"
})
public class PathNodeType {

    @XmlElement(name = "FileName", required = true)
    protected String fileName;
    @XmlElement(name = "Line", required = true)
    protected String line;
    @XmlElement(name = "Column", required = true)
    protected String column;
    @XmlElement(name = "NodeId", required = true)
    protected String nodeId;
    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "Type", required = true)
    protected String type;
    @XmlElement(name = "Length", required = true)
    protected String length;
    @XmlElement(name = "Snippet", required = true)
    protected SnippetType snippet;

    /**
     * Gets the value of the fileName property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the value of the fileName property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setFileName(String value) {
        this.fileName = value;
    }

    /**
     * Gets the value of the line property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLine() {
        return line;
    }

    /**
     * Sets the value of the line property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLine(String value) {
        this.line = value;
    }

    /**
     * Gets the value of the column property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getColumn() {
        return column;
    }

    /**
     * Sets the value of the column property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setColumn(String value) {
        this.column = value;
    }

    /**
     * Gets the value of the nodeId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the value of the nodeId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setNodeId(String value) {
        this.nodeId = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the length property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLength() {
        return length;
    }

    /**
     * Sets the value of the length property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLength(String value) {
        this.length = value;
    }

    /**
     * Gets the value of the snippet property.
     *
     * @return
     *     possible object is
     *     {@link SnippetType }
     *
     */
    public SnippetType getSnippet() {
        return snippet;
    }

    /**
     * Sets the value of the snippet property.
     *
     * @param value
     *     allowed object is
     *     {@link SnippetType }
     *
     */
    public void setSnippet(SnippetType value) {
        this.snippet = value;
    }

}
