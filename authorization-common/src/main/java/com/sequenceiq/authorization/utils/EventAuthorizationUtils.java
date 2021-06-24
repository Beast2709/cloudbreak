package com.sequenceiq.authorization.utils;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ACTION_PREFIX;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.GET_ACTION_PREFIX;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Component
public class EventAuthorizationUtils {

    private CommonPermissionCheckingUtils permissionCheckingUtils;

    public EventAuthorizationUtils(CommonPermissionCheckingUtils permissionCheckingUtils) {
        this.permissionCheckingUtils = permissionCheckingUtils;
    }

    public void checkPermissionBasedOnResourceTypeAndCrn(Collection<EventAuthorizationDto> eventAuthorizationDtos) {
        throwIfNull(eventAuthorizationDtos,
                () -> new IllegalArgumentException("The collection of " + EventAuthorizationDto.class.getSimpleName() + "s should not be null!"));
        for (EventAuthorizationDto dto : eventAuthorizationDtos) {
            String resourceType = dto.getResourceType();
            Arrays.asList(AuthorizationResourceAction.values()).stream()
                    .filter(action -> isResourceTypeHasDescribeOrGetAction(action, resourceType))
                    .findFirst()
                    .ifPresentOrElse(
                            action -> checkPermissionForResource(action, dto.getResourceCrn()),
                            () -> throwIllegalStateExceptionForResourceType(resourceType)
                    );
        }
    }

    private boolean isResourceTypeHasDescribeOrGetAction(AuthorizationResourceAction action, String resourceType) {
        return (action.name().startsWith(DESCRIBE_ACTION_PREFIX) || action.name().contains(GET_ACTION_PREFIX))
                && action.name().contains(resourceType.toUpperCase());
    }

    private void checkPermissionForResource(AuthorizationResourceAction action, String resourceCrn) {
        permissionCheckingUtils.checkPermissionForUserOnResource(
                action,
                ThreadBasedUserCrnProvider.getUserCrn(),
                resourceCrn);
    }

    private void throwIllegalStateExceptionForResourceType(String resourceType) {
        throw new IllegalStateException("Unable to find AuthZ action for resource: " + resourceType);
    }

}
