package io.nuvalence.platform.audit.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.platform.audit.service.error.RoleConfigurationException;
import io.nuvalence.platform.audit.service.usermanagement.UserManagementClient;
import io.nuvalence.platform.audit.service.usermanagement.models.ApplicationRoles;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * ApplicationRoleConfig class.
 */
@Component
@Slf4j
public class ApplicationRoleConfig {
    private static final String CONFIG = "roles.json";
    private final UserManagementClient client;

    public ApplicationRoleConfig(UserManagementClient client) {
        this.client = client;
    }

    /**
     * Publishes roles to User Management from JSON file.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void publishRoles() {
        try {
            final Resource rolesResource = new ClassPathResource(CONFIG);

            if (!rolesResource.exists()) {
                throw new RoleConfigurationException("Role configuration file does not exist.");
            }

            try (final InputStream fileStream = rolesResource.getInputStream()) {
                ApplicationRoles roles =
                        new ObjectMapper().readValue(fileStream, ApplicationRoles.class);

                this.client.publishRoles(roles);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
