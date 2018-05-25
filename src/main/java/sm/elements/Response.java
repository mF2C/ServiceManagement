/**
 * Response class.
 * Part of the mF2C Project: http://www.mf2c-project.eu/
 * <p>
 * This code is licensed under an Apache 2.0 license. Please, refer to the LICENSE.TXT file for more information
 *
 * @author Francisco Carpio - TUBS
 */
package sm.elements;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    private String id;
    private String message;
    private String resourceURI;
    private int status;

    // from CIMI
    private List<Service> services;
    private List<SlaViolation> slaViolations;

    // to others
    private Service service;
    @JsonProperty("service-instance")
    private ServiceInstance serviceInstance;

    public Response() {
        services = new ArrayList<>();
    }

    public Response(String id, String resourceURI) {
        this.id = id;
        this.resourceURI = resourceURI;
    }

    public void setOk(){
        setMessage(HttpStatus.OK.getReasonPhrase());
        setStatus(HttpStatus.OK.value());
    }

    public void setCreated(){
        setMessage(HttpStatus.CREATED.getReasonPhrase());
        setStatus(HttpStatus.CREATED.value());
    }

    public void setBadRequest(){
        setMessage(HttpStatus.BAD_REQUEST.getReasonPhrase());
        setStatus(HttpStatus.BAD_REQUEST.value());
    }

    public void setNotFound(){
        setMessage(HttpStatus.NOT_FOUND.getReasonPhrase());
        setStatus(HttpStatus.NOT_FOUND.value());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResourceURI() {
        return resourceURI;
    }

    public void setResourceURI(String resourceURI) {
        this.resourceURI = resourceURI;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public List<SlaViolation> getSlaViolations() {
        return slaViolations;
    }

    public void setSlaViolations(List<SlaViolation> slaViolations) {
        this.slaViolations = slaViolations;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }
}
