package io.nuvalence.platform.audit.service.usermanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.platform.audit.service.error.RolesPublishingException;
import io.nuvalence.platform.audit.service.usermanagement.models.ApplicationRoles;
import io.nuvalence.platform.audit.service.utils.ServiceTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Client to interface with User Management API.
 */
@Component
@Slf4j
public class UserManagementClient {

    @Value("${userManagement.baseUrl}")
    private String baseUrl;

    private RestTemplate httpClient;

    private final ServiceTokenProvider serviceTokenProvider;

    public UserManagementClient(ServiceTokenProvider serviceTokenProvider) {
        httpClient = new RestTemplate();
        this.serviceTokenProvider = serviceTokenProvider;
    }

    /**
     * Sets the class-level httpClient (to be used for mocking).
     *
     * @param restTemplate The RestTemplate object
     */
    public void setHttpClient(RestTemplate restTemplate) {
        this.httpClient = restTemplate;
    }

    /**
     * Loads role configuration from file and uploads it to User Management.
     *
     * @param roles Role configuration to upload.
     * @throws IOException if file cannot be loaded.
     */
    public void publishRoles(ApplicationRoles roles) throws IOException {
        String rolesRequest = new ObjectMapper().writeValueAsString(roles);

        final HttpEntity<String> payload = new HttpEntity<>(rolesRequest, getHeaders());
        final String url = String.format("%s/api/v2/application/roles", baseUrl);

        ResponseEntity<?> response =
                httpClient.exchange(url, HttpMethod.PUT, payload, Object.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RolesPublishingException(
                    "Failed to upload roles: " + response.getStatusCode());
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(serviceTokenProvider.getServiceToken());
        return headers;
    }
}
