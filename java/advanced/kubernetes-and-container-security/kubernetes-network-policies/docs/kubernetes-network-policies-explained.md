# Kubernetes Network Policies, Explained

## The idea in one sentence

A network policy is not what you wrote, it is what the pods can reach.

## The 5 acts

### 1. A flat network

Five pods in the namespaces shop, pay and ops try every other pod: web, jobs, api, db and admin. All 20 pairs answer. The design lists three: web to api, api to db and admin to db. The other 17 are open and unwanted.

### 2. Default deny, then the design

A policy that selects every pod and lists no rule allows no traffic in. Then one allow for each line of the design, naming the namespace and the pod label of the caller. The measurement is 3 of 20, none unwanted and none missing: it equals the design.

### 3. Mistakes the API server accepts

An allow for the whole shop namespace lets the jobs pod reach the api: 1 unwanted pair. A typo in the app label of an allow selects nothing, so web cannot reach api: 1 missing pair. A deny written for the web only leaves the jobs pod with no policy at all: 4 unwanted pairs.

### 4. Egress and DNS

Before any egress rule the api reaches a server outside the cluster. A deny of egress in pay with one allow, api to db, keeps the design at 3 of 20, closes the outside server and also stops the lookup of the service name db until DNS is allowed.

### 5. The label is the credential

With the intended policies in place the jobs pod is relabelled app=web. The allow for web now applies to it: 1 unwanted pair. A policy trusts labels, so whoever can set labels chooses the policy.

## The verdict

Deny by default in every namespace, allow one conversation at a time naming the namespace and the pod, allow DNS when egress is denied, measure every pair against the design on every change, and control who can set labels.

## How to recognise this in code you did not write

- A cluster with no network policy at all.
- An allow that names a namespace and no pod.
- An egress deny that took DNS down.
- Pods labelled by hand to make something work.

## Where you have already met this

- A cluster where every pod can reach the database.
- A policy copied from a blog post and never tested.
- An egress rule that broke DNS on a Friday.
- A label changed to get a pod working.
