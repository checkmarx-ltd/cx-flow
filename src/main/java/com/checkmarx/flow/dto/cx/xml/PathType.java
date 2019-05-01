package com.checkmarx.flow.dto.cx.xml;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for PathType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PathType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PathNode" type="{}PathNodeType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ResultId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="PathId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="SimilarityId" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PathType", propOrder = {
    "pathNode"
})
public class PathType {

    @XmlElement(name = "PathNode")
    protected List<PathNodeType> pathNode;
    @XmlAttribute(name = "ResultId")
    protected String resultId;
    @XmlAttribute(name = "PathId")
    protected String pathId;
    @XmlAttribute(name = "SimilarityId")
    protected String similarityId;

    /**
     * Gets the value of the pathNode property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pathNode property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPathNode().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PathNodeType }
     *
     *
     */
    public List<PathNodeType> getPathNode() {
        if (pathNode == null) {
            pathNode = new ArrayList<PathNodeType>();
        }
        return this.pathNode;
    }

    /**
     * Gets the value of the resultId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getResultId() {
        return resultId;
    }

    /**
     * Sets the value of the resultId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setResultId(String value) {
        this.resultId = value;
    }

    /**
     * Gets the value of the pathId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPathId() {
        return pathId;
    }

    /**
     * Sets the value of the pathId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPathId(String value) {
        this.pathId = value;
    }

    /**
     * Gets the value of the similarityId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSimilarityId() {
        return similarityId;
    }

    /**
     * Sets the value of the similarityId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSimilarityId(String value) {
        this.similarityId = value;
    }

}
