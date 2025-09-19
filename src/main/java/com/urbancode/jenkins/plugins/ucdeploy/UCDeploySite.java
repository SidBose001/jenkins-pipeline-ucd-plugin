/**
 * (c) Copyright IBM Corporation 2017.
 * This is licensed under the following license.
 * The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
 * U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package com.urbancode.jenkins.plugins.ucdeploy;

import com.urbancode.ud.client.UDRestClient;
import hudson.AbortException;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class is used to configure individual sites which are
 * stored globally in the GlobalConfig object
 *
 */
@SuppressWarnings("deprecation") // Triggered by DefaultHttpClient
public class UCDeploySite implements Serializable {
    public static final Logger log = LoggerFactory.getLogger(UCDeploySite.class);

    private static final long serialVersionUID = -8723534991244260459L;

    private String profileName;

    private String url;

    private String user;

    private Secret password;

    private boolean trustAllCerts;

    public boolean skipProps;

    private boolean alwaysCreateNewClient;


    public static DefaultHttpClient client;

    /**
     * Instantiates a new UrbanDeploy site.
     *
     */
    public UCDeploySite() {
    }

    /**
     * Necessary constructor to allow jenkins to treate the password as an encrypted value
     *
     * @param profileName
     * @param url the url of the UrbanDeploy instance
     * @param user
     * @param password
     * @param trustAllCerts
     */
    public UCDeploySite(
            String profileName,
            String url,
            String user,
            Secret password,
            boolean trustAllCerts,
            boolean skipProps,
            boolean alwaysCreateNewClient)
    {
        this.profileName = profileName;
        this.url = url;
        this.user = user;
        this.password = password;
        this.trustAllCerts = trustAllCerts;
        this.skipProps = skipProps;
        this.alwaysCreateNewClient = alwaysCreateNewClient;
        client = UDRestClient.createHttpClient(user, password.getPlainText(), trustAllCerts);
    }

    /**
     * Constructor used to bind json to matching parameter names in global.jelly
     *
     * @param profileName
     * @param url
     * @param user
     * @param password
     * @param trustAllCerts
     */
    @DataBoundConstructor
    public UCDeploySite(
            String profileName,
            String url,
            String user,
            String password,
            boolean trustAllCerts,
            boolean skipProps,
            boolean alwaysCreateNewClient)
    {
        this(profileName, url, user, Secret.fromString(password), trustAllCerts, skipProps, alwaysCreateNewClient);
    }

    public DefaultHttpClient getClient() {
        final long _start = System.nanoTime(); 
        listener.getLogger().println("[UrbanCode Deploy] getClient() start | trustAllCerts=" + trustAllCerts + ", alwaysCreateNewClient=" + alwaysCreateNewClient); 
        try {
            listener.getLogger().println("[UCD] getClient() entry: currentClientId=" + (client == null ? "<null>" : System.identityHashCode(client)));
            if (client == null || alwaysCreateNewClient == true) {
                listener.getLogger().println("[UrbanCode Deploy] getClient(): creating new HTTP client (clientWasNull=" + (client == null) + ")"); 
                client = UDRestClient.createHttpClient(user, password.getPlainText(), trustAllCerts); 
                listener.getLogger().println("[UCD] ctor: created HttpClient instance id=" + System.identityHashCode(client) + ", class=" + client.getClass().getName());
                listener.getLogger().println("[UrbanCode Deploy] getClient(): new HTTP client created successfully"); 
            } else {
                listener.getLogger().println("[UCD] getClient(): REUSE HttpClient id=" + System.identityHashCode(client));
                listener.getLogger().println("[UrbanCode Deploy] getClient(): reusing cached HTTP client"); 
            }
            return client; 
        } catch (Exception e) {
            listener.error("[UrbanCode Deploy] getClient(): exception while creating/retrieving client: " + e.getMessage(), e);
            throw e;
        } finally {
            long _ms = (System.nanoTime() - _start) / 1_000_000L; 
            listener.getLogger().println("[UrbanCode Deploy] getClient() end | durationMs=" + _ms + ", cachedClientPresent=" + (client != null)); 
        }
    }

    public DefaultHttpClient getTempClient(String tempUser, Secret tempPassword) {
        final long _start = System.nanoTime(); // timing start [web:271]
        listener.getLogger().println("[UrbanCode Deploy] getTempClient() start | trustAllCerts=" + trustAllCerts + ", tempUserPrefix=" + (tempUser == null ? "<null>" : tempUser.substring(0, Math.min(3, tempUser.length())) + "***")); // context with redacted user [web:271]
        try {
            DefaultHttpClient tmp = UDRestClient.createHttpClient(tempUser, tempPassword.getPlainText(), trustAllCerts); 
            listener.getLogger().println("[UCD] getTempClient(): TEMP HttpClient id=" + System.identityHashCode(tmp));
            listener.getLogger().println("[UrbanCode Deploy] getTempClient(): temporary HTTP client created successfully");
            return tmp; // unchanged return [web:271]
        } catch (Exception e) {
            listener.error("[UrbanCode Deploy] getTempClient(): exception while creating temporary client for user prefix '" +
                    (tempUser == null ? "<null>" : tempUser.substring(0, Math.min(3, tempUser.length())) + "***") +
                    "': " + e.getMessage(), e); 
            throw e; 
        } finally {
            long _ms = (System.nanoTime() - _start) / 1_000_000L; 
            listener.getLogger().println("[UrbanCode Deploy] getTempClient() end | durationMs=" + _ms); 
        }
    }


    /**
     * Gets the display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        if (StringUtils.isEmpty(profileName)) {
            return url;
        } else {
            return profileName;
        }
    }

    /**
     * Gets the profile name.
     *
     * @return the profile name
     */
    public String getProfileName() {
        return profileName;
    }

    /**
     * Sets the profile name.
     *
     * @param profileName
     *          the new profile name
     */
    @DataBoundSetter
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url
     *          the new url
     */
    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
        if (this.url != null) {
            this.url = this.url.replaceAll("\\\\", "/");
        }
        while (this.url != null && this.url.endsWith("/")) {
            this.url = this.url.substring(0, this.url.length() - 2);
        }
    }

    public URI getUri() throws AbortException {
        URI udSiteUri;

        try {
            udSiteUri = new URI(url);
        }
        catch (URISyntaxException ex) {
            throw new AbortException("URL " + url + " is malformed: " + ex.getMessage());
        }

        return udSiteUri;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the username.
     *
     * @param username
     *          the new username
     */
    @DataBoundSetter
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public Secret getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password
     *          the new password
     */
    @DataBoundSetter
    public void setPassword(Secret password) {
        this.password = password;
    }

    /**
     * Gets trustAllCerts
     *
     * @return if all certificates are trusted
     */
    public boolean isTrustAllCerts() {
        return trustAllCerts;
    }

    /**
     * Sets trustAllCerts to trust all ssl certificates or not
     *
     * @param trustAllCerts
     */
    @DataBoundSetter
    public void setTrustAllCerts(boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
    }

    /**
     * Gets skipProps
     *
     * @return skipProps
     */
    public boolean isSkipProps() {
        return skipProps;
    }

    /**
     * Sets skipProps
     *
     * @param skipProps
     */
    @DataBoundSetter
    public void setSkipProps(boolean skipProps) {
        this.skipProps = skipProps;
    }

    public boolean isAlwaysCreateNewClient() {
        return alwaysCreateNewClient;
    }

    @DataBoundSetter
    public void setAlwaysCreateNewClient(boolean alwaysCreateNewClient) {
        this.alwaysCreateNewClient = alwaysCreateNewClient;
    }

    /**
     * Test whether the client can connect to the UCD site
     *
     * @throws Exception
     */
    public void verifyConnection() throws Exception {
        URI uri = UriBuilder.fromPath(url).path("rest").path("state").build();
        executeJSONGet(uri);
    }

    public void executeJSONGet(URI uri) throws Exception {
        listener.getLogger().println("[UrbanCode Deploy] uri: " + uri.toString());
        HttpClient client = getClient();
        HttpGet method = new HttpGet(uri.toString());
        try {
            HttpResponse response = client.execute(method);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == 401) {
                throw new Exception("Error connecting to IBM UrbanCode Deploy: Invalid user and/or password");
            }
            else if (responseCode != 200) {
                throw new Exception("Error connecting to IBM UrbanCode Deploy: " + responseCode + "using URI: " + uri.toString());
            }
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                listener.getLogger().println("[UrbanCode Deploy] response: " + EntityUtils.toString(entity));
            }
        }
        finally {
            method.releaseConnection();
        }
    }
}
