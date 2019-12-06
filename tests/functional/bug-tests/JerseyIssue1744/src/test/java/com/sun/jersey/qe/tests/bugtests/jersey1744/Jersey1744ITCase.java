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
package com.sun.jersey.qe.tests.bugtests.jersey1744;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
import com.sun.jersey.test.framework.spi.container.external.ExternalTestContainerFactory;

/**
 * Tests JERSEY issue 1744. Custom status reason phrase returned from the resource method was not propagated out of the
 * servlet container.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class Jersey1744ITCase extends JerseyTest {

    public Jersey1744ITCase() throws TestContainerException {
        super(new WebAppDescriptor.Builder().build());
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new ExternalTestContainerFactory();
    }

    @Test
    public void testCustomResponse428() {
        ClientResponse response = resource().path("resource/428").get(ClientResponse.class);
        assertNotNull(response);
        assertNotNull(response.getStatusInfo());
        assertEquals(428, response.getStatusInfo().getStatusCode());
        assertEquals("my-phrase", response.getStatusInfo().getReasonPhrase());
    }

    @Test
    public void testCustomResponse428WithEntity() {
        ClientResponse response = resource().path("resource/428-entity").get(ClientResponse.class);
        assertNotNull(response.getStatusInfo());
        assertEquals(428, response.getStatusInfo().getStatusCode());
        assertEquals("my-phrase", response.getStatusInfo().getReasonPhrase());
    }
}
