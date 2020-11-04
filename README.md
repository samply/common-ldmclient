# Samply Common LDM Client

Samply Common LDM Client is an abstract library for the communication with **L**ocal **D**ata**m**anagement systems.

## Features

Defines an abstract class to be used with _samply.share.client_ or other similar applications

## Build

In order to build this project, you need to configure maven properly and use the maven profile that
fits to your project.

``` 
mvn clean package
```

## Configuration

Samply Common LDM Client does not need or support any configuration. This is done in the implementing modules.

## Maven artifact

To use the module, include the following artifact in your project.

```
<dependency>
    <groupId>de.samply</groupId>
    <artifactId>common-ldmclient</artifactId>
    <version>3.0.0</version>
</dependency>
```
