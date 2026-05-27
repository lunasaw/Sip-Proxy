package io.github.lunasaw.gbproxy.gateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore;
import io.github.lunasaw.gbproxy.gateway.api.InviteContextStore.InviteContext;
import io.github.lunasaw.gbproxy.gateway.config.GatewayProperties;
import io.github.lunasaw.gbproxy.gateway.dto.ByeRequest;
import io.github.lunasaw.gbproxy.gateway.dto.InviteResponseRequest;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SipCommandController.class)
class SipCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GatewayProperties gatewayProperties;

    @MockBean
    private ServerCommandSender commandSender;

    @MockBean
    private InviteContextStore inviteContextStore;

    @MockBean(name = "gatewayForwardRestTemplate")
    private RestTemplate restTemplate;

    @Test
    void inviteResponse_local_node_but_expired_transaction_returns_410() throws Exception {
        when(gatewayProperties.getNodeId()).thenReturn("node-A");
        when(inviteContextStore.find("call-1")).thenReturn(new InviteContext("node-A", "ctx-key-1"));

        InviteResponseRequest req = new InviteResponseRequest();
        req.setCallId("call-1");
        req.setSdp("v=0\r\n");

        // SipTransactionRegistry.getContext returns null in unit test → 410 Gone
        mockMvc.perform(post("/sip/invite/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isGone());
    }

    @Test
    void inviteResponse_unknown_callId_returns_410() throws Exception {
        when(inviteContextStore.find("missing")).thenReturn(null);

        InviteResponseRequest req = new InviteResponseRequest();
        req.setCallId("missing");
        req.setSdp("v=0\r\n");

        mockMvc.perform(post("/sip/invite/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isGone());
    }

    @Test
    void inviteResponse_store_unavailable_returns_503() throws Exception {
        when(inviteContextStore.find(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "redis down"));

        InviteResponseRequest req = new InviteResponseRequest();
        req.setCallId("call-1");
        req.setSdp("v=0\r\n");

        mockMvc.perform(post("/sip/invite/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void inviteResponse_unknown_node_returns_502() throws Exception {
        when(gatewayProperties.getNodeId()).thenReturn("node-A");
        when(gatewayProperties.getNodes()).thenReturn(java.util.Map.of());
        when(inviteContextStore.find("call-1")).thenReturn(new InviteContext("node-B", "ctx-key-1"));

        InviteResponseRequest req = new InviteResponseRequest();
        req.setCallId("call-1");
        req.setSdp("v=0\r\n");

        mockMvc.perform(post("/sip/invite/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadGateway());
    }

    @Test
    void bye_delegates_to_commandSender() throws Exception {
        ByeRequest req = new ByeRequest();
        req.setCallId("call-1");

        mockMvc.perform(post("/sip/invite/bye")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(commandSender).deviceBye("call-1");
    }

    @Test
    void whoami_returns_nodeId() throws Exception {
        when(gatewayProperties.getNodeId()).thenReturn("node-A");

        mockMvc.perform(post("/sip/whoami"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value("node-A"));
    }
}
