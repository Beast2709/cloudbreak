package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.ResourcePropertyProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CustomConfigProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.exception.CustomConfigsException;
import com.sequenceiq.cloudbreak.repository.CustomConfigsRepository;

@Service
public class CustomConfigsService implements ResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigsService.class);

    @Inject
    private CustomConfigsRepository customConfigsRepository;

    @Inject
    private CustomConfigsValidator validator;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public void initializeCrnForCustomConfigs(CustomConfigs customConfigs, String accountId) {
        customConfigs.setResourceCrn(regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CUSTOM_CONFIGS, accountId));
    }

    public List<CustomConfigs> getAll(String accountId) {
        return customConfigsRepository.findCustomConfigsByAccountId(accountId);
    }

    public CustomConfigs getByNameOrCrn(NameOrCrn nameOrCrn, String accountId) {
        return nameOrCrn.hasName() ? getByName(nameOrCrn.getName(), accountId) : getByCrn(nameOrCrn.getCrn());
    }

    public CustomConfigs getByCrn(String crn) {
        CustomConfigs toReturn = null;
        if (crn != null) {
            toReturn = customConfigsRepository.findCustomConfigsByResourceCrn(crn)
                    .orElseThrow(NotFoundException.notFound("Custom configs", crn));
        }
        return toReturn;
    }

    private List<CustomConfigProperty> getCustomServiceConfigs(Set<CustomConfigProperty> configs) {
        return configs.stream()
                .filter(config -> config.getRoleType() == null)
                .collect(Collectors.toList());
    }

    private List<CustomConfigProperty> getCustomRoleConfigs(Set<CustomConfigProperty> configs) {
        return configs.stream()
                .filter(config -> config.getRoleType() != null)
                .collect(Collectors.toList());
    }

    private void validate(CustomConfigs customConfigs) {
        try {
            validator.validateIfAccountIsEntitled(ThreadBasedUserCrnProvider.getAccountId());
            validator.validateServiceNames(customConfigs);
        } catch (IOException e) {
            LOGGER.error("Could not validate Custom configs");
        }
    }

    public Map<String, List<ApiClusterTemplateConfig>> getCustomServiceConfigsMap(CustomConfigs customConfigs) {
        Map<String, List<ApiClusterTemplateConfig>> serviceMappedToConfigs = new HashMap<>();
        List<CustomConfigProperty> customServiceConfigsList = getCustomServiceConfigs(customConfigs.getConfigs());
        customServiceConfigsList.forEach(serviceConfig -> {
            serviceMappedToConfigs.computeIfAbsent(serviceConfig.getServiceType(), k -> new ArrayList<>());
            serviceMappedToConfigs.get(serviceConfig.getServiceType()).add(new ApiClusterTemplateConfig()
                    .name(serviceConfig.getConfigName()).value(serviceConfig.getConfigValue()));
        });
        return serviceMappedToConfigs;
    }

    public Map<String, List<ApiClusterTemplateRoleConfigGroup>> getCustomRoleConfigsMap(CustomConfigs customConfigs) {
        Map<String, List<ApiClusterTemplateRoleConfigGroup>> serviceMappedToRoleConfigs = new HashMap<>();
        List<CustomConfigProperty> customRoleConfigGroupsList = getCustomRoleConfigs(customConfigs.getConfigs());
        customRoleConfigGroupsList.forEach(customRoleConfigGroup -> {
            String configName = customRoleConfigGroup.getConfigName();
            String configValue = customRoleConfigGroup.getConfigValue();
            serviceMappedToRoleConfigs.computeIfAbsent(customRoleConfigGroup.getServiceType(), k -> new ArrayList<>());
            Optional<ApiClusterTemplateRoleConfigGroup> roleConfigGroupIfExists = serviceMappedToRoleConfigs.get(customRoleConfigGroup.getServiceType()).stream()
                    .filter(rcg -> rcg.getRoleType().equalsIgnoreCase(customRoleConfigGroup.getRoleType()))
                    .findFirst();
            roleConfigGroupIfExists.ifPresentOrElse(roleConfigGroup -> roleConfigGroup.getConfigs().add(new ApiClusterTemplateConfig()
                            .name(configName).value(configValue)),
                    () -> serviceMappedToRoleConfigs.get(customRoleConfigGroup.getServiceType()).add(new ApiClusterTemplateRoleConfigGroup()
                    .roleType(customRoleConfigGroup.getRoleType()).addConfigsItem(new ApiClusterTemplateConfig().name(configName)
                            .value(configValue))));
        });
        return serviceMappedToRoleConfigs;
    }

    public CustomConfigs getByName(String name, String accountId) {
        CustomConfigs toReturn = null;
        if (name != null) {
            toReturn = customConfigsRepository.findCustomConfigsByNameAndAccountId(name, accountId)
                    .orElseThrow(NotFoundException.notFound("Custom configs", name));
        }
        return toReturn;
    }

    public CustomConfigs create(CustomConfigs customConfigs, String accountId) {
        //validation
        validate(customConfigs);
        Optional<CustomConfigs> customConfigsByName =
                customConfigsRepository.findCustomConfigsByNameAndAccountId(customConfigs.getName(), accountId);
        if (customConfigsByName.isPresent()) {
            throw new CustomConfigsException("Custom Configs with name " +
                    customConfigsByName.get().getName() + "exists. Provide a different name");
        }
        initializeCrnForCustomConfigs(customConfigs, accountId);
        customConfigs.setAccount(accountId);
        customConfigs.getConfigs().forEach(config -> config.setCustomConfigs(customConfigs));
        customConfigsRepository.save(customConfigs);
        return customConfigs;
    }

    public CustomConfigs clone(NameOrCrn nameOrCrn, String newName, String accountId) {
        return nameOrCrn.hasName() ? cloneByName(nameOrCrn.getName(), newName, accountId) : cloneByCrn(nameOrCrn.getCrn(), newName, accountId);
    }

    public CustomConfigs cloneByName(String name, String newCustomConfigsName, String accountId) {
        Optional<CustomConfigs> customConfigsByName = Optional.of(customConfigsRepository.findCustomConfigsByNameAndAccountId(name, accountId)
                .orElseThrow(() -> new NotFoundException("Custom configs with name " + name + " does not exist. Cannot be cloned.")));
        CustomConfigs newCustomConfigs = new CustomConfigs(customConfigsByName.get());
        Set<CustomConfigProperty> newConfigSet = new HashSet<>(customConfigsByName.get().getConfigs());
        newConfigSet.forEach(config -> {
            config.setId(null);
            config.setCustomConfigs(null);
        });
        newCustomConfigs.setConfigs(newConfigSet);
        newCustomConfigs.setName(newCustomConfigsName);
        return create(newCustomConfigs, accountId);
    }

    public CustomConfigs cloneByCrn(String crn, String newCustomConfigsName, String accountId) {
        Optional<CustomConfigs> customConfigsByCrn = Optional.of(customConfigsRepository.findCustomConfigsByResourceCrn(crn))
                .orElseThrow(() -> new NotFoundException("Custom configs with crn " + crn + " not found. Cannot be cloned."));
        CustomConfigs newCustomConfigs = new CustomConfigs(customConfigsByCrn.get());
        Set<CustomConfigProperty> newConfigSet = Set.copyOf(customConfigsByCrn.get().getConfigs());
        newConfigSet.forEach(config -> {
            config.setId(null);
            config.setCustomConfigs(null);
        });
        newCustomConfigs.setConfigs(newConfigSet);
        newCustomConfigs.setName(newCustomConfigsName);
        return create(newCustomConfigs, accountId);
    }

    public CustomConfigs deleteByCrn(String crn) {
        CustomConfigs customConfigsByCrn = getByCrn(crn);
        customConfigsRepository.deleteById(customConfigsByCrn.getId());
        return customConfigsByCrn;
    }

    public CustomConfigs deleteByName(String name, String accountId) {
        CustomConfigs customConfigsByName = getByName(name, accountId);
        customConfigsRepository.deleteById(customConfigsByName.getId());
        return customConfigsByName;
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return customConfigsRepository.findResourceCrnsByNamesAndAccountId(ThreadBasedUserCrnProvider.getAccountId(), resourceNames);
    }

    @Override
    public Optional<AuthorizationResourceType> getSupportedAuthorizationResourceType() {
        return Optional.of(AuthorizationResourceType.CUSTOM_CONFIGS);
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.CUSTOM_CONFIGS);
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrns(Collection<String> crns) {
        Map<String, Optional<String>> crnMappedToName = new HashMap<>();
        customConfigsRepository.findResourceNamesByCrnsAndAccountId(ThreadBasedUserCrnProvider.getAccountId(), crns)
                .forEach(nameAndCrn -> crnMappedToName.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return crnMappedToName;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return customConfigsRepository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(NotFoundException.notFound("CustomConfigs", resourceName));
    }
}
