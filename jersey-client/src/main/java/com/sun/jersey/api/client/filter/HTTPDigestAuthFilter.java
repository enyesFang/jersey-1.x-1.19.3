/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jersey.api.client.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Client filter adding HTTP Digest authentication headers in the request
 *
 * @author raphael.jolivet@gmail.com
 */
public final class HTTPDigestAuthFilter extends ClientFilter {

    // -------------------------------------------------------
    // Static constants
    // -------------------------------------------------------

    /**
     * Number of bytes use in the random number generated by client
     */
    static private final int CNONCE_NB_BYTES = 4;

    static private final Charset CHARACTER_SET = Charset.forName("iso-8859-1");
    /**
     * Init random generator
     */
    static private final SecureRandom randomGenerator;

    static {
        try {
            randomGenerator = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Pattern to parse key value pairs like: 'foobar="foo bar",toto=titi,...'
     */
    static private final Pattern TOKENS_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*(\"([^\"]+)\"|(\\w+))\\s*,?\\s*");

    // -------------------------------------------------------
    // Inner enums
    // -------------------------------------------------------

    /**
     * Types of "qop"
     */
    private enum QOP {
        AUTH,
        AUTH_INT
    }

    // -------------------------------------------------------
    // Attributes
    // -------------------------------------------------------

    private final String user;
    private final byte[] password;

    // State
    private class State {
        String nextNonce;
        String realm;
        String opaque;
        String algorithm;
        String domain;
        QOP qop = null;
        int counter = 1;
    }

    private final ThreadLocal<State> state = new ThreadLocal<State>() {
        @Override
        protected State initialValue() {
            return new State();
        }
    };



    // -------------------------------------------------------
    // Constructors
    // -------------------------------------------------------

    /**
     * Creates a new HTTP Digest Authentication filter using provided username
     * and password credentials.
     *
     * @param user username
     * @param password password
     */
    public HTTPDigestAuthFilter (
            final String user,
            final String password) {
        this(user, password.getBytes(CHARACTER_SET));
    }

    /**
     * Creates a new HTTP Digest Authentication filter using provided username
     * and password credentials. This constructor allows you to avoid storing
     * plain password value in a String variable.
     *
     * @param user username
     * @param password password
     */
    public HTTPDigestAuthFilter (
            final String user,
            final byte[] password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Append 'key="val",' to a buffer
     */
    static private void addKeyVal(
            StringBuffer buffer,
            String key,
            String val,
            boolean withQuotes) {

        String quote = (withQuotes) ? "\"" : "";
        buffer.append(key).append('=').append(quote).append(val).append(quote).append(',');
    }

    static private void addKeyVal(
            StringBuffer buffer,
            String key,
            String val) {
        addKeyVal(buffer, key, val, true);
    }

    /**
     * Converts array of bytes in hexadecimal format.
     *
     * @param data data to be converted to hex
     */
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Compute md5 hash of a byte array.
     *
     * @param data from which the hash will be computed.
     * @return computed hash as a hexadecimal string.
     */
    static String MD5(final byte[] text) {
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            md.update(text, 0, text.length);
            byte[] md5hash = md.digest();
            return convertToHex(md5hash);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * Compute a MD5 representation for the byte arrays joined with ':'.
     *
     * @param vals values to be joined must not be null.
     * @return joined arrays MD5 string.
     */
    private static String md5ForJoined(byte[]... vals) {
        return MD5(joined((byte)':', vals));
    }

    /**
     * Compute a MD5 representation for the Strings joined with ':'.
     *
     * @param vals values to be joined must not be null.
     * @return joined arrays MD5 string.
     */
    private static String md5ForJoined(String... vals) {
        return MD5(joined(':', vals).getBytes(CHARACTER_SET));
    }

    /**
     * Join parameters into a big array, use ':' as a separator.
     * @param vals individual arrays to be joined
     * @return joined array
     */
    private static byte[] joined(final byte separator, final byte[]... vals) {
        final ByteArrayOutputStream jointArray = new ByteArrayOutputStream();
        boolean firstItem = true;
        for (byte[] val : vals) {
            try {
                if (firstItem) {
                    firstItem = false;
                } else {
                    jointArray.write(separator);
                }
                jointArray.write(val);
            } catch (IOException ex) {
                Logger.getLogger(HTTPDigestAuthFilter.class.getName()).log(Level.SEVERE, null, ex);
                throw new WebApplicationException(ex);
            }
        }
        return jointArray.toByteArray();
    }

    /**
     * Join parameters into one big String, use ':' as a separator.
     * @param vals individual Strings to be joined
     * @return joined array
     */
    private static String joined(final char separator, final String[] vals) {
        final StringBuilder result = new StringBuilder();
        boolean firstValue = true;

        for(String s: vals) {
            if (!firstValue) {
                result.append(separator);
            } else {
                firstValue = false;
            }
            result.append(s);
        }
        return result.toString();
    }


    /**
     * Generate a random sequence of bytes and return it hexadecimal representation.
     *
     * @param nbBytes number of random bytes.
     * @return random data.
     */
    String randHexBytes(int nbBytes) {

        // Init array of bytes
        byte[] bytes = new byte[nbBytes];

        // Fill it with randomness
        randomGenerator.nextBytes(bytes);

        // Transform to Hexa
        return convertToHex(bytes);

    }

    /**
     * Parse the "authenticate" header of the server response.
     * Key/Value pairs are filled.
     * If several schemes are found, only the DIGEST one is returned.
     * If no www-authenticate field is found with the "Digest" scheme, null is returned
     *
     * @param lines All www-authenticate lines of the header.
     * @return parsed headers, null when "Digest" scheme not present.
     */
    static HashMap<String, String> parseHeaders(Collection<String> lines) {

        // Loop on lines
        for (String line : lines) {

            // Split spaces
            String[] parts = line.trim().split("\\s+", 2);

            // There should be 2 parts
            if (parts.length != 2) {
                continue;
            }

            // Check the scheme
            if (!parts[0].toLowerCase().equals("digest")) {
                continue;
            }

            // Parse the tokens
            Matcher match = TOKENS_PATTERN.matcher(parts[1]);

            // Create map
            HashMap<String, String> result = new HashMap<String, String>();

            // Find next pair
            while (match.find()) {

                // Defensive check, we should have 4 groups (key)=("(val)" | (val))
                int nbGroups = match.groupCount();
                if (nbGroups != 4) {
                    continue;
                }

                // Key without quotes
                String key = match.group(1);
                String valNoQuotes = match.group(3);
                String valQuotes = match.group(4);

                // Put pairs in maps
                result.put(
                        key,
                        (valNoQuotes == null) ? valQuotes : valNoQuotes);

            } // End of loop on pairs

            return result;

        } // End of loop on lines

        // No line with scheme "digest" found
        return null;
    }

    // -------------------------------------------------------
    // Main filter method
    // -------------------------------------------------------

    @Override
    public ClientResponse handle(final ClientRequest request) throws ClientHandlerException {

        // Remember if we sent a request a with headers
        boolean reqHadAuthHeaders = false;

        // Have we already login ? : Then add authorization info to the headers
        if (state.get().nextNonce != null) {

            // Remember we sent headers
            reqHadAuthHeaders = true;

            // Alias to string representation of qop
            String qopStr = null;
            if (state.get().qop != null) {
                qopStr = (state.get().qop == QOP.AUTH_INT) ? "auth-int" : "auth";
            }

            // Init the value of the "authorized" header
            StringBuffer buff = new StringBuffer();

            // Authorization scheme
            buff.append("Digest ");

            // Key/val pairs
            addKeyVal(buff, "username", this.user);
            addKeyVal(buff, "realm", state.get().realm);
            addKeyVal(buff, "nonce", state.get().nextNonce);
            if (state.get().opaque != null) {
                addKeyVal(buff, "opaque", state.get().opaque);
            }
            if (state.get().algorithm != null) {
                addKeyVal(buff, "algorithm", state.get().algorithm, false);
            }
            if (state.get().qop != null) {
                addKeyVal(buff, "qop", qopStr, false);
            }
            //if (this.domain != null) addKeyVal(buff, "domain", this.domain);

            // -------------------------------------------------------
            // Compute the Digest Hash
            // -------------------------------------------------------

            // HA1
            String HA1 = md5ForJoined(
                    this.user.getBytes(CHARACTER_SET),
                    state.get().realm.getBytes(CHARACTER_SET),
                    this.password);

            // Get exact requested URI
            String uri = request.getURI().getRawPath();


            // Repeat URI in header
            addKeyVal(buff, "uri", uri);

            // HA2 : Switch on qop
            String HA2;
            if (state.get().qop == QOP.AUTH_INT && (request.getEntity() != null)) {
                HA2 = md5ForJoined(
                        request.getMethod(),
                        uri,
                        request.getEntity().toString());
            } else {
                HA2 = md5ForJoined(
                        request.getMethod(),
                        uri);
            }

            // Compute response
            String response;
            if (state.get().qop == null) { // Simple response

                response = md5ForJoined(
                        HA1,
                        state.get().nextNonce,
                        HA2);

            } else { // Quality of protection is set

                // Generate client nonce (UID)
                String cnonce = randHexBytes(CNONCE_NB_BYTES);

                // Counter in hexadecimal
                String nc = String.format("%08x", state.get().counter);
                state.get().counter += 1;

                // Add them to key/value pairs
                addKeyVal(buff, "cnonce", cnonce);
                addKeyVal(buff, "nc", nc, false);

                response = md5ForJoined(
                        HA1,
                        state.get().nextNonce,
                        nc,
                        cnonce,
                        qopStr,
                        HA2);
            }

            // Append the response
            addKeyVal(buff, "response", response);

            // Remove the last coma
            buff.deleteCharAt(buff.length() - 1);
            String authLine = buff.toString();

            // Add the whole Authorization line to the header
            request.getHeaders().add(
                    HttpHeaders.AUTHORIZATION,
                    authLine);

        } // End of "we already logged in ?"

        // Forward the request to the next filter and get the result back
        ClientResponse response = getNext().handle(request);

        // The server asked for authentication ? (status 401)
        if (response.getClientResponseStatus() == Status.UNAUTHORIZED) {

            // Parse the www-authentication headers
            HashMap<String, String> map = parseHeaders(
                    response.getHeaders().get(
                            HttpHeaders.WWW_AUTHENTICATE));

            // No digest authentication request found ? => We can do nothing more
            if (map == null) {
                return response;
            }

            // Get header values
            state.get().realm = map.get("realm");
            state.get().nextNonce = map.get("nonce");
            state.get().opaque = map.get("opaque");
            state.get().algorithm = map.get("algorithm");
            state.get().domain = map.get("domain");

            // Parse Qop
            String qop = map.get("qop");
            if (qop == null) {
                state.get().qop = null;
            } else {
                if (qop.contains("auth-int")) {
                    state.get().qop = QOP.AUTH_INT;
                } else if (qop.contains("auth")) {
                    state.get().qop = QOP.AUTH;
                } else {
                    state.get().qop = null;
                }
            }

            // Parse "stale"
            String staleStr = map.get("stale");


            boolean stale = (staleStr != null) && staleStr.toLowerCase().equals("true");

            // Did we send the initial request without headers ?
            // Or the server asked to retry with new nonce ?
            if (stale || !reqHadAuthHeaders) {
                // Close previous response
                response.close();
                // Then try to resent same request with updated headers
                return this.handle(request);
            } else {
                // We already tried to log, but the authentication failed :
                // Just forward this response
                return response;
            }
        }

        // Not 401 status : no authentication issue
        return response;

    } // End of #handle()
}
