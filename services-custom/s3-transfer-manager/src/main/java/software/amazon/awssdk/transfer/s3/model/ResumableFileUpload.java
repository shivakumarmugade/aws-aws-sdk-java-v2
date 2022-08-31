/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.transfer.s3.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * POJO class that holds the state and can be used to resume a paused upload file operation.
 * <p>
 * <b>Serialization: </b>When serializing this token, the following structures will not be preserved/persisted:
 * <ul>
 *     <li>{@link TransferRequestOverrideConfiguration}</li>
 *     <li>{@link AwsRequestOverrideConfiguration} (from {@link PutObjectRequest})</li>
 * </ul>
 *
 * @see S3TransferManager#uploadFile(UploadFileRequest)
 * @see S3TransferManager#resumeUploadFile(ResumableFileUpload)
 */
@SdkPublicApi
public final class ResumableFileUpload implements ResumableTransfer,
                                                  ToCopyableBuilder<ResumableFileUpload.Builder, ResumableFileUpload> {

    private final UploadFileRequest uploadFileRequest;
    private final Instant fileLastModified;
    private final String multipartUploadId;
    private final Long partSizeInBytes;
    private final Long totalNumOfParts;
    private final long fileLength;

    private ResumableFileUpload(DefaultBuilder builder) {
        this.uploadFileRequest = Validate.paramNotNull(builder.uploadFileRequest, "uploadFileRequest");
        this.fileLastModified = Validate.paramNotNull(builder.fileLastModified, "fileLastModified");
        this.fileLength = Validate.paramNotNull(builder.fileLength, "fileLength");
        this.multipartUploadId = builder.multipartUploadId;
        this.totalNumOfParts = builder.totalNumOfParts;
        this.partSizeInBytes = builder.partSizeInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResumableFileUpload that = (ResumableFileUpload) o;

        if (fileLength != that.fileLength) {
            return false;
        }
        if (!uploadFileRequest.equals(that.uploadFileRequest)) {
            return false;
        }
        if (!fileLastModified.equals(that.fileLastModified)) {
            return false;
        }
        if (!Objects.equals(multipartUploadId, that.multipartUploadId)) {
            return false;
        }
        if (!Objects.equals(partSizeInBytes, that.partSizeInBytes)) {
            return false;
        }
        return Objects.equals(totalNumOfParts, that.totalNumOfParts);
    }

    @Override
    public int hashCode() {
        int result = uploadFileRequest.hashCode();
        result = 31 * result + fileLastModified.hashCode();
        result = 31 * result + (multipartUploadId != null ? multipartUploadId.hashCode() : 0);
        result = 31 * result + (partSizeInBytes != null ? partSizeInBytes.hashCode() : 0);
        result = 31 * result + (totalNumOfParts != null ? totalNumOfParts.hashCode() : 0);
        result = 31 * result + (int) (fileLength ^ (fileLength >>> 32));
        return result;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * @return the {@link UploadFileRequest} to resume
     */
    public UploadFileRequest uploadFileRequest() {
        return uploadFileRequest;
    }

    /**
     * Last modified time of the file since last pause
     */
    public Instant fileLastModified() {
        return fileLastModified;
    }

    /**
     * File length since last pause
     */
    public long fileLength() {
        return fileLength;
    }

    /**
     * Return the part size in bytes or {@link OptionalLong#empty()} if unknown
     */
    public OptionalLong partSizeInBytes() {
        return partSizeInBytes == null ? OptionalLong.empty() : OptionalLong.of(partSizeInBytes);
    }

    /**
     * Return the total number of parts associated with this transfer or {@link OptionalLong#empty()} if unknown
     */
    public OptionalLong totalNumOfParts() {
        return totalNumOfParts == null ? OptionalLong.empty() : OptionalLong.of(totalNumOfParts);
    }

    /**
     * The multipart upload ID, or {@link Optional#empty()} if unknown
     *
     * @return the optional total size of the transfer.
     */
    public Optional<String> multipartUploadId() {
        return Optional.ofNullable(multipartUploadId);
    }

    @Override
    public String toString() {
        return ToString.builder("ResumableFileUpload")
                       .add("fileLastModified", fileLastModified)
                       .add("multipartUploadId", multipartUploadId)
                       .add("uploadFileRequest", uploadFileRequest)
                       .add("fileLength", fileLength)
                       .add("totalNumOfParts", totalNumOfParts)
                       .add("partSizeInBytes", partSizeInBytes)
                       .build();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, ResumableFileUpload> {

        /**
         * Sets the upload file request
         *
         * @param uploadFileRequest the upload file request
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder uploadFileRequest(UploadFileRequest uploadFileRequest);

        /**
         * The {@link UploadFileRequest} request
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link UploadFileRequest} builder avoiding the
         * need to create one manually via {@link UploadFileRequest#builder()}.
         *
         * @param uploadFileRequestBuilder the upload file request builder
         * @return a reference to this object so that method calls can be chained together.
         * @see #uploadFileRequest(UploadFileRequest)
         */
        default ResumableFileUpload.Builder uploadFileRequest(Consumer<UploadFileRequest.Builder>
                                                                      uploadFileRequestBuilder) {
            UploadFileRequest request = UploadFileRequest.builder()
                                                             .applyMutation(uploadFileRequestBuilder)
                                                             .build();
            uploadFileRequest(request);
            return this;
        }

        /**
         * Sets multipart ID associated with this transfer
         *
         * @param multipartUploadId the multipart ID
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder multipartUploadId(String multipartUploadId);

        /**
         * Sets the last modified time of the object
         *
         * @param fileLastModified the last modified time of the file
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder fileLastModified(Instant fileLastModified);

        /**
         * Sets the file length
         *
         * @param fileLength the last modified time of the object
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder fileLength(Long fileLength);

        /**
         * Sets the total number of parts
         *
         * @param totalNumOfParts the total number of parts
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder totalNumOfParts(Long totalNumOfParts);

        /**
         * The part size associated with this transfer
         * @param partSizeInBytes the part size in bytes
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder partSizeInBytes(Long partSizeInBytes);
    }

    private static final class DefaultBuilder implements Builder {

        private String multipartUploadId;
        private UploadFileRequest uploadFileRequest;
        private Long partSizeInBytes;
        private Long totalNumOfParts;
        private Instant fileLastModified;
        private Long fileLength;

        private DefaultBuilder() {
        }

        private DefaultBuilder(ResumableFileUpload persistableFileUpload) {
            this.multipartUploadId = persistableFileUpload.multipartUploadId;
            this.uploadFileRequest = persistableFileUpload.uploadFileRequest;
            this.partSizeInBytes = persistableFileUpload.partSizeInBytes;
            this.fileLastModified = persistableFileUpload.fileLastModified;
            this.totalNumOfParts = persistableFileUpload.totalNumOfParts;
            this.fileLength = persistableFileUpload.fileLength;
        }

        @Override
        public Builder uploadFileRequest(UploadFileRequest uploadFileRequest) {
            this.uploadFileRequest = uploadFileRequest;
            return this;
        }

        @Override
        public Builder multipartUploadId(String mutipartUploadId) {
            this.multipartUploadId = mutipartUploadId;
            return this;
        }


        @Override
        public Builder fileLastModified(Instant fileLastModified) {
            this.fileLastModified = fileLastModified;
            return this;
        }

        @Override
        public Builder fileLength(Long fileLength) {
            this.fileLength = fileLength;
            return this;
        }

        @Override
        public Builder totalNumOfParts(Long totalNumOfParts) {
            this.totalNumOfParts = totalNumOfParts;
            return this;
        }

        @Override
        public Builder partSizeInBytes(Long partSizeInBytes) {
            this.partSizeInBytes = partSizeInBytes;
            return this;
        }

        @Override
        public ResumableFileUpload build() {
            return new ResumableFileUpload(this);
        }
    }
}
