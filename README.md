# SERVICE MANAGER

Responsible of categorizing services into the system and improving QoS before and during the execution of a service

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![CircleCI](https://circleci.com/gh/mF2C/service-manager.svg?style=svg)](https://circleci.com/gh/mF2C/service-manager)

This development is part of the European Project mF2C.

## Usage

The Service Manager provides a graphical user interface in `https://localhost/sm/index.html` to register new services into the system and to launch service instances.

#### Service registration

To register a new service, there are a set of required parameters that must be specified:

- Name: descriptive name of the service
- Executable name: specific name of the executable (e.g. hello-world)
- Type of executable: the type of application [compss, docker, docker-compose, docker-swarm, kubernetes]
- Agent type: the type of agent where the application must run [cloud, normal, micro]
- SLA template: a SLA template has to be selected for the service

And a set of optional parameters:

- Description: short description of the service
- Ports: required ports to run the service
- Number of agents: specific number of agents to run the service, if not specified, the service is launched in all available agents
- CPU architecture: type of architecture for the service to run [x86-64, ARM]
- Operating system: type of OS for the service to run [linux, mac, windows]
- Required resources: set of required resources (i.e. sensors) for the service to run 
- Optional resources: set of optional resources (i.e. actuators) for the service to use

#### Service catalog

Once the service is registered, it will appear in the service catalog among with other registered services. The service can be launched in the system using the launch button or be deleted.

### API

- Endpoint `http://localhost:46200`
- GET `/api` -> returns the list of all services
- GET `/api/<service_id>` -> returns the specified service
- POST `/api`, DATA `service` -> submit new service
- GET `/api/{service_instance_id}` -> check QoS and returns the specified service instance
- Service definition example:

       {
            "name": "compss-hello-world",
            "description": "hello world example",
            "exec": "mf2c/compss-test:it2",
            "exec_type": "compss",
            "exec_ports": [8080, 8081],
            "sla_templates": [template1],
            "agent_type": "normal",
            "num_agents": 1,
            "cpu_arch": "x86-64",
            "os": "linux",
            "req_resource": ["sensor_1"],
            "opt_resource": ["sensor_2"]
       }


## CHANGELOG

### 1.10.3 (11.12.19)

 - Fixed issues with the GUI when registering services
 
### 1.10.2 (06.12.19)

 - Added initial values for cpu, memory, disk, network when a service is registered
 
### 1.10.1 (15.11.19)

 - Added the SLA template id when launching a service through the GUI
 
### 1.10.0 (13.11.19)

 - Updated QoS enforcement to consider different guarantees for every service operation
 - Updated SM-LM interface for the GUI

### 1.9.3 (24.10.19)

#### Changed

 - Now QoS enforcement takes into account the number of agents used from service-instance instead from service.
 
### 1.9.2 (21.10.19)

#### Changed

 - Updates logging in the QoS Enforcement.
 - Fixes a bug in QoS enforcement when retrieving a service from a service-instance.
 - Fixes a bug in QoS with SLA constraint parameters
 
### 1.9.1 (08.10.19)

#### Changed

 - Fixes a bug when updating a `qos-model` into cimi.

### 1.9.0 (07.10.19)

#### Changed

 - QoS Provider updated to work without `agreement` and with `device_id` instead of `url` in the `agent` resource.

### 1.8.4 (03.10.19)

#### Changed

 - Now if a service-instance has no valid agreement and the qos-model cannot be submitted, the QoS Provider returns 404.

### 1.8.3 (16.09.19)

#### Added

 - wget to docker image for health check
 
#### Changed

 - Fixes long strings formatting in the GUI
 
 
### 1.8.2 (16.08.19)

#### Changed

 - New library for the SSE client
 
 
### 1.8.1 (15.08.19)

#### Changed

 - Checks if cimi is up before trying to connect to the event manager
 - Improves the logs


### 1.8.0 (14.05.19)

#### Added

 - Now QoS enforcement supports subscription to the Event Manager
 - Added a new call to Lifecycle when the expected execution time of a service is longer than the one specified in the agreement

#### Changed

 - updated service definition
 - shortened api endpoints





