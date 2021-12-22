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

 ## License
        
 Copyright 2020 The Samply Development Community
        
 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
        
 http://www.apache.org/licenses/LICENSE-2.0
        
 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
