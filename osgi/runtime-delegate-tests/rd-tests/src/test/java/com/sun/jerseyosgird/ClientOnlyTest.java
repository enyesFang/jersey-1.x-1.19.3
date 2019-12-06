/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jerseyosgird;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.repositories;

import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jerseyosgird.util.Helper;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(JUnit4TestRunner.class)
public class ClientOnlyTest {

    private static final int port = Helper.getEnvVariable("JERSEY_HTTP_PORT", 8080);
    private static final String CONTEXT = "/jersey";
    private static final URI baseUri = UriBuilder.fromUri("http://localhost").port(port).path(CONTEXT).build();

    @Inject
    protected BundleContext bundleContext;


    @Test
    public void testSimpleResource() throws Exception {

        GrizzlyWebServer gws = new GrizzlyWebServer(port);
        gws.addGrizzlyAdapter(new ServletAdapter(new SimpleNonJerseyServlet()), new String[] {"/jersey"});
        gws.start();

        WebResource r = createResource().path("/simple");
        String response = r.get(String.class);

        System.out.println("RESULT = " + response);
        assertEquals("raw servlet", response);

        gws.stop();
    }



    @Configuration
    public static Option[] configuration() {

        Option[] options = options(

                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),//"INFO"),
                systemProperty("org.osgi.service.http.port").value(String.valueOf(port)),

                systemPackage("sun.misc"),

                // define maven repository
                repositories(
                        "http://repo1.maven.org/maven2",
                        "http://repository.apache.org/content/groups/snapshots-group",
                        "http://repository.ops4j.org/maven2",
                        "http://svn.apache.org/repos/asf/servicemix/m2-repo",
                        "http://repository.springsource.com/maven/bundles/release",
                        "http://repository.springsource.com/maven/bundles/external",
                        "http://maven.java.net/content/repositories/snapshots"),

                // log
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.4"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.4"),

                //mvn: url handler
                mavenBundle("org.ops4j.pax.url", "pax-url-mvn", "1.1.2"),

                // felix config admin
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.2.4"),

                // felix preference service
                mavenBundle("org.apache.felix", "org.apache.felix.prefs","1.0.2"),

                // blueprint
                mavenBundle("org.apache.geronimo.blueprint", "geronimo-blueprint", "1.0.0"),

                // bundles
                mavenBundle("org.apache.mina", "mina-core", "2.0.0-RC1"),
                mavenBundle("org.apache.sshd", "sshd-core", "0.3.0"),

                // HTTP SPEC
                mavenBundle("org.apache.geronimo.specs","geronimo-servlet_2.5_spec","1.1.2"),


                // load PAX url war
                mavenBundle("org.ops4j.pax.url","pax-url-war","1.1.2"),

                mavenBundle().groupId("javax.ws.rs").artifactId("jsr311-api").versionAsInProject(),

                mavenBundle().groupId("com.sun.jersey").artifactId("jersey-core").versionAsInProject(),
                mavenBundle().groupId("com.sun.jersey").artifactId("jersey-client").versionAsInProject(),

                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-servlet-webserver").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-http").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-framework").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-rcm").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-portunif").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-utils").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-lzma").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly").artifactId("grizzly-http-servlet").versionAsInProject(),

                // start felix framework
                felix());
        return options;
    }

    public WebResource createResource() {
        final Client c = Client.create();
        final WebResource rootResource = c.resource(baseUri);
        return rootResource;
    }
}

