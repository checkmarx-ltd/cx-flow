package com.checkmarx.flow.dto.cx.xml;

import javax.xml.bind.annotation.*;


/**
 * <p>Java class for ResultType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ResultType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Path" type="{}PathType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="NodeId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="FileName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Status" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Line" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Column" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="FalsePositive" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Severity" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="AssignToUser" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="state" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Remark" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="DeepLink" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="SeverityIndex" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ResultType", propOrder = {
    "path"
})
public class ResultType {

    @XmlElement(name = "Path", required = true)
    protected PathType path;
    @XmlAttribute(name = "NodeId")
    protected String nodeId;
    @XmlAttribute(name = "FileName")
    protected String fileName;
    @XmlAttribute(name = "Status")
    protected String status;
    @XmlAttribute(name = "Line")
    protected String line;
    @XmlAttribute(name = "Column")
    protected String column;
    @XmlAttribute(name = "FalsePositive")
    protected String falsePositive;
    @XmlAttribute(name = "Severity")
    protected String severity;
    @XmlAttribute(name = "AssignToUser")
    protected String assignToUser;
    @XmlAttribute(name = "state")
    protected String state;
    @XmlAttribute(name = "Remark")
    protected String remark;
    @XmlAttribute(name = "DeepLink")
    protected String deepLink;
    @XmlAttribute(name = "SeverityIndex")
    protected String severityIndex;

    /**
     * Gets the value of the path property.
     *
     * @return
     *     possible object is
     *     {@link PathType }
     *
     */
    public PathType getPath() {
        return path;
    }

    /**
     * Sets the value of the path property.
     *
     * @param value
     *     allowed object is
     *     {@link PathType }
     *
     */
    public void setPath(PathType value) {
        this.path = value;
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
     * Gets the value of the status property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setStatus(String value) {
        this.status = value;
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
     * Gets the value of the falsePositive property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getFalsePositive() {
        return falsePositive;
    }

    /**
     * Sets the value of the falsePositive property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setFalsePositive(String value) {
        this.falsePositive = value;
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
     * Gets the value of the assignToUser property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getAssignToUser() {
        return assignToUser;
    }

    /**
     * Sets the value of the assignToUser property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setAssignToUser(String value) {
        this.assignToUser = value;
    }

    /**
     * Gets the value of the state property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setState(String value) {
        this.state = value;
    }

    /**
     * Gets the value of the remark property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getRemark() {
        return remark;
    }

    /**
     * Sets the value of the remark property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setRemark(String value) {
        this.remark = value;
    }

    /**
     * Gets the value of the deepLink property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDeepLink() {
        return deepLink;
    }

    /**
     * Sets the value of the deepLink property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDeepLink(String value) {
        this.deepLink = value;
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

}
