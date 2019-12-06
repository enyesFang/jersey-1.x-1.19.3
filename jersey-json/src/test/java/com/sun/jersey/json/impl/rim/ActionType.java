/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-793 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.07.03 at 05:59:50 PM CEST 
//


package com.sun.jersey.json.impl.rim;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Represents an action on a set of affected RegistryObjects within an AuditableEvent.
 * 
 * <p>Java class for ActionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActionType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:ebxml-regrep:xsd:rim:4.0}ExtensibleObjectType">
 *       &lt;sequence>
 *         &lt;element name="AffectedObjects" type="{urn:oasis:names:tc:ebxml-regrep:xsd:rim:4.0}RegistryObjectListType" minOccurs="0"/>
 *         &lt;element name="AffectedObjectRefs" type="{urn:oasis:names:tc:ebxml-regrep:xsd:rim:4.0}ObjectRefListType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="eventType" use="required" type="{urn:oasis:names:tc:ebxml-regrep:xsd:rim:4.0}objectReferenceType" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActionType", propOrder = {
    "affectedObjects",
    "affectedObjectRefs"
})
public class ActionType
    extends ExtensibleObjectType
{

    @XmlElement(name = "AffectedObjects")
    protected RegistryObjectListType affectedObjects;
    @XmlElement(name = "AffectedObjectRefs")
    protected ObjectRefListType affectedObjectRefs;
    @XmlAttribute(required = true)
    protected String eventType;

    /**
     * Gets the value of the affectedObjects property.
     * 
     * @return
     *     possible object is
     *     {@link RegistryObjectListType }
     *     
     */
    public RegistryObjectListType getAffectedObjects() {
        return affectedObjects;
    }

    /**
     * Sets the value of the affectedObjects property.
     * 
     * @param value
     *     allowed object is
     *     {@link RegistryObjectListType }
     *     
     */
    public void setAffectedObjects(RegistryObjectListType value) {
        this.affectedObjects = value;
    }

    /**
     * Gets the value of the affectedObjectRefs property.
     * 
     * @return
     *     possible object is
     *     {@link ObjectRefListType }
     *     
     */
    public ObjectRefListType getAffectedObjectRefs() {
        return affectedObjectRefs;
    }

    /**
     * Sets the value of the affectedObjectRefs property.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjectRefListType }
     *     
     */
    public void setAffectedObjectRefs(ObjectRefListType value) {
        this.affectedObjectRefs = value;
    }

    /**
     * Gets the value of the eventType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Sets the value of the eventType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEventType(String value) {
        this.eventType = value;
    }

}
