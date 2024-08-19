package software.amazon.awssdk.services.smithyrpcv2protocol.transform;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.runtime.transform.Marshaller;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.services.smithyrpcv2protocol.model.NoInputOutputRequest;
import software.amazon.awssdk.utils.MapUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * {@link NoInputOutputRequest} Marshaller
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public class NoInputOutputRequestMarshaller implements Marshaller<NoInputOutputRequest> {
    private static final OperationInfo SDK_OPERATION_BINDING = OperationInfo.builder()
            .requestUri("/service/RpcV2Protocol/operation/NoInputOutput").httpMethod(SdkHttpMethod.POST)
            .hasExplicitPayloadMember(false).hasImplicitPayloadMembers(false).hasPayloadMembers(false)
            .putAdditionalMetadata(BaseAwsJsonProtocolFactory.HTTP_EXTRA_HEADERS, MapUtils.of("smithy-protocol", "rpc-v2-cbor"))
            .putAdditionalMetadata(BaseAwsJsonProtocolFactory.USE_NO_OP_GENERATOR, true).build();

    private final BaseAwsJsonProtocolFactory protocolFactory;

    public NoInputOutputRequestMarshaller(BaseAwsJsonProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    @Override
    public SdkHttpFullRequest marshall(NoInputOutputRequest noInputOutputRequest) {
        Validate.paramNotNull(noInputOutputRequest, "noInputOutputRequest");
        try {
            ProtocolMarshaller<SdkHttpFullRequest> protocolMarshaller = protocolFactory
                    .createProtocolMarshaller(SDK_OPERATION_BINDING);
            return protocolMarshaller.marshall(noInputOutputRequest);
        } catch (Exception e) {
            throw SdkClientException.builder().message("Unable to marshall request to JSON: " + e.getMessage()).cause(e).build();
        }
    }
}
