package com.jk.explore.security.cloudmisconf;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/** The AWS SDK for Java v2, pointed at the emulator with made-up credentials: the same calls a real account would get. */
public final class Cloud {

    public static final Region REGION = Region.EU_WEST_1;

    private final S3Client s3;
    private final Ec2Client ec2;
    private final IamClient iam;

    public Cloud(String endpoint) {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        this.s3 = S3Client.builder().endpointOverride(URI.create(endpoint)).region(REGION).credentialsProvider(credentials).httpClient(Apache5HttpClient.create())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
        this.ec2 = Ec2Client.builder().endpointOverride(URI.create(endpoint)).region(REGION).credentialsProvider(credentials).httpClient(Apache5HttpClient.create()).build();
        this.iam = IamClient.builder().endpointOverride(URI.create(endpoint)).region(Region.AWS_GLOBAL).credentialsProvider(credentials).httpClient(Apache5HttpClient.create()).build();
    }

    public S3Client s3() {
        return s3;
    }

    public Ec2Client ec2() {
        return ec2;
    }

    public IamClient iam() {
        return iam;
    }
}
