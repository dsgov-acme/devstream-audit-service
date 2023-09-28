package io.nuvalence.platform.audit.service.config;

import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import io.nuvalence.auth.access.AuthorizationHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
@SpringBootTest(properties = {"spring.cloud.gcp.pubsub.enabled=true"})
@ActiveProfiles("test")
class PubSubConfigTest {
    @Autowired
    @Qualifier("pubSubInputChannel")
    MessageChannel inputChannel;

    @Autowired private PubSubInboundChannelAdapter pubSubInboundChannelAdapter;

    @MockBean private AuthorizationHandler authorizationHandler;

    @Test
    void messageChannelAdapter_shouldConfigureOutputChannel() {
        Assertions.assertEquals(inputChannel, pubSubInboundChannelAdapter.getOutputChannel());
    }

    @Test
    void messageChannelAdapter_ShouldSetAutoAck() {
        Assertions.assertEquals(AckMode.AUTO_ACK, pubSubInboundChannelAdapter.getAckMode());
    }
}
