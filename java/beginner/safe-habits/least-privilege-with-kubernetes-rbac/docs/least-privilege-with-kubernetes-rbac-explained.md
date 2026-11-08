# Least Privilege with Kubernetes RBAC, Explained

## The idea in one sentence

RBAC allows only what a rule says, so the harm from a stolen token is the sum of its rules, and the common mistakes are cluster-admin, wildcards, and the verbs that let an identity grant itself more.

## The 5 acts

### 1. The nightly job that was given cluster-admin

The shop's namespace has 7 verbs (get, list, watch, create, update, patch, delete) on 8 resources (pods, pods/exec, secrets, configmaps, services, deployments, roles, rolebindings): 7 x 8 = 56 things a token could be used for.
Before the fix, the nightly-export-admin service account is bound to cluster-admin, and a stolen token reaches all 56, and the whole cluster as well. After the fix, the exporter has the role order-exporter with two rules: get and list on pods, and get on the one config map named export-settings.
A stolen exporter token reaches 2 of 56, plus that named config map. It can read export-settings, not payment-keys, and not secrets.

### 2. The wildcard trap: read everything

The reporter's role, report-reader, has one rule: get and list on *, which means every resource. Read everything sounds read-only and harmless.
But secrets are a resource too, so the reporter can read every password and key in the namespace. It cannot update a deployment. It reaches 16 of 56: 2 verbs x 8 resources.
And * also covers kinds of object added after the role was written. Name the resources instead, and use resourceNames for a single object.

### 3. Ways to become more powerful

The role deployer, held by the ci-deployer service account, has four rules: get, list and create on pods; get, list, create, update and patch on deployments; create on rolebindings; and bind and escalate on roles.
It cannot get secrets. But it has 4 escalation paths, ways to become more powerful than it was given: it can create pods, and a pod can mount any secret or token; it can create role bindings; it can bind roles it does not hold; and it can edit roles to add permissions it does not hold. The exporter has none.
So create pods is the same as read secrets, and bind or escalate is the same as owning the namespace.

### 4. A cluster-wide role through a namespaced binding

The role shop-viewer is a ClusterRole: it has no namespace, and its rule is get, list and watch on pods, services and configmaps. The support agent holds it through a RoleBinding in the shop namespace.
So the support agent can list pods in shop, but not in warehouse, and cannot delete pods in shop. By contrast, the nightly-export-admin is bound by a ClusterRoleBinding, and can list pods in warehouse: a cluster binding ignores namespaces.
The binding sets the scope, not the role: a role can be shared, and the binding is where the reach is decided.

### 5. A linter for the habits that cause incidents

The linter reads the manifests in infra/rbac/shop.yaml: 5 roles, 5 bindings and 6 service accounts. It finds 6 problems: a binding to cluster-admin, a role with wildcard resources, the escalation verbs escalate and bind, and two service accounts whose token is mounted into every pod.
A linter finds wildcards and cluster-admin; it does not find a role that is too wide but spelled out in full. So ask the cluster too, with kubectl auth can-i --list for each service account.

## The verdict

One service account per workload, bound in one namespace, with rules that name resources and, where possible, objects. Turn off token mounting where the pod does not call the API. Review any binding to cluster-admin and any use of create pods, bind, escalate or impersonate, and check with `kubectl auth can-i --list`.

## How to recognise this in code you did not write

- `cluster-admin` bindings in install guides.
- `resources: ["*"]` in Helm charts.
- `automountServiceAccountToken: false` in hardened manifests.
- Policy tools that fail a build on cluster-admin.

## Where you have already met this

- `cluster-admin` in tutorials, copied into production.
- `kubectl auth can-i` and `kubectl auth can-i --list`.
- Cloud kubernetes services that map cloud identities onto service accounts.
- Reports that begin "a pod's service account token was stolen".
