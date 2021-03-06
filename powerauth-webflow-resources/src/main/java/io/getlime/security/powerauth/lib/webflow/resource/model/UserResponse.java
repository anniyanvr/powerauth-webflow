/*
 * Copyright 2017 Wultra s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getlime.security.powerauth.lib.webflow.resource.model;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class representing the user profile.
 *
 * @author Petr Dvorak, petr@wultra.com
 */
public class UserResponse {

    /**
     * Information about the user.
     */
    public static class User {

        private String id;
        private String givenName;
        private String familyName;
        private final Map<String, Object> extras = new LinkedHashMap<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getGivenName() {
            return givenName;
        }

        public void setGivenName(String givenName) {
            this.givenName = givenName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public Map<String, Object> getExtras() {
            return extras;
        }
    }

    /**
     * Information about the connection.
     */
    public static class Connection {

        private String language;
        private boolean sca;
        private String organizationId;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public boolean isSca() {
            return sca;
        }

        public void setSca(boolean sca) {
            this.sca = sca;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }
    }

    /**
     * Information about the service.
     */
    public static class Service {
        private String applicationName;
        private String applicationDisplayName;
        private String applicationEnvironment;
        private Date timestamp;

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public String getApplicationDisplayName() {
            return applicationDisplayName;
        }

        public void setApplicationDisplayName(String applicationDisplayName) {
            this.applicationDisplayName = applicationDisplayName;
        }

        public String getApplicationEnvironment() {
            return applicationEnvironment;
        }

        public void setApplicationEnvironment(String applicationEnvironment) {
            this.applicationEnvironment = applicationEnvironment;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
    }

    private User user;
    private Connection connection;
    private Service service;

    public UserResponse() {
        super();
        this.user = new User();
        this.connection = new Connection();
        this.service = new Service();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
