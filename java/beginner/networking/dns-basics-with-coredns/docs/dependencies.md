# Dependencies

This project needs Docker, and uses one container image. The Java code is the plain JDK only.

## What Docker is

a tool that runs programs in isolated containers. The demo starts a CoreDNS container and stops and removes it afterwards. Docker must be installed and running.

## What CoreDNS is

an open-source DNS server written in Go, the default name service in Kubernetes. It is configured by a Corefile that lists plugins; `file`, `rewrite`, `cache` and `errors` are used here.

## Why this project uses them

The plain project models DNS messages and a resolver in Java. A real server is written by other people and follows the standard, so asking it questions with our own encoder and decoder is the proof that the model matches reality, and its plugins show what a real server adds: zones, rewrites and a cache with a time-to-live countdown.

## What to install

A JDK and a running Docker. Gradle downloads the Java side, Docker pulls the CoreDNS image the first time, and the versions are pinned:

| Tool | Version |
| --- | --- |
| CoreDNS image | coredns/coredns:1.14.7 |
| Docker | any recent Docker Engine or Docker Desktop |
| Java | 25 |
| JUnit | 6.1.3 |

## What it costs

- The demo and the container tests need Docker.
- The first run downloads the image.
- Only the A record over IPv4 is parsed by the Java code; CoreDNS speaks far more.
