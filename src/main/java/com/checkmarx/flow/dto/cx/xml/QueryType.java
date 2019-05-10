package com.checkmarx.flow.dto.cx.xml;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for QueryType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="QueryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Result" type="{}ResultType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="categories" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="cweId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="group" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Severity" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Language" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="LanguageHash" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="LanguageChangeDate" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="SeverityIndex" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="QueryPath" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="QueryVersionCode" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryType", propOrder = {
    "result"
})
public class QueryType {

    @XmlElement(name = "Result")
    protected List<ResultType> result;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "categories")
    protected String categories;
    @XmlAttribute(name = "cweId")
    protected String cweId;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "group")
    protected String group;
    @XmlAttribute(name = "Severity")
    protected String severity;
    @XmlAttribute(name = "Language")
    protected String language;
    @XmlAttribute(name = "LanguageHash")
    protected String languageHash;
    @XmlAttribute(name = "LanguageChangeDate")
    protected String languageChangeDate;
    @XmlAttribute(name = "SeverityIndex")
    protected String severityIndex;
    @XmlAttribute(name = "QueryPath")
    protected String queryPath;
    @XmlAttribute(name = "QueryVersionCode")
    protected String queryVersionCode;

    /**
     * Gets the value of the result property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the result property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getResult().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ResultType }
     *
     *
     */
    public List<ResultType> getResult() {
        if (result == null) {
            result = new ArrayList<>();
        }
        return this.result;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the categories property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCategories() {
        return categories;
    }

    /**
     * Sets the value of the categories property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCategories(String value) {
        this.categories = value;
    }

    /**
     * Gets the value of the cweId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCweId() {
        return cweId;
    }

    /**
     * Sets the value of the cweId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCweId(String value) {
        this.cweId = value;
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
     * Gets the value of the group property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the value of the group property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGroup(String value) {
        this.group = value;
    }

    /**
     * Gets the value of the severity property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the value of the severity property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSeverity(String value) {
        this.severity = value;
    }

    /**
     * Gets the value of the language property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets the value of the language property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLanguage(String value) {
        this.language = value;
    }

    /**
     * Gets the value of the languageHash property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLanguageHash() {
        return languageHash;
    }

    /**
     * Sets the value of the languageHash property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLanguageHash(String value) {
        this.languageHash = value;
    }

    /**
     * Gets the value of the languageChangeDate property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLanguageChangeDate() {
        return languageChangeDate;
    }

    /**
     * Sets the value of the languageChangeDate property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLanguageChangeDate(String value) {
        this.languageChangeDate = value;
    }

    /**
     * Gets the value of the severityIndex property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSeverityIndex() {
        return severityIndex;
    }

    /**
     * Sets the value of the severityIndex property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSeverityIndex(String value) {
        this.severityIndex = value;
    }

    /**
     * Gets the value of the queryPath property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getQueryPath() {
        return queryPath;
    }

    /**
     * Sets the value of the queryPath property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setQueryPath(String value) {
        this.queryPath = value;
    }

    /**
     * Gets the value of the queryVersionCode property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getQueryVersionCode() {
        return queryVersionCode;
    }

    /**
     * Sets the value of the queryVersionCode property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setQueryVersionCode(String value) {
        this.queryVersionCode = value;
    }

}
