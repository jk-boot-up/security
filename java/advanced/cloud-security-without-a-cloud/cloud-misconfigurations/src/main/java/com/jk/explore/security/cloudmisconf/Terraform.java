package com.jk.explore.security.cloudmisconf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The same account as OpenTofu/Terraform files, in two versions: as the team first wrote it and as it should be. The scanners read these before anything is deployed. */
public final class Terraform {

    private Terraform() {
    }

    static final String FIRST = """
            resource "aws_s3_bucket" "statements" {
              bucket = "bank-statements"
            }
            resource "aws_s3_bucket_acl" "statements" {
              bucket = aws_s3_bucket.statements.id
              acl    = "public-read"
            }
            resource "aws_s3_bucket" "exports" {
              bucket = "bank-exports"
            }
            resource "aws_s3_bucket_policy" "exports" {
              bucket = aws_s3_bucket.exports.id
              policy = jsonencode({
                Version   = "2012-10-17"
                Statement = [{ Effect = "Allow", Principal = "*", Action = "s3:GetObject", Resource = "arn:aws:s3:::bank-exports/*" }]
              })
            }
            resource "aws_iam_policy" "developer" {
              name   = "developer-everything"
              policy = jsonencode({
                Version   = "2012-10-17"
                Statement = [{ Effect = "Allow", Action = "*", Resource = "*" }]
              })
            }
            resource "aws_security_group" "web" {
              name = "web"
              ingress {
                from_port   = 22
                to_port     = 22
                protocol    = "tcp"
                cidr_blocks = ["0.0.0.0/0"]
              }
            }
            resource "aws_security_group" "database" {
              name = "database"
              ingress {
                from_port   = 5432
                to_port     = 5432
                protocol    = "tcp"
                cidr_blocks = ["0.0.0.0/0"]
              }
            }
            resource "aws_instance" "web" {
              ami           = "ami-03cf127a"
              instance_type = "t3.micro"
              metadata_options {
                http_tokens                 = "optional"
                http_put_response_hop_limit = 2
              }
            }
            resource "aws_ebs_volume" "data" {
              availability_zone = "eu-west-1a"
              size              = 8
              encrypted         = false
            }
            """;

    static String bucket(String name) {
        return """
                resource "aws_s3_bucket" "%1$s" {
                  bucket = "bank-%1$s"
                }
                resource "aws_s3_bucket_public_access_block" "%1$s" {
                  bucket                  = aws_s3_bucket.%1$s.id
                  block_public_acls       = true
                  block_public_policy     = true
                  ignore_public_acls      = true
                  restrict_public_buckets = true
                }
                resource "aws_s3_bucket_server_side_encryption_configuration" "%1$s" {
                  bucket = aws_s3_bucket.%1$s.id
                  rule {
                    apply_server_side_encryption_by_default {
                      sse_algorithm = "AES256"
                    }
                  }
                }
                resource "aws_s3_bucket_versioning" "%1$s" {
                  bucket = aws_s3_bucket.%1$s.id
                  versioning_configuration {
                    status = "Enabled"
                  }
                }
                """.formatted(name);
    }

    static final String FIXED = bucket("statements") + bucket("exports") + """
            resource "aws_iam_policy" "developer" {
              name   = "statements-reader"
              policy = jsonencode({
                Version   = "2012-10-17"
                Statement = [{ Effect = "Allow", Action = ["s3:GetObject"], Resource = "arn:aws:s3:::bank-statements/*" }]
              })
            }
            resource "aws_security_group" "web" {
              name        = "web"
              description = "web servers"
              ingress {
                description = "https from the load balancer"
                from_port   = 443
                to_port     = 443
                protocol    = "tcp"
                cidr_blocks = ["10.0.0.0/16"]
              }
            }
            resource "aws_security_group" "database" {
              name        = "database"
              description = "database"
              ingress {
                description = "postgres from the application"
                from_port   = 5432
                to_port     = 5432
                protocol    = "tcp"
                cidr_blocks = ["10.0.1.0/24"]
              }
            }
            resource "aws_instance" "web" {
              ami           = "ami-03cf127a"
              instance_type = "t3.micro"
              metadata_options {
                http_tokens                 = "required"
                http_put_response_hop_limit = 1
              }
              root_block_device {
                encrypted = true
              }
            }
            resource "aws_ebs_volume" "data" {
              availability_zone = "eu-west-1a"
              size              = 8
              encrypted         = true
            }
            """;

    public static Path write(Path dir, String text) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("main.tf"), text);
        return dir;
    }
}
