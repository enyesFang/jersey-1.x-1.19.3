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

package com.sun.jersey.wadl.generators.json;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;

import com.sun.jersey.server.wadl.ApplicationDescription;
import com.sun.jersey.server.wadl.ApplicationDescription.ExternalGrammar;
import com.sun.jersey.server.wadl.WadlGeneratorImpl;
import com.sun.jersey.server.wadl.generators.AbstractWadlGeneratorGrammarGenerator;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.Representation;
import javax.ws.rs.ext.ContextResolver;
import javax.xml.bind.JAXBContext;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

/**
 * This is an implementation of the grammar generator but for JSON elements
 * @author gdavison
 */
public class WadlGeneratorJSONGrammarGenerator 
    extends AbstractWadlGeneratorGrammarGenerator<URI> {

    public static final String JSON_NAMESPACE = "http://wadl.dev.java.net/2009/02/json-schema";
    // Use of the describedby is consistent with the JSON-Schema proposed spec and means
    // when rendered as JSON the WADL can easily tell the difference between this and the
    // xml references
    public static final QName JSON_ELEMENT_QNAME = new QName(JSON_NAMESPACE,"describedby", "json");
    
    private static final Logger LOGGER = Logger.getLogger( WadlGeneratorJSONGrammarGenerator.class.getName() );
    
    
    private Map<Class, String> classNameMap = new HashMap<Class, String>();
    
    
    public WadlGeneratorJSONGrammarGenerator() {
        super(new WadlGeneratorImpl(), URI.class);
    }
    
    // ================ filter actions =======================
    
    
    @Override
    public boolean acceptMediaType(MediaType type) {
        if (type.equals(MediaType.APPLICATION_JSON_TYPE)
            || type.getSubtype().endsWith("+json") ) {
            return true;
        }
        else {
            return false;
        }
    }

    // ================ methods for post build actions =======================
    
    
    @Override
    protected Resolver buildModelAndSchemas(Map<String, ExternalGrammar> extraFiles) {

        class P
        {
            public P(Class clazz, MediaType mt)
            {
                this.clazz = clazz;
                this.mt = mt;
            }
            
            Class clazz;
            MediaType mt;
            
            public String toString() {
                if (mt!=null) {
                    return clazz + " " + mt;
                }
                else {
                    return clazz.toString();
                }
            }
        }
        
        // Lets get all candidate classes so we can create the JAX-B context
        // include any @XmlSeeAlso references.

        Set<P> classSet = new HashSet<P>();
        
        // We don't know the media type for the set also classes, so
        // we are just going to have to guess
        //
        for ( Class next : _seeAlso ) {
            classSet.add(new P(next, null));
        }

        // Process input inot a list
        //
        for ( Pair pair : _hasTypeWantsName ) {
            HasType hasType = pair.hasType; 
            Class clazz = hasType.getPrimaryClass();
 
            // Is this class itself interesting?

            classSet.add( new P(clazz, hasType.getMediaType() ));
    
            // Make sure we do something about the parameters
            // TODO make this actually do something useful
           
            if ( SPECIAL_GENERIC_TYPES.contains (clazz) ) {
 
                Type type = hasType.getType();
                if ( type instanceof ParameterizedType )
                {
                    Type parameterType = ((ParameterizedType)type).getActualTypeArguments()[0];
                    if (parameterType instanceof Class)
                    {
                        classSet.add( new P((Class) parameterType, hasType.getMediaType() ));
                    }
                }
            }
        }

        // Get a list of resolved classes
        final Set<Class> resolvedClasses = new HashSet<Class>();
        
        
        try {

            // Some usefull instances
            final List<StreamResult> results = new ArrayList<StreamResult>();
            
            // Lets see if we are doing straigh Jackson POJO mapping, ant or
            // maven tasks don't have access to this yet so in future we will
            // need to find a way to wire this in
            
            final boolean isPOJOMapping = _fap!=null ?
                    _fap.getFeature(JSONConfiguration.FEATURE_POJO_MAPPING) : false;

            // For each entry in the classSet generate a JSON entry if possible
            //
            
            nextPair : for (Iterator<P> it = classSet.iterator(); it.hasNext();) {
                P next = it.next();

                try {

                    boolean generateJsonSchema = false;
                    if (isPOJOMapping) {
                        // All classes get a show at JSON-Schema
                        generateJsonSchema = true;
                    }
                    else if (_providers !=null) {
                    
                        // Try to work out the mapping in this case
                        // TODO POJO mapping case, does this mean something different?
                        // in non POJO case reject non JAXBRootElement

                        JSONConfiguration.Notation notation =
                                JSONConfiguration.DEFAULT.getNotation();

                        ContextResolver<JAXBContext> contextR = _providers.getContextResolver(JAXBContext.class, 
                                next.mt!=null ? next.mt : MediaType.WILDCARD_TYPE);
                        if (contextR!=null) {
                            JAXBContext context = contextR.getContext(next.clazz);
                            if (context instanceof JSONJAXBContext) {
                                notation = ((JSONJAXBContext)context).getJSONConfiguration().getNotation();
                            }
                        }

                        // This generation only supports
                        // natural / jackson mapped JSON-Schema at the moment
                        // others might be close, but we shouldn't generate
                        // a schema in this case
                        switch (notation)
                        {
                            // This is Jackson; but may require further configuration
                            // based on the settings
                            case NATURAL :
                                generateJsonSchema = true;
                                break;
                            case MAPPED :
                            case BADGERFISH :
                            case MAPPED_JETTISON :
                            default :
                                LOGGER.log( Level.INFO, 
                                        "Cannot support mapping " + notation + " for " + next);
                                break;
                        }
                        
                    }
                    
                    // If this is a valid configuration then generate
                    //
                    if (generateJsonSchema) {
                        final ObjectMapper mapper = new ObjectMapper();
                        JsonSchema schema = mapper.generateJsonSchema(next.clazz);
                        String jsonName = derriveName(next.clazz);
                        ObjectNode schemaNode = schema.getSchemaNode();
                        schemaNode.put("name", jsonName);

                        CharArrayWriter writer = new CharArrayWriter();
                        mapper.writeTree(mapper.getJsonFactory().createJsonGenerator(writer), schemaNode);

                        StreamResult sr = new StreamResult(writer);


                        sr.setSystemId(jsonName);

                        results.add(sr);
                        
                        //
                        
                        resolvedClasses.add(next.clazz);
                    }
                } catch (JsonMappingException je) {
                    LOGGER.log( Level.SEVERE, "Failed to generate the schema for the JSON class " + next, je );
                    it.remove();
                }
                    
            }
            
            // Store the new files for later use
            //

            for (StreamResult result : results) {
                CharArrayWriter writer = ( CharArrayWriter )result.getWriter();
                byte[] contents = writer.toString().getBytes( "UTF8" );
                extraFiles.put(
                        result.getSystemId() ,
                        new ApplicationDescription.ExternalGrammar(
                                MediaType.valueOf("application/schema+json"), // I don't think there is a specific media type for XML Schema
                                contents, false));
            }
        }
        catch ( IOException e ) {
            LOGGER.log( Level.SEVERE, "Failed to generate the schema for the JSON elements due to an IO error", e );
        }

        // Create introspector

        
        return new Resolver() {
            @Override
            public <T> T resolve(Class type, MediaType mt, Class<T> resolvedType) {
                
                // We only return a QName
                if (!URI.class.equals(resolvedType)) {
                    return null;
                }
                
                
                // Filter by media type
                if (!acceptMediaType(mt)) {
                    return null;
                }


                if (resolvedClasses.contains(type)) {
                    
                    String filename = classNameMap.get(type);
                    URI uri;
                    if (_wadl==null)  {
                        // In the generator case just generate as before
                        // but us special URI, feels a bit fragile
                        uri = URI.create(
                          "application.wadl-/" + filename);
                    }
                    if (_wadl.toString().endsWith("application.wadl")) {
                        uri = URI.create(
                          "application.wadl/" + filename);
                    }
                    else {
                        uri = _root.resolve(
                                URI.create(
                                  "application.wadl/" + filename));
                    }
                    
                    return resolvedType.cast(uri);
                    
                } else {
                    return null;
                }
                
            }
        };
    
    
    }

    private String derriveName(Class next) {

        String shortName = next.getSimpleName();
        String localCamelCase =
                Character.toLowerCase(shortName.charAt(0))
                + (shortName.length()>1 ? shortName.substring(1) : "");
        String suggestedName = localCamelCase;
        int counter = 0;
        
        while (classNameMap.values().contains(suggestedName)) {
            suggestedName = localCamelCase + (++counter);
        }
        
        classNameMap.put(next, suggestedName);
        return suggestedName;
    }

    
    
    // ================ methods for creating wants name actions ===============
    
    @Override
    protected WantsName<URI> createParmWantsName(final Param param) {
        return new WantsName<URI>() {
                   public boolean isElement()
                   {
                       return false;
                   }
                   
                   public void setName(URI name) {
                       param.getOtherAttributes().
                               put(JSON_ELEMENT_QNAME, name.toString());
                   }
               };
    }

    @Override
    protected WantsName<URI> createRepresentationWantsName(final Representation rt) {
        return new WantsName<URI>() {
                   @Override
                   public boolean isElement()
                   {
                       return true;
                   }

                   @Override
                   public void setName(URI name) {
                       rt.getOtherAttributes().
                               put(JSON_ELEMENT_QNAME, name.toString());
                   }
               };
    }
    
    
}
