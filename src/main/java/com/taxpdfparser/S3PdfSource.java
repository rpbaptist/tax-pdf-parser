package com.taxpdfparser;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public final class S3PdfSource implements PdfSource {

    private final S3Client s3;

    public S3PdfSource() {
        this(S3Client.create());
    }

    public S3PdfSource(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public byte[] fetch(String bucket, String key) {
        ResponseBytes<GetObjectResponse> object =
            s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
        return object.asByteArray();
    }

    @Override
    public void delete(String bucket, String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
