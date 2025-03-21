package com.sequenceiq.cloudbreak.cloud.aws.common.validator;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.model.AwsDiskType;

@Component
public class AwsStorageValidator implements Validator {

    private static final Logger LOGGER = getLogger(AwsStorageValidator.class);

    @Inject
    private AwsPlatformResources awsPlatformResources;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        validateInstanceStorageOnInstanceTypes(ac, cloudStack);
    }

    private void validateInstanceStorageOnInstanceTypes(AuthenticatedContext ac, CloudStack cloudStack) {
        LOGGER.debug("Check instance storage availability on instance types");
        Set<String> instanceTypes = cloudStack.getGroups().stream()
                .map(Group::getInstances)
                .flatMap(Collection::stream)
                .filter(isEphemeralStorageTemplates())
                .map(CloudInstance::getTemplate)
                .map(InstanceTemplate::getFlavor)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(instanceTypes)) {
            LOGGER.debug("No instance types to check for instance storage.");
        } else {
            LOGGER.debug("Instance types to check: {}", instanceTypes);
            List<String> notSupportedTypes = getInstanceStorageNotSupportedTypes(ac, instanceTypes);
            if (!CollectionUtils.isEmpty(notSupportedTypes)) {
                LOGGER.warn("The following instance types does not support instance storage: {}", notSupportedTypes);
                throw new CloudConnectorException(String.format("The following instance types does not support instance storage: %s", notSupportedTypes));
            }
        }
    }

    private Predicate<CloudInstance> isEphemeralStorageTemplates() {
        return instance -> {
            InstanceTemplate template = instance.getTemplate();
            boolean hasEbsStorage = template.getVolumes().stream().map(Volume::getType).anyMatch(type -> !AwsDiskType.Ephemeral.value().equals(type));
            return hasEbsStorage && template.getTemporaryStorage() == TemporaryStorage.EPHEMERAL_VOLUMES;
        };
    }

    private List<String> getInstanceStorageNotSupportedTypes(AuthenticatedContext ac, Set<String> instanceTypes) {
        Location location = ac.getCloudContext().getLocation();
        CloudVmTypes cloudVmTypes = awsPlatformResources.virtualMachines(ac.getCloudCredential(), location.getRegion(), Map.of());
        Map<String, Set<VmType>> cloudVmResponses = cloudVmTypes.getCloudVmResponses();
        String az = location.getAvailabilityZone().value();
        return cloudVmResponses.get(az).stream()
                .filter(vmType -> instanceTypes.contains(vmType.value()))
                .filter(vmType -> Objects.isNull(vmType.getMetaData().getEphemeralConfig()))
                .map(VmType::value)
                .collect(Collectors.toList());
    }
}
