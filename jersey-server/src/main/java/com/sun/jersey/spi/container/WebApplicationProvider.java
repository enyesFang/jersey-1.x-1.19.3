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

package com.sun.jersey.spi.container;

import com.sun.jersey.api.container.ContainerException;

/**
 * Service-provider interface for creating Web application instances.
 * <p>
 * An implementation (a service-provider) identifies itself by placing a 
 * provider-configuration file (if not already present), 
 * "com.sun.research.ws.rest.spi.container.WebApplicationProvider" in the 
 * resource directory <tt>META-INF/services</tt>, and including the fully qualified
 * service-provider-class of the implementation in the file.
 * <p>
 * Only the first registered provider in the provider-configuration file will
 * be used any subsequent providers (if present) will be ignored.
 *
 * @author Paul.Sandoz@Sun.Com
 */
public interface WebApplicationProvider {

    /**
     * Instantiate a new {@link WebApplication}.
     * 
     * @return the Web application.
     * @throws ContainerException if there is an error creating the Web application.
     */
    public abstract WebApplication createWebApplication()
        throws ContainerException;    
}
