/**
 * QoS Enforcement module inside the Service Manager
 * Part of the mF2C Project: http://www.mf2c-project.eu/
 * <p>
 * This code is licensed under an Apache 2.0 license. Please, refer to the LICENSE.TXT file for more information
 *
 * @author Francisco Carpio - TUBS
 */
package sm.enforcement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import sm.CimiInterface;
import sm.elements.Agreement;
import sm.elements.Service;
import sm.elements.ServiceInstance;
import sm.elements.ServiceOperationReport;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static sm.Parameters.*;

public class QosEnforcer {

   private static final Logger log = LoggerFactory.getLogger(QosEnforcer.class);

   public QosEnforcer() {
      log.info("Starting QosEnforcer...");
      ExecutorService executorService1 = Executors.newSingleThreadExecutor();
      EventSubscriberRunnable eventSubscriberRunnable1 = new EventSubscriberRunnable(emUrl + SERVICE_OPERATION_REPORTS_STREAM_CREATE);
      executorService1.submit(eventSubscriberRunnable1);
      ExecutorService executorService2 = Executors.newSingleThreadExecutor();
      EventSubscriberRunnable eventSubscriberRunnable2 = new EventSubscriberRunnable(emUrl + SERVICE_OPERATION_REPORTS_STREAM_UPDATE);
      executorService2.submit(eventSubscriberRunnable2);
   }

   static boolean checkServiceOperationReport(ServiceOperationReport serviceOperationReport) {
      if (serviceOperationReport != null) {
         log.info("Checking service operation report: " + serviceOperationReport.getId());
         ServiceInstance serviceInstance = CimiInterface.getServiceInstance(serviceOperationReport.getRequestingApplicationId().getHref());
         if (serviceInstance == null) {
            log.error("No service-instance found, ignoring QoS enforcement for service-operation-report " + serviceOperationReport.getId());
            return false;
         }
         Service service = CimiInterface.getService(serviceInstance.getService());
         if (service == null) {
            log.error("No service found, ignoring QoS enforcement for service-operation-report " + serviceOperationReport.getId());
            return false;
         }
         if (service.getNumAgents() >= MAX_AGENTS_ENFORCEMENT) {
            log.info("Service " + serviceInstance.getId() + " has already maximum number of agents [" + MAX_AGENTS_ENFORCEMENT + "]");
            return false;
         }
         Agreement agreement = CimiInterface.getAgreement(serviceInstance.getAgreement());
         if (agreement == null) {
            log.error("No agreement found, ignoring QoS enforcement for service-operation-report " + serviceOperationReport.getId());
            return false;
         }
         int expectedDuration;
         Integer agreementValue = null;
         try {
            Instant startTime = Instant.parse(serviceOperationReport.getStartTime());
            Instant expectedEndTime = Instant.parse(serviceOperationReport.getExpectedEndTime());
            expectedDuration = (int) Duration.between(startTime, expectedEndTime).toSeconds();
         } catch (Exception e) {
            log.error("Error with timestamps in " + serviceOperationReport.getId() + ": " + e.getMessage());
            return false;
         }
         try {
            for (Agreement.Details.Guarantee guarantee : agreement.getDetails().getGuarantees()) {
               if (guarantee.getName().equals(serviceOperationReport.getOperationName())) {
                  agreementValue = Integer.parseInt(guarantee.getConstraint().replaceAll("[^\\d.]", ""));
                  break;
               }
            }
         } catch (Exception e) {
            log.error("Error with constraint value in " + agreement.getId() + ": " + e.getMessage());
            return false;
         }
         if (agreementValue == null) {
            log.error("No guarantees [" + serviceOperationReport.getOperationName() + "] found in " + agreement.getId());
            return false;
         }
         log.info(serviceOperationReport.getId() + " has expected duration [" + expectedDuration + "s], agreement value [" + agreementValue + "s]");
         int numAgents;
         if (expectedDuration > agreementValue) {
            numAgents = serviceInstance.getAgents().size() * MUL_FACTOR_ENFORCEMENT;
            AgentRequest agentRequest = new AgentRequest(numAgents, serviceInstance.getId());
            if (!addMoreAgentsToServiceInstance(agentRequest))
               return false;
         } else {
            numAgents = serviceInstance.getAgents().size() / MUL_FACTOR_ENFORCEMENT;
            if (numAgents < 1)
               numAgents = 1;
         }
         if (service.getNumAgents() != numAgents) {
            log.info("Updating num_agents to " + numAgents + " in " + service.getId());
            service.setNumAgents(numAgents);
            CimiInterface.putService(service);
         }
      }
      return true;
   }

   private static boolean addMoreAgentsToServiceInstance(AgentRequest agentRequest) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<AgentRequest> entity = new HttpEntity<>(agentRequest, headers);
      RestTemplate restTemplate = new RestTemplate();
      try {
         ResponseEntity<String> responseEntity = restTemplate.exchange(
                 lmUrl
                 , HttpMethod.POST
                 , entity
                 , String.class);
         if (responseEntity.getStatusCodeValue() == HttpStatus.OK.value()) {
            log.info("Number of agents successfully updated into LM for " + agentRequest.getData().getServiceInstanceId());
            return true;
         }
      } catch (Exception e) {
         log.error("Error updating the number of agents into LM for " + agentRequest.getData().getServiceInstanceId()
                 + " [" + e.getMessage() + "]");
      }
      return false;
   }
}
