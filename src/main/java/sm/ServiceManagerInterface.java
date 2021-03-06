/**
 * Categorizer api.
 * Part of the mF2C Project: http://www.mf2c-project.eu/
 * <p>
 * This code is licensed under an Apache 2.0 license. Please, refer to the LICENSE.TXT file for more information
 *
 * @author Francisco Carpio - TUBS
 */
package sm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import sm.elements.*;
import sm.providing.heuristic.HeuristicAlgorithm;
import sm.providing.learning.LearningAlgorithm;
import sm.providing.learning.LearningModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static sm.Parameters.*;

@RestController
@RequestMapping(value = SM_ROOT)
public class ServiceManagerInterface {

   private static final Logger log = LoggerFactory.getLogger(ServiceManagerInterface.class);

   @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response getServices() {
      Response response = new Response(null, SM_ROOT);
      try {
         response.setServices(CimiInterface.getServices());
         response.setOk();
      } catch (Exception e) {
         response.setBadRequest();
         response.setMessage(e.getMessage());
      }
      return response;
   }

   @GetMapping(value = SERVICE + SERVICE_ID, produces = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response getService(@PathVariable String service_id) {
      String serviceId = "service/" + service_id;
      Response response = new Response(serviceId, SM_ROOT);
      Service service;
      try {
         if ((service = CimiInterface.getService(serviceId)) != null) {
            response.setService(service);
            response.setOk();
         } else {
            response.setNotFound();
         }
      } catch (Exception e) {
         response.setBadRequest();
         response.setMessage(e.getMessage());
      }
      return response;
   }

   @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response postService(@RequestBody Service service) {
      Response response = new Response(service.getName(), SM_ROOT);
      try {
         Service serviceCategorized = ServiceManager.categorizer.run(service);
         if (serviceCategorized != null) {
            response.setService(serviceCategorized);
            response.setCreated();
         } else {
            response.setService(service);
            response.setAccepted();
         }
      } catch (Exception e) {
         response.setBadRequest();
         response.setMessage(e.getMessage());
      }
      return response;
   }

   @GetMapping(value = SERVICE_INSTANCE + SERVICE_INSTANCE_ID, produces = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response checkQos(@PathVariable String service_instance_id) {
      String serviceInstanceId = "service-instance/" + service_instance_id;
      Response response = new Response(serviceInstanceId, SM_ROOT);
      try {
         // retrieve the service-instance
         ServiceInstance serviceInstance = CimiInterface.getServiceInstance(serviceInstanceId);
         if (serviceInstance == null) {
            response.setNotFound();
            response.setMessage("service-instance not found");
            return response;
         }
         // retrieve the service
         Service service = CimiInterface.getService(serviceInstance.getService());
         if (service == null) {
            response.setNotFound();
            response.setMessage("service not found");
            return response;
         }
         // retrieve all past service-instances of the service
         List<ServiceInstance> serviceInstances = CimiInterface.getServiceInstances();
         // list for all violations
         List<SlaViolation> slaViolations = new ArrayList<>();
         for (ServiceInstance serviceInstance1 : serviceInstances) {
            // check if the service instance was running on the same agents
            if (serviceInstance1.getAgents().equals(serviceInstance.getAgents())) {
               // retrieve the agreement of every service instance
               Agreement agreement = CimiInterface.getAgreement(serviceInstance1.getAgreement());
               // retrieve the violations from past services
               List<SlaViolation> slaViolations1 = CimiInterface.getSlaViolations(agreement.getId());
               if (slaViolations1 != null)
                  slaViolations.addAll(slaViolations1);
            }
         }
         float isFailure = 0;
         if (slaViolations.size() == 0)
            log.info("No SLA violations found for service : " + service.getId());
         else isFailure = 1;
         // search for qos models or create a new one in case it does not exist yet
         QosModel qosModel = ServiceManager.qosProvider.getQosModel(service.getId(), serviceInstance.getAgents(), algorithm);
         LearningModel learningModel = null;
         if (algorithm.equals(DRL)) {
            learningModel = LearningAlgorithm.getLearningModel(qosModel, serviceInstance);
            LearningAlgorithm.setIsFailure(isFailure);
         } else if (algorithm.equals(HEU)) {
            HeuristicAlgorithm.initialize(serviceInstance, ACCEPTANCE_RATIO);
         }
         serviceInstance = ServiceManager.qosProvider.checkQos(serviceInstance, qosModel, learningModel, algorithm);
         CimiInterface.putQosModel(qosModel);
         response.setServiceInstance(serviceInstance);
         response.setOk();
         log.info("QoS checked for service-instance: " + serviceInstance.getId());
      } catch (Exception e) {
         response.setBadRequest();
         response.setMessage(e.getMessage());
      }
      return response;
   }

   @PostMapping(value = GUI, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response postServiceFromGUI(@RequestBody Service service) {
      Response response = new Response(service.getName(), SM_ROOT + GUI);
      if (service.getSlaTemplates().get(0) == null) {
         response.setBadRequest();
         response.setMessage("Please, select the SLA template");
         return response;
      } else {
         return CimiInterface.postService(service);
      }
   }

   @DeleteMapping(value = GUI + SERVICE + SERVICE_ID, produces = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response deleteServiceFromGUI(@PathVariable String service_id) {
      return CimiInterface.deleteService("service/" + service_id);
   }

   @GetMapping(value = GUI + SLA_TEMPLATE, produces = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response getSlaTemplatesFromGUI() {
      return CimiInterface.getSlaTemplates();
   }

   @PostMapping(value = GUI + SERVICE_INSTANCE, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
   public @ResponseBody
   Response postServiceInstanceFromGUI(@RequestBody Map<String, Object> payload) {
      Response response = new Response();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
      RestTemplate restTemplate = new RestTemplate();
      log.info("Received request for launching service: " + payload.get("service_id").toString());
      try {
         ResponseEntity<String> responseEntity = restTemplate.exchange(
                 lmUrl + SERVICE
                 , HttpMethod.POST
                 , entity
                 , String.class);
         if (responseEntity.getStatusCodeValue() == HttpStatus.OK.value()) {
            String message = "Service instance successfully launched";
            log.info(message);
            response.setMessage(message);
            response.setStatus(responseEntity.getStatusCodeValue());
         }
      } catch (Exception e) {
         String message = "Error launching service instance: " + e.getMessage();
         log.error(message);
         response.setMessage(message);
         response.setStatus(HttpStatus.NOT_FOUND.value());
      }
      return response;
   }
}
