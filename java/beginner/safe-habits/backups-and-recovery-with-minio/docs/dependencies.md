# Dependencies

This project uses the AWS SDK for Java to talk to the store, and two Docker images: the object store and its admin tool.

## What AWS SDK for Java v2 (s3, url-connection-client) is

the standard Java client for the S3 API. It works unchanged against Amazon S3, MinIO, RustFS and other S3-compatible stores; here it is pointed at the loopback address with path-style access.

## What RustFS (rustfs/rustfs) is

an open-source S3-compatible object store. MinIO's community edition no longer publishes container images, so this project runs RustFS, which implements the same S3 features and a MinIO-style admin API. The lesson does not depend on which store it is.

## What rc (rustfs/rc) is

the command-line admin tool of RustFS, used only to create the backup user, its policy and the attachment. It plays the role that `mc admin` plays for MinIO.

## What Docker is

runs the two images. The container and its private network are removed when the run ends.

## Why this project uses them

The earlier backup project modelled the ideas with folders. Versioning, Object Lock and policies are features of a real object store, so a real one is the only honest way to show that they behave as described, and the same code works against Amazon S3.

## What to install

A JDK, and Docker for the demo and the integration tests. Gradle downloads the Java libraries and Docker pulls the images, and the versions are pinned:

| Tool | Version |
| --- | --- |
| AWS SDK for Java v2 | 2.56.0 |
| RustFS (image) | 1.0.2 |
| rc admin tool (image) | v0.1.36 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- A Docker image of a few hundred megabytes on the first run.
- The SDK's default checksum behaviour had to be set to "when required", because this store rejected the SDK's streaming checksum trailer; the explicit SHA-256 header is used instead.
- The demo needs Docker, so it runs under `integrationTest`.
