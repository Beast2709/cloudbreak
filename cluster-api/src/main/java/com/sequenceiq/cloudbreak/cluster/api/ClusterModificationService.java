package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

public interface ClusterModificationService {

    List<String> upscaleCluster(HostGroup hostGroup, Collection<InstanceMetaData> metas) throws CloudbreakException;

    void stopCluster(boolean full) throws CloudbreakException;

    int startCluster() throws CloudbreakException;

    Map<String, String> getComponentsByCategory(String blueprintName, String hostGroupName);

    String getStackRepositoryJson(StackRepoDetails repoDetails, String stackRepoId);

    void cleanupCluster(Telemetry telemetry) throws CloudbreakException;

    void upgradeClusterRuntime(Set<ClusterComponent> components, boolean patchUpgrade) throws CloudbreakException;

    Map<String, String> gatherInstalledParcels(String stackName);

    void updateServiceConfigAndRestartService(String serviceName, String configName, String newConfigValue) throws Exception;

    void downloadAndDistributeParcels(Set<ClusterComponent> components, boolean patchUpgrade) throws CloudbreakException;

    Optional<String> getRoleConfigValueByServiceType(String clusterName, String roleConfigGroup, String serviceType, String configName);

    default void removeUnusedParcels(Set<ClusterComponent> usedParcelComponents) throws CloudbreakException {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void stopComponents(Map<String, String> components, String hostname) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void ensureComponentsAreStopped(Map<String, String> components, String hostname) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void initComponents(Map<String, String> components, String hostname) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void installComponents(Map<String, String> components, String hostname) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void regenerateKerberosKeytabs(String hostname, KerberosConfig kerberosConfig) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void startComponents(Map<String, String> components, String hostname) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void restartAll(boolean withMgmtServices) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

}
