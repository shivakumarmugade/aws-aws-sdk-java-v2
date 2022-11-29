package software.amazon.awssdk.services.jsonprotocoltests.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventStreamOperationWithOnlyInputRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link EventStreamOperationWithOnlyInputRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class EventStreamOperationWithOnlyInputRequestMarshaller implements Marshaller<EventStreamOperationWithOnlyInputRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
            .requestUri("/2016-03-11/EventStreamOperationWithOnlyInput").httpMethod(SdkHttpMethod.POST)
            .hasExplicitPayloadMember(false).hasImplicitPayloadMembers(true).hasPayloadMembers(true).hasEventStreamingInput(true)
            .enableTraitValidation(RequiredTrait.class).build();

    private final BaseAwsJsonProtocolFactory protocolFactory;

    public EventStreamOperationWithOnlyInputRequestMarshaller(BaseAwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public SdkHttpFullRequest marshall(EventStreamOperationWithOnlyInputRequest eventStreamOperationWithOnlyInputRequest) {
        Validate.paramNotNull(eventStreamOperationWithOnlyInputRequest, "eventStreamOperationWithOnlyInputRequest");
        try {
            ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = protocolFactory
                    .createProtocolMarshaller(SDK_OPERATION_BINDING);
            return protocolMarshaller.marshall(eventStreamOperationWithOnlyInputRequest);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}
