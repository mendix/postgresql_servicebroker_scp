# Cloud foundry Service Broker to share a PostgreSQL instance within a Space

## Background 
This service broker allows you to share a PostgreSQL instance within a Space.  

The Service broker requires a PostgreSQL database bound as its master database. 

A service provisioning call will create a PostgreSQL database within the Master database. A binding call will return a database uri that can be used to connect to the database. Unbinding calls will disable the database user role and deprovisioning calls will delete all resources created.

The broker uses a PostgreSQL table for it's meta data. It does not maintain an internal database, so it has no dependencies besides PostgreSQL.

Capability with the Cloud Foundry service broker API is indicated by the project version number. For example, version 2.11.0 is based off the 2.11 version of the broker API.

[spring-boot-starter-security](https://github.com/spring-projects/spring-boot/tree/master/spring-boot-starters/spring-boot-starter-security) is used. See the documentation here for configuration: [Spring boot security](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-security)


## Deploy In Cloud Foundry

- Download the package
- Unzip
- Push the Service Broker to CF
- Config the service broker
- Register the service broker
- Check Marketplace


### 1. Download package
https://github.com/mendix/postgresql_servicebroker_scp/releases/download/1.2/release1.2.zip

### 2. Unzip

Unzip the package and open a cmd prompt in the unzipped folder.

### 3. Push the service-broker app
- Select the target space of your cloud foundry account. 
```
$ cf login -a https://api.example.com -u username@example.com
API endpoint: https://api.example.com

Password>
Authenticating...
OK

Select an org (or press enter to skip):
1. example-org
2. example-other-org

Org> 1
Targeted org example-org

Select a space (or press enter to skip):
1. development
2. staging
3. production

Space> 1
Targeted space development
```
- Push the service-broker to the space and use the `<org name>_<space name>` in the name of the service-broker. This will form the unique route for your service. 

```
cf push pg-shared-<org name>_<space name> --no-start
```

### 4. Config the service broker

- Define a `secret` and set it  as a environment variable to the service-broker. 

```
cf set-env pg-shared-<org name>_<space name> JAVA_OPTS "-Dsecurity.user.password=mysecret"
```

- Create a PostgreSQL database and Bind it to your service broker
The database you need to create becomes the master databasa. Before you are able to create this database you need to know which plans you have available in your account. 

The command to create a new instance is: 
```
cf create-service postgresql <plan> masterdb_<org name>_<space name>
```

 - Bind your database to your service broker App. 

```
cf bind-service pg-shared-<org name>_<space name> masterdb_<org name>_<space name>
```

### 4. Register the Service broker

- Restage and start the service broker App:
```
cf restage pg-shared-<org name>_<space name>
```

- Create Cloud Foundry service broker within the scope of your space:
```
cf create-service-broker postgresql-shared-<org name>_<space name> user mysecret https://pg-shared-<org name>-<space name>.<your domain> --space-scoped
```

### 5 Check the marketplace
Your service should now be visible within the marketplace of your space. 
```
cf marketplace
```
You should find below service in your marketplace. 

SERVICE:  `<org name>_<space name>_postgresql_shared`   

PLAN:  `shared_on_<org name>_<space name>*`

DESCRIPTION:  `PostgreSQL database on shared instance: <org name>_<space name>`



