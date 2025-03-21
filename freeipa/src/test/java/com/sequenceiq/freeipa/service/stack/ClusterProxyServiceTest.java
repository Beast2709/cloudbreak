package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.CcmV2Config;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigService;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.util.ClusterProxyServiceAvailabilityChecker;
import com.sequenceiq.freeipa.util.HealthCheckAvailabilityChecker;

@ExtendWith(MockitoExtension.class)
public class ClusterProxyServiceTest {

    private static final long STACK_ID = 100L;

    private static final String TEST_ACCOUNT_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String FREEIPA_SERVICE = "freeipa";

    @InjectMocks
    private ClusterProxyService underTest;

    @Mock
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Mock
    private ClusterProxyServiceAvailabilityChecker clusterProxyServiceAvailabilityChecker;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private PollingService serviceEndpointHealthPollingService;

    @Mock
    private HealthCheckAvailabilityChecker healthCheckAvailabilityChecker;

    @Mock
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"CCMV2", "CCMV2_JUMPGATE"}, mode = EnumSource.Mode.INCLUDE)
    public void testClusterProxyRegistrationWhenCCMV2OrJumpgate(Tunnel ccmv2Mode) {
        Stack aStack = getAStack();
        aStack.setTunnel(ccmv2Mode);
        aStack.setCcmV2AgentCrn("testAgentCrn");
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");

        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", null, "privateIpAddress",
                ServiceFamilies.GATEWAY.getDefaultPort(), "testInstanceId", true);
        ConfigRegistrationResponse configRegResposne = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResposne);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);

        underTest.registerFreeIpaForBootstrap(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();

        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertEquals(true, proxyRegisterationReq.isUseCcmV2(), ccmv2Mode.toString() + " should be enabled.");
        assertEquals(List.of(new CcmV2Config("privateIpAddress", ServiceFamilies.GATEWAY.getDefaultPort(),
                        "testAgentCrn-testInstanceId", FREEIPA_SERVICE)),
                proxyRegisterationReq.getCcmV2Configs(), ccmv2Mode.toString() + " config should match");
    }

    @ParameterizedTest
    @EnumSource(value = Tunnel.class, names = {"CCMV2", "CCMV2_JUMPGATE"}, mode = EnumSource.Mode.INCLUDE)
    public void testUpdateClusterProxyRegisterationWhenCCMV2OrJumpgate(Tunnel ccmv2Mode) {
        Stack aStack = getAStack();
        aStack.setTunnel(ccmv2Mode);
        aStack.setCcmV2AgentCrn("testAgentCrn");

        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setUsePrivateIpToTls(true);
        aStack.setSecurityConfig(securityConfig);

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("test.freeipa.domain");

        GatewayConfig primaryGateway = new GatewayConfig("primaryAddress", null, "primaryPrivateAddress",
                ServiceFamilies.GATEWAY.getDefaultPort(), "privateInstanceId", true);
        GatewayConfig gatewayConfig1 = new GatewayConfig("connectionAddress1", null, "privateIpAddress1",
                ServiceFamilies.GATEWAY.getDefaultPort(), "testInstanceId1", true);
        GatewayConfig gatewayConfig2 = new GatewayConfig("connectionAddress2", null, "privateIpAddress2",
                ServiceFamilies.GATEWAY.getDefaultPort(), "testInstanceId2", true);
        ConfigRegistrationResponse configRegResposne = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(primaryGateway);
        when(gatewayConfigService.getNotDeletedGatewayConfigs(aStack)).thenReturn(List.of(gatewayConfig1, gatewayConfig2));
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResposne);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);
        when(clusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(aStack)).thenReturn(true);
        when(serviceEndpointHealthPollingService.pollWithTimeout(any(), any(), anyLong(), anyInt(), anyInt())).thenReturn(null);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(aStack)).thenReturn(true);

        underTest.updateFreeIpaRegistrationAndWait(STACK_ID, List.of("testInstanceId1", "testInstanceId2"));

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();

        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertEquals(true, proxyRegisterationReq.isUseCcmV2(), ccmv2Mode.toString() + " should be enabled.");
        assertEquals(List.of(
                new CcmV2Config("privateIpAddress1", ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-testInstanceId1", FREEIPA_SERVICE
                ),
                new CcmV2Config("privateIpAddress2", ServiceFamilies.GATEWAY.getDefaultPort(), "testAgentCrn-testInstanceId2", FREEIPA_SERVICE
                )), proxyRegisterationReq.getCcmV2Configs(), ccmv2Mode.toString() + " config should match");
    }

    @Test
    public void testClusterProxyRegistrationWhenCCMV1() {
        Stack aStack = getAStack();
        aStack.setTunnel(Tunnel.CCM);
        aStack.setMinaSshdServiceId("minaSshdServiceId");

        GatewayConfig gatewayConfig = new GatewayConfig("connectionAddress", "publicAddress", "privateAddress",
                9443, "instanceId", false);

        ConfigRegistrationResponse configRegResposne = mock(ConfigRegistrationResponse.class);

        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResposne);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);

        underTest.registerFreeIpaForBootstrap(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();

        assertEquals(false, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertEquals(true, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should be enabled");
        assertEquals(List.of(new TunnelEntry("instanceId", "GATEWAY", "privateAddress", 9443, "minaSshdServiceId")),
                proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should be configured.");
    }

    @Test
    public void testClusterProxyRegistrationWhenCCMDisabled() {
        Stack aStack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");

        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        ConfigRegistrationResponse configRegResposne = mock(ConfigRegistrationResponse.class);

        when(stackService.getStackById(STACK_ID)).thenReturn(aStack);
        when(clusterProxyEnablementService.isClusterProxyApplicable(any())).thenReturn(true);
        when(gatewayConfigService.getPrimaryGatewayConfig(aStack)).thenReturn(gatewayConfig);
        when(securityConfigService.findOneByStack(aStack)).thenReturn(null);
        when(clusterProxyRegistrationClient.registerConfig(any())).thenReturn(configRegResposne);
        when(stackUpdater.updateClusterProxyRegisteredFlag(aStack, true)).thenReturn(aStack);
        when(freeIpaService.findByStack(aStack)).thenReturn(freeIpa);

        underTest.registerFreeIpaForBootstrap(STACK_ID);

        ArgumentCaptor<ConfigRegistrationRequest> captor = ArgumentCaptor.forClass(ConfigRegistrationRequest.class);
        verify(clusterProxyRegistrationClient).registerConfig(captor.capture());

        ConfigRegistrationRequest proxyRegisterationReq = captor.getValue();
        assertEquals(false, proxyRegisterationReq.isUseCcmV2(), "CCMV2 should not be enabled.");
        assertEquals(false, proxyRegisterationReq.isUseTunnel(), "CCMV1 tunnel should not be enabled");
        assertNull(proxyRegisterationReq.getCcmV2Configs(), "CCMV2 config should not be initialized");
        assertNull(proxyRegisterationReq.getTunnels(), "CCMV1 tunnel should not be initialized");
    }

    @Test
    public void testGetPath() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");

        String result = underTest.getProxyPath("testCrn");

        assertEquals("basePath/proxy/testCrn/freeipa", result);
    }

    @Test
    public void testGetPathWithStackWithoutServiceName() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        String result = underTest.getProxyPath(stack, Optional.empty());

        assertEquals("basePath/proxy/resourceCrn/freeipa.ipadom", result);
    }

    @Test
    public void testGetPathWithStackWithServiceNameNotRegistered() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        ReadConfigResponse readConfigResponse = new ReadConfigResponse();
        readConfigResponse.setServices(List.of());
        when(clusterProxyRegistrationClient.readConfig("resourceCrn")).thenReturn(readConfigResponse);

        String result = underTest.getProxyPath(stack, Optional.of("unregistered"));

        assertEquals("basePath/proxy/resourceCrn/freeipa.ipadom", result);
    }

    @Test
    public void testGetPathWithStackWithServiceNameNotRegisteredWithEmptyServices() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        ReadConfigResponse readConfigResponse = new ReadConfigResponse();
        when(clusterProxyRegistrationClient.readConfig("resourceCrn")).thenReturn(readConfigResponse);

        String result = underTest.getProxyPath(stack, Optional.of("unregistered"));

        assertEquals("basePath/proxy/resourceCrn/freeipa.ipadom", result);
    }

    @Test
    public void testGetPathWithStackWithServiceNameRegistered() {
        when(clusterProxyConfiguration.getClusterProxyBasePath()).thenReturn("basePath");
        Stack stack = getAStack();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setDomain("ipadom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        ReadConfigResponse readConfigResponse = new ReadConfigResponse();
        ReadConfigService service = new ReadConfigService();
        service.setName("registered");
        readConfigResponse.setServices(List.of(service));
        when(clusterProxyRegistrationClient.readConfig("resourceCrn")).thenReturn(readConfigResponse);

        String result = underTest.getProxyPath(stack, Optional.of("registered"));

        assertEquals("basePath/proxy/resourceCrn/registered", result);
    }

    private Stack getAStack() {
        Stack stack = new Stack();
        stack.setAccountId(TEST_ACCOUNT_ID);
        stack.setResourceCrn("resourceCrn");
        SecurityConfig securityConfig = new SecurityConfig();
        stack.setSecurityConfig(securityConfig);
        return stack;
    }
}
