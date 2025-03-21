package com.sequenceiq.cloudbreak.cmtemplate.configproviders.cruisecontrol;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@ExtendWith(MockitoExtension.class)
public class CruiseControlRoleConfigProviderTest {

    private final CruiseControlRoleConfigProvider provider = new CruiseControlRoleConfigProvider();

    @Mock
    private BlueprintView blueprintView;

    @Mock
    private CmTemplateProcessor processor;

    @Test
    void testRoleConfigWithCdpVersionIs7211() {
        cdpMainVersionIs("7.2.11");
        HostgroupView hostGroup = new HostgroupView("test group");
        assertEquals(createExpectedConfigWithStackVersionAtLeast7211(),
                provider.getRoleConfigs(CruiseControlRoles.CRUISE_CONTROL_SERVER, hostGroup, getTemplatePreparationObject(hostGroup)));
    }

    @Test
    void testRoleConfigWithCdpVersionIsHigherThan7211() {
        cdpMainVersionIs("7.2.12");
        HostgroupView hostGroup = new HostgroupView("test group");
        assertEquals(createExpectedConfigWithStackVersionAtLeast7211(),
                provider.getRoleConfigs(CruiseControlRoles.CRUISE_CONTROL_SERVER, hostGroup, getTemplatePreparationObject(hostGroup)));
    }

    @Test
    void testRoleConfigWithCdpVersionIsLowerThan7211() {
        cdpMainVersionIs("7.2.10");
        HostgroupView hostGroup = new HostgroupView("test group");
        assertEquals(List.of(), provider.getRoleConfigs(CruiseControlRoles.CRUISE_CONTROL_SERVER, hostGroup, getTemplatePreparationObject(hostGroup)));
    }

    private TemplatePreparationObject getTemplatePreparationObject(HostgroupView hostGroup) {
        TemplatePreparationObject preparationObject = TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(hostGroup))
                .withBlueprintView(blueprintView)
                .build();
        return preparationObject;
    }

    private void cdpMainVersionIs(String version) {
        when(blueprintView.getProcessor()).thenReturn(processor);
        when(processor.getStackVersion()).thenReturn(version);
    }

    private List<ApiClusterTemplateConfig> createExpectedConfigWithStackVersionAtLeast7211() {
        return List.of(config("self.healing.enabled", "true"),
                config("anomaly.notifier.class", "com.linkedin.kafka.cruisecontrol.detector.notifier.SelfHealingNotifier"),
                config("metric.anomaly.finder.class", "com.cloudera.kafka.cruisecontrol.detector.EmptyBrokerAnomalyFinder"),
                config("cpu.balance.threshold", "1.5"),
                config("disk.balance.threshold", "1.5"),
                config("network.inbound.balance.threshold", "1.5"),
                config("network.outbound.balance.threshold", "1.5"),
                config("replica.count.balance.threshold", "1.5"),
                config("disk.capacity.threshold", "0.85"),
                config("cpu.capacity.threshold", "0.75"),
                config("network.inbound.capacity.threshold", "0.85"),
                config("network.outbound.capacity.threshold", "0.85"),
                config("leader.replica.count.balance.threshold", "1.5"),
                config("anomaly.detection.goals", "com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal"),
                config("default.goals", "com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaDistributionGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskUsageDistributionGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuUsageDistributionGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.TopicReplicaDistributionGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.LeaderReplicaDistributionGoal"),
                config("hard.goals", "com.linkedin.kafka.cruisecontrol.analyzer.goals.RackAwareGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.ReplicaCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.DiskCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkInboundCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.NetworkOutboundCapacityGoal," +
                        "com.linkedin.kafka.cruisecontrol.analyzer.goals.CpuCapacityGoal"),
                config("auth_method", "Trusted Proxy"),
                config("auth_admins", "kafka"),
                config("trusted.proxy.spnego.fallback.enabled", "true")
        );
    }

}
