/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cloudfoundry.community.servicebroker.postgresql.config;


import org.cloudfoundry.community.servicebroker.postgresql.service.PostgreSQLDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.config.BrokerApiVersionConfig;
import org.springframework.cloud.servicebroker.model.Catalog;
import org.springframework.cloud.servicebroker.model.Plan;
import org.springframework.cloud.servicebroker.model.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ComponentScan(basePackages = "org.cloudfoundry.community.servicebroker",
        excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = BrokerApiVersionConfig.class) })
public class BrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BrokerConfiguration.class);

    private String jdbcURL;
    private String serviceName;
        
    @PostConstruct
    public void init(){
    	String vCapServices = System.getenv("VCAP_SERVICES");
    	if (vCapServices != null) {
	    	
			try {
				JSONObject vCapJson = new JSONObject(vCapServices);			
				JSONArray pgList = vCapJson.getJSONArray("postgresql");
		    	for( int i = 0; i < pgList.length(); i++)  {
		    		JSONObject pgObject = pgList.getJSONObject(i);
		    		if(pgObject != null) {
		    			JSONObject credentialsJSON = pgObject.getJSONObject("credentials");
		    			this.serviceName = pgObject.getString("name");
		    			String db = credentialsJSON.getString("dbname");
		    			String user = credentialsJSON.getString("username");
		    			String password = credentialsJSON.getString("password");
		    			String host = credentialsJSON.getString("hostname");
		    			String port = credentialsJSON.getString("port");
		    			this.jdbcURL = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?user=" + user + "&password=" + password;
		    			logger.info("JDBC URL found in VCAP settings: " + "jdbc:postgresql://" + host + ":" + port + "/" + db + "?user=" + user + "&password=....");
		    			break; 
		    		}
		    	}
			
			} catch (JSONException e) {
				logger.error(e.getMessage(), e);			
			}
    	}
    }
    
    
    @Bean
    public JdbcTemplate jdbcTemplate(){        
    	DriverManagerDataSource dataSource = new DriverManagerDataSource();        
        dataSource.setUrl(this.jdbcURL);
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PostgreSQLDatabase postgreSQLDatabase(JdbcTemplate jdbcTemplate){
        return new PostgreSQLDatabase(jdbcTemplate);
    }

    @Bean
    public Catalog catalog() throws IOException {
        ServiceDefinition serviceDefinition = new ServiceDefinition("pg_shared_" + this.serviceName, this.serviceName + "_postgresql_shared", "PostgreSQL database on shared instance: " + this.serviceName ,
                true, false, getPlans(), getTags(), getServiceDefinitionMetadata(), Arrays.asList("syslog_drain"), null);
        return new Catalog(Arrays.asList(serviceDefinition));
    }

    private static List<String> getTags() {
        return Arrays.asList("PostgreSQL", "Shared Instance");
    }

    private Map<String, Object> getServiceDefinitionMetadata() {
        Map<String, Object> sdMetadata = new HashMap<>();
        sdMetadata.put("displayName", "PostgreSQL Shared on " + this.serviceName );
        sdMetadata.put("imageUrl", "https://wiki.postgresql.org/images/3/30/PostgreSQL_logo.3colors.120x120.png");
        sdMetadata.put("longDescription", "This service allows you to re-use a PostgreSQL instance for multiple Applications");
        sdMetadata.put("providerDisplayName", "PostgreSQL DB Shared on " +  this.serviceName + " cluster");
        sdMetadata.put("documentationUrl", "https://github.com/mendix/postgresql_servicebroker_scp");
        sdMetadata.put("supportUrl", "https://github.com/mendix/postgresql_servicebroker_scp");
        return sdMetadata;
    }

    private List<Plan> getPlans() {
        Plan basic = new Plan("postgresql-shared-plan-on-" + this.serviceName, "shared_on_" + this.serviceName,
                "This plan will create a database on an existing PostgreSQL instance", getBasicPlanMetadata());
        return Arrays.asList(basic);
    }

    private static Map<String, Object> getBasicPlanMetadata() {
        Map<String, Object> planMetadata = new HashMap<>();
        planMetadata.put("bullets", getBasicPlanBullets());
        return planMetadata;
    }

    private static List<String> getBasicPlanBullets() {
        return Arrays.asList("Single PG database", "Limited storage", "Shared instance");
    }
}