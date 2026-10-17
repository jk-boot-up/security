# Dependencies

This project uses one Java library for reading YAML, and three local tools, only for the integration tests.

## What SnakeYAML Engine is

reads the Kubernetes manifests into plain maps and lists. It is a YAML 1.2 parser and has no object construction from tags, which suits untrusted manifests.

## What Docker is

runs the kind node.

## What kind is

makes a real Kubernetes cluster inside a Docker container. The tests name it uniquely and always delete it.

## What kubectl is

asks the cluster `auth can-i` as each service account, to check the Java evaluator.

## Why this project uses them

The Java evaluator teaches how rules combine. A real cluster, asked in a different way by a tool other people wrote, is the proof that the model matches reality.

## What to install

A JDK for the demo and the ordinary tests. For the integration tests, Docker, kind and kubectl on the PATH. The versions in use are:

| Tool | Version |
| --- | --- |
| SnakeYAML Engine | 3.2 |
| kind | 0.33.0 |
| kubectl (client) | 1.27.2 |
| Kubernetes (kindest/node image) | 1.37.0 |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The integration tests take about three minutes, because a cluster starts and kubectl is called 490 times.
- The kubectl client is older than the cluster by more than the supported skew, which prints a warning but works for these questions.
- The tests need Docker, kind and kubectl installed.
