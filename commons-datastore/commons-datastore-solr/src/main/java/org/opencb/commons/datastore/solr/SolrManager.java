/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.commons.datastore.solr;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.CoreStatus;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class SolrManager {

    private String host;
    private String mode;
    private int timeout;

    private SolrClient solrClient;

    private Logger logger;

    public SolrManager(String host, String mode, int timeout) {
        this.host = host;
        this.mode = mode;
        this.timeout = timeout;

        this.solrClient = new HttpSolrClient.Builder(host).build();

        // The default implementation is HttpSolrClient and we can set up some parameters
        ((HttpSolrClient) this.solrClient).setRequestWriter(new BinaryRequestWriter());
        ((HttpSolrClient) this.solrClient).setSoTimeout(timeout);

        logger = LoggerFactory.getLogger(SolrManager.class);
    }

    public SolrManager(SolrClient solrClient, String host, String mode, int timeout) {
        this.host = host;
        this.mode = mode;
        this.timeout = timeout;

        this.solrClient = solrClient;

        logger = LoggerFactory.getLogger(SolrManager.class);
    }

    public boolean isAlive(String collection) {
        try {
            SolrPing solrPing = new SolrPing();
            SolrPingResponse response = solrPing.process(solrClient, collection);
            return ("OK").equals(response.getResponse().get("status"));
        } catch (SolrServerException | IOException | SolrException e) {
            return false;
        }
    }

    public void create(String dbName, String configSet) throws SolrException {
        if (StringUtils.isEmpty(dbName)) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Missing name when creating Solr collection");
        }

        if (StringUtils.isEmpty(configSet)) {
            throw new IllegalArgumentException("Missing Solr configset!");
        }

        if (isCloud()) {
            if (existsCollection(dbName)) {
                logger.warn("Solr cloud collection {} already exists", dbName);
            } else {
                createCollection(dbName, configSet);
            }
        } else {
            if (existsCore(dbName)) {
                logger.warn("Solr standalone core {} already exists", dbName);
            } else {
                createCore(dbName, configSet);
            }
        }
    }

    /**
     * Create a Solr core from a configuration set directory. By default, the configuration set directory is located
     * inside the folder server/solr/configsets.
     *
     * @param coreName  Core name
     * @param configSet Configuration set name
     * @throws SolrException Exception
     */
    public void createCore(String coreName, String configSet) throws SolrException {
        try {
            logger.debug("Creating core: host={}, core={}, configSet={}", host, coreName, configSet);
            CoreAdminRequest.Create request = new CoreAdminRequest.Create();
            request.setCoreName(coreName);
            request.setConfigSet(configSet);
            request.process(solrClient);
        } catch (Exception e) {
            throw new SolrException(SolrException.ErrorCode.CONFLICT, e);
        }
    }

    /**
     * Create a Solr collection from a configuration directory. The configuration has to be uploaded to the zookeeper,
     * $ ./bin/solr zk upconfig -n <config name> -d <path to the config dir> -z <host:port zookeeper>.
     * For Solr, collection name, configuration name and number of shards are mandatory in order to create a collection.
     * Number of replicas is optional.
     *
     * @param collectionName Collection name
     * @param configSet      Configuration name
     * @throws SolrException Exception
     */
    public void createCollection(String collectionName, String configSet) throws SolrException {
        logger.debug("Creating collection: host={}, collection={}, config={}, numShards={}, numReplicas={}",
                host, collectionName, configSet, 1, 1);
        try {
            CollectionAdminRequest request = CollectionAdminRequest.createCollection(collectionName, configSet, 1, 1);
            request.process(solrClient);
        } catch (Exception e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
        }
    }

    public boolean exists(String dbName) throws SolrException {
        if (StringUtils.isEmpty(dbName)) {
            throw new SolrException(SolrException.ErrorCode.CONFLICT, "Missing name when checking collection");
        }

        if (StringUtils.isNotEmpty(mode)) {
            logger.warn("Solr 'mode' is empty, setting default 'cloud'");
            mode = "cloud";
        }

        switch (mode.toLowerCase()) {
            case "core":
            case "standalone": {
                return existsCore(dbName);
            }
            case "collection":
            case "cloud": {
                return existsCollection(dbName);
            }
            default: {
                throw new IllegalArgumentException("Invalid Solr mode '" + mode + "'. Valid values are 'standalone' or 'cloud'");
            }
        }
    }

    /**
     * Check if a given core exists.
     *
     * @param coreName Core name
     * @return True or false
     */
    public boolean existsCore(String coreName) {
        try {
            CoreStatus status = CoreAdminRequest.getCoreStatus(coreName, solrClient);
            status.getInstanceDirectory();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Check if a given collection exists.
     *
     * @param collectionName Collection name
     * @return True or false
     * @throws SolrException SolrException
     */
    public boolean existsCollection(String collectionName) throws SolrException {
        try {
            List<String> collections = CollectionAdminRequest.listCollections(solrClient);
            for (String collection : collections) {
                if (collection.equals(collectionName)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new SolrException(SolrException.ErrorCode.CONFLICT, e);
        }
    }

    /**
     * Remove a given collection or core.
     *
     * @param dbName Collection name
     * @throws SolrException SolrException
     */
    public void remove(String dbName) throws SolrException {
        if (isCloud()) {
            removeCollection(dbName);
        } else {
            removeCore(dbName);
        }
    }

    /**
     * Remove a collection.
     *
     * @param collectionName Collection name
     * @throws SolrException SolrException
     */
    public void removeCollection(String collectionName) throws SolrException {
        try {
            CollectionAdminRequest request = CollectionAdminRequest.deleteCollection(collectionName);
            request.process(solrClient);
        } catch (SolrServerException | IOException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Remove a core.
     *
     * @param coreName Core name
     * @throws SolrException SolrException
     */
    public void removeCore(String coreName) throws SolrException {
        try {
            CoreAdminRequest.unloadCore(coreName, true, true, solrClient);
        } catch (SolrServerException | IOException e) {
            throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e.getMessage(), e);
        }
    }

    public void close() throws IOException {
        if (solrClient != null) {
            solrClient.close();
        }
    }

    public String getHost() {
        return host;
    }

    public SolrManager setHost(String host) {
        this.host = host;
        return this;
    }

    public String getMode() {
        return mode;
    }

    public SolrManager setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public SolrManager setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public SolrClient getSolrClient() {
        return solrClient;
    }

    public SolrManager setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
        return this;
    }

    private boolean isCloud() {
        if (StringUtils.isEmpty(mode)) {
            logger.warn("Solr 'mode' is empty, setting default 'cloud'");
            mode = "cloud";
        }
        switch (mode.toLowerCase()) {
            case "collection":
            case "cloud": {
                return true;
            }
            case "core":
            case "standalone": {
                return false;
            }
            default: {
                throw new IllegalArgumentException("Invalid Solr mode '" + mode + "'. Valid values are 'standalone' or 'cloud'");
            }
        }
    }
}
