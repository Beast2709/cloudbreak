package com.sequenceiq.freeipa.converter.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@Component
public class InstanceGroupRequestToInstanceGroupConverter {

    @Inject
    private InstanceTemplateRequestToTemplateConverter templateConverter;

    @Inject
    private SecurityGroupRequestToSecurityGroupConverter securityGroupConverter;

    @Inject
    private InstanceGroupNetworkRequestToInstanceGroupNetworkConverter instanceGroupNetworkConverter;

    @Inject
    private DefaultInstanceGroupProvider defaultInstanceGroupProvider;

    public InstanceGroup convert(InstanceGroupRequest source, String accountId, Stack stack,
        FreeIpaServerRequest ipaServerRequest, String diskEncryptionSetId, DetailedEnvironmentResponse environment) {
        InstanceGroup instanceGroup = new InstanceGroup();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        instanceGroup.setTemplate(source.getInstanceTemplate() == null
                ? defaultInstanceGroupProvider.createDefaultTemplate(cloudPlatform, accountId, diskEncryptionSetId)
                : templateConverter.convert(source.getInstanceTemplate(), cloudPlatform, accountId, diskEncryptionSetId));
        instanceGroup.setSecurityGroup(securityGroupConverter.convert(source.getSecurityGroup()));
        instanceGroup.setInstanceGroupNetwork(source.getNetwork() == null
                ? defaultInstanceGroupProvider.createDefaultNetwork(cloudPlatform, stack.getNetwork())
                : instanceGroupNetworkConverter.convert(stack.getCloudPlatform(), source.getNetwork()));
        instanceGroup.setAvailabilityZones(getAvailabilityZoneFromEnv(instanceGroup, environment));
        String instanceGroupName = source.getName();
        instanceGroup.setGroupName(instanceGroupName);
        instanceGroup.setInstanceGroupType(source.getType());
        instanceGroup.setAttributes(defaultInstanceGroupProvider.createAttributes(cloudPlatform, stack.getName(), instanceGroupName));
        if (source.getNodeCount() > 0) {
            addInstanceMetadatas(source, instanceGroup, ipaServerRequest.getHostname(), ipaServerRequest.getDomain());
        }
        instanceGroup.setNodeCount(source.getNodeCount());
        return instanceGroup;
    }

    private void addInstanceMetadatas(InstanceGroupRequest request, InstanceGroup instanceGroup, String hostname, String domain) {
        Set<InstanceMetaData> instanceMetaDataSet = new LinkedHashSet<>();
        for (int i = 0; i < request.getNodeCount(); i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setDiscoveryFQDN(hostname + String.format("%d.", i) + domain);
            instanceMetaDataSet.add(instanceMetaData);
        }
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
    }

    private Set<String> getAvailabilityZoneFromEnv(InstanceGroup group, DetailedEnvironmentResponse environment) {
        if (environment.getNetwork() != null
                && environment.getNetwork().getSubnetMetas() != null
                && (group.getAvailabilityZones() == null || group.getAvailabilityZones().isEmpty())) {
            return getAvailabilityZones(environment, group.getInstanceGroupNetwork().getAttributes());
        } else {
            return Set.of();
        }
    }

    private Set<String> getAvailabilityZones(DetailedEnvironmentResponse environment, Json attributes) {
        Set<String> azs = new HashSet<>();
        if (attributes != null) {
            List<String> subnetIds = (List<String>) attributes.getMap().getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>());
            for (String subnetId : subnetIds) {
                for (Map.Entry<String, CloudSubnet> cloudSubnetEntry : environment.getNetwork().getSubnetMetas().entrySet()) {
                    CloudSubnet value = cloudSubnetEntry.getValue();
                    if (subnetId.equals(value.getId()) || subnetId.equals(value.getName())) {
                        azs.add(value.getAvailabilityZone());
                        break;
                    }
                }
            }
        }
        return azs;
    }
}
