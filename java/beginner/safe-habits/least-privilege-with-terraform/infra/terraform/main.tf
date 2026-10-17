# The least-privilege lesson for an online store, written as infrastructure code.
# Every policy below is an IAM policy document: the JSON it produces is what the Java evaluator reads.

terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.69.1"
    }
  }
}

variable "endpoint" {
  description = "The address of the local AWS emulator, for example http://127.0.0.1:4566"
  type        = string
}

provider "aws" {
  region                      = "eu-west-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    iam = var.endpoint
    sts = var.endpoint
  }
}

# 1. The shortcut: everything, everywhere.
data "aws_iam_policy_document" "wildcard" {
  statement {
    sid       = "EverythingEverywhere"
    effect    = "Allow"
    actions   = ["*"]
    resources = ["*"]
  }
}

# 2. The nightly sales export: read the reports bucket and query the orders table, nothing else.
data "aws_iam_policy_document" "exporter" {
  statement {
    sid       = "ListReports"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::shop-reports"]
  }
  statement {
    sid       = "ReadReports"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["arn:aws:s3:::shop-reports/*"]
  }
  statement {
    sid       = "QueryOrders"
    effect    = "Allow"
    actions   = ["dynamodb:Query"]
    resources = ["arn:aws:dynamodb:eu-west-1:000000000000:table/orders"]
  }
}

# 3. Broad allow with an explicit deny on the secrets: the deny wins over any allow.
data "aws_iam_policy_document" "admin_with_deny" {
  statement {
    sid       = "AllowEverything"
    effect    = "Allow"
    actions   = ["*"]
    resources = ["*"]
  }
  statement {
    sid       = "NeverTouchSecrets"
    effect    = "Deny"
    actions   = ["s3:*", "kms:*"]
    resources = ["arn:aws:s3:::shop-secrets", "arn:aws:s3:::shop-secrets/*", "arn:aws:kms:eu-west-1:000000000000:key/*"]
  }
}

# 4. The permissions boundary: the most any identity wearing it can ever do.
data "aws_iam_policy_document" "boundary" {
  statement {
    sid       = "ReadOnlyShopData"
    effect    = "Allow"
    actions   = ["s3:Get*", "s3:List*", "dynamodb:GetItem", "dynamodb:Query", "dynamodb:Scan"]
    resources = ["*"]
  }
  statement {
    sid       = "NoSecrets"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = ["arn:aws:s3:::shop-secrets", "arn:aws:s3:::shop-secrets/*"]
  }
}

# 5. A deploy role that looks harmless and is not: it can pass any role to a new function.
data "aws_iam_policy_document" "deployer" {
  statement {
    sid       = "DeployFunctions"
    effect    = "Allow"
    actions   = ["lambda:CreateFunction", "lambda:InvokeFunction"]
    resources = ["arn:aws:lambda:eu-west-1:000000000000:function:checkout"]
  }
  statement {
    sid       = "PassAnyRole"
    effect    = "Allow"
    actions   = ["iam:PassRole"]
    resources = ["*"]
  }
}

locals {
  documents = {
    wildcard        = data.aws_iam_policy_document.wildcard.json
    exporter        = data.aws_iam_policy_document.exporter.json
    admin_with_deny = data.aws_iam_policy_document.admin_with_deny.json
    boundary        = data.aws_iam_policy_document.boundary.json
    deployer        = data.aws_iam_policy_document.deployer.json
  }
}

resource "aws_iam_policy" "policy" {
  for_each = local.documents
  name     = "shop-${replace(each.key, "_", "-")}"
  policy   = each.value
}

# A user wearing the wildcard policy, held in by the boundary.
resource "aws_iam_user" "bounded_wildcard" {
  name                 = "bounded-wildcard"
  permissions_boundary = aws_iam_policy.policy["boundary"].arn
}

resource "aws_iam_user_policy_attachment" "bounded_wildcard" {
  user       = aws_iam_user.bounded_wildcard.name
  policy_arn = aws_iam_policy.policy["wildcard"].arn
}

output "documents" {
  description = "The JSON of every policy document, by name"
  value       = local.documents
}
