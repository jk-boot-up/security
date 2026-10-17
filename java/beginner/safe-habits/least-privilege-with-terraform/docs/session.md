# Session Guide — Least Privilege with Terraform and LocalStack

## Learning Objectives

By the end of the session you can:

- read an IAM policy and say what it allows
- explain why an explicit deny wins and a boundary grants nothing
- explain why a bucket and its contents are different resources
- name a policy shape that escalates privilege although it allows little

## Timetable

| Start | Topic | Time |
| --- | --- | --- |
| 0:00 | The problem and the analogy | 10 min |
| 0:10 | Act 1: Policies written as code | 7 min |
| 0:17 | Act 2: How a request is matched | 7 min |
| 0:24 | Act 3: A deny beats every allow | 7 min |
| 0:31 | Act 4: A permissions boundary | 7 min |
| 0:38 | Act 5: A linter, and the way to more power | 7 min |
| 0:45 | Exercises | 15 min |

## Walkthrough

Start with the key cabinet. Run the demo one act at a time. After act one, ask which policy the team would write on a busy day. After act two, ask what breaks when the slash-star is left off. In act five, ask who would review the deployer policy and what they would look for.

## Exercises

1. Add a policy for the support tool: read orders, no customers, and check its blast radius.
2. Write a policy that allows only s3:GetObject on shop-reports, and show it cannot list the bucket.
3. Add iam:CreatePolicyVersion to a policy and see what the linter says.
4. Change the boundary to allow writes to reports and see how the count moves.
