package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractStackTerminationAction<P extends Payload>
        extends AbstractStackAction<StackTerminationState, StackTerminationEvent, StackTerminationContext, P> {
    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Inject
    private StackUtil stackUtil;

    protected AbstractStackTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackTerminationContext createFlowContext(FlowParameters flowParameters,
            StateContext<StackTerminationState, StackTerminationEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(payload.getResourceId())));
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(stack.getTenant().getId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        List<CloudResource> resources = cloudResourceConverter.convert(stack.getResources());
        return createStackTerminationContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, resources, payload);
    }

    protected StackTerminationContext createStackTerminationContext(FlowParameters flowParameters, Stack stack, CloudContext cloudContext,
            CloudCredential cloudCredential, CloudStack cloudStack, List<CloudResource> resources, P payload) {
        return new StackTerminationContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, resources, false);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackTerminationContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
