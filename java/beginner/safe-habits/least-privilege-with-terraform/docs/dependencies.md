# Dependencies

This project uses one Java library, for reading JSON, and four local tools, only for the integration tests.

## What Jackson (jackson-databind) is

reads the policy documents into trees. Version 3 changed its package to tools.jackson.

## What OpenTofu is

the open-source fork of Terraform. It turns the Terraform code into calls to the IAM service. The tests use it, and fall back to Terraform if it is not installed.

## What Terraform AWS provider is

the plugin that knows the AWS IAM calls. It is downloaded once into a cache folder in the home directory, which is large (hundreds of megabytes).

## What LocalStack is

a local AWS emulator, run in Docker. The community image 4.14.0 is the last one that starts without an account token; newer images need one.

## What Docker is

runs the emulator.

## Why this project uses them

The Java evaluator teaches how the rules combine. Applying the same code to an emulator, and reading it back, proves that the JSON the evaluator reads is the JSON the cloud would store.

## What to install

A JDK for the demo and the ordinary tests. For the integration tests, Docker and OpenTofu (`brew install opentofu`) or Terraform on the PATH. The versions in use are:

| Tool | Version |
| --- | --- |
| Jackson (jackson-databind) | 3.2.3 |
| OpenTofu | 1.13.1 |
| Terraform AWS provider | 6.69.1 |
| LocalStack (community image) | 4.14.0 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The first integration run downloads the AWS provider, which is large, and the emulator image.
- The emulator image is the last community one, so it will not get newer AWS features.
- The free emulator does not enforce IAM policies, so enforcement is not tested.
- The integration tests need Docker and OpenTofu or Terraform installed.
