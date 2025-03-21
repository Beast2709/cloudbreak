package com.sequenceiq.datalake.flow.create;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.INIT_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_START_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_STORAGE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_WAIT_ENV_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_CREATION_WAIT_RDS_STATE;
import static com.sequenceiq.datalake.flow.create.SdxCreateState.SDX_STACK_CREATION_IN_PROGRESS_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class SdxCreateFlowConfig extends AbstractFlowConfiguration<SdxCreateState, SdxCreateEvent> implements RetryableFlowConfiguration<SdxCreateEvent> {

    private static final List<Transition<SdxCreateState, SdxCreateEvent>> TRANSITIONS = new Builder<SdxCreateState, SdxCreateEvent>()
            .defaultFailureEvent(SDX_CREATE_FAILED_EVENT)

            .from(INIT_STATE)
            .to(SDX_CREATION_STORAGE_VALIDATION_STATE)
            .event(SdxCreateEvent.STORAGE_VALIDATION_WAIT_EVENT).defaultFailureEvent()

            .from(SDX_CREATION_STORAGE_VALIDATION_STATE)
            .to(SDX_CREATION_WAIT_RDS_STATE)
            .event(SdxCreateEvent.STORAGE_VALIDATION_SUCCESS_EVENT).defaultFailureEvent()

            .from(SDX_CREATION_WAIT_RDS_STATE)
            .to(SDX_CREATION_WAIT_ENV_STATE)
            .event(SdxCreateEvent.RDS_WAIT_SUCCESS_EVENT).defaultFailureEvent()

            .from(SDX_CREATION_WAIT_ENV_STATE)
            .to(SDX_CREATION_START_STATE)
            .event(SdxCreateEvent.ENV_WAIT_SUCCESS_EVENT).defaultFailureEvent()

            .from(SDX_CREATION_START_STATE)
            .to(SDX_STACK_CREATION_IN_PROGRESS_STATE)
            .event(SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT).defaultFailureEvent()

            .from(SDX_STACK_CREATION_IN_PROGRESS_STATE)
            .to(SDX_CREATION_FINISHED_STATE)
            .event(SdxCreateEvent.SDX_STACK_CREATION_SUCCESS_EVENT).failureEvent(SDX_CREATE_FAILED_EVENT)

            .from(SDX_CREATION_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(SdxCreateEvent.SDX_CREATE_FINALIZED_EVENT).defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<SdxCreateState, SdxCreateEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SDX_CREATION_FAILED_STATE, SDX_CREATE_FAILED_HANDLED_EVENT);

    public SdxCreateFlowConfig() {
        super(SdxCreateState.class, SdxCreateEvent.class);
    }

    @Override
    public SdxCreateEvent[] getEvents() {
        return SdxCreateEvent.values();
    }

    @Override
    public SdxCreateEvent[] getInitEvents() {
        return new SdxCreateEvent[]{
                SdxCreateEvent.STORAGE_VALIDATION_WAIT_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Create SDX";
    }

    @Override
    protected List<Transition<SdxCreateState, SdxCreateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxCreateState, SdxCreateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxCreateEvent getRetryableEvent() {
        return SDX_CREATE_FAILED_HANDLED_EVENT;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }
}
