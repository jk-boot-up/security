# Kubernetes Network Policies, Explained

## The idea in one sentence

A network policy is not what you wrote, it is what the pods can reach.

## The 5 acts

### 1. A flat network

The input: five pods in the namespaces shop, pay and ops (web, jobs, api, db and admin, each with an app label), the design's three pairs (web to api, api to db, admin to db) and no policy.
The result: curl inside each pod against every other pod answers on all 20 pairs. Five pods make 5 x 4 = 20 ordered pairs. 20 reachable = 3 wanted + 17 unwanted; nothing is missing.

### 2. Default deny, then the design

Step 1, the input: deny-all-ingress in shop, pay and ops. Its podSelector {} selects every pod of the namespace, and it lists no ingress rule, so nothing may come in. The result: 0 of 20 reachable, and the design's 3 pairs are missing.
Step 2, the input: allow-api-from-shop-web, allow-db-from-pay-api and allow-db-from-ops-admin. The result: 3 of 20 reachable = 3 wanted + 0 unwanted, nothing missing: the measurement equals the design.
One allow, line by line: the podSelector under spec picks the pods it protects (app=api in pay); the from element names the namespace shop and the label app=web, both in one element, so a caller needs both; ports allows only 8080.

### 3. Mistakes the API server accepts

Each mistake replaces one of the design's policies; the API server answers created every time.
Mistake 1: allow-api-from-shop has a namespace selector and no pod selector, so any pod of shop may call the api. The result: 4 of 20 = 3 wanted + 1 unwanted, shop/jobs -> pay/api.
Mistake 2: allow-apii-from-shop-web selects app=apii, a label no pod has: it selects no pod, so the api stays denied. The result: 2 of 20 = 2 wanted + 0 unwanted, 1 missing, shop/web -> pay/api.
Mistake 3: deny-ingress-web replaces the deny for all of shop and selects only web, so jobs has no policy at all. The result: 7 of 20 = 3 wanted + 4 unwanted: every other pod reaches jobs.

### 4. Egress and DNS

Before, with the design's ingress policies and nothing on egress: the api reaches a server outside the cluster, and reaches db by its service name.
The input: deny-all-egress in pay and allow-api-to-db. The result: the 20 pairs still give 3 of 20 = 3 wanted; the outside server times out; db by address answers; db by name times out.
Why: the name db.pay.svc.cluster.local is first asked of kube-dns (2 pods in kube-system, port 53), and no egress allow names them. After allow-dns, db by name answers and the outside server still times out.

### 5. The label is the credential

Before: the design's policies; shop/jobs has app=jobs, and 3 of 20 are reachable, equal to the design.
The change: kubectl -n shop label pod jobs app=web --overwrite, by anyone who may edit pods in shop.
The result: shop/jobs has app=web, and 4 of 20 = 3 wanted + 1 unwanted: shop/jobs -> pay/api.
Why: allow-api-from-shop-web asks two questions of a caller: is its namespace shop, and does it have app=web. Jobs now answers yes to both.

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
