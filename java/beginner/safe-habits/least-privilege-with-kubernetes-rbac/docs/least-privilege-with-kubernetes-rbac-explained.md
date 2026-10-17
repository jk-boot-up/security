# Least Privilege with Kubernetes RBAC, Explained

## The idea in one sentence

RBAC allows only what a rule says, so the harm from a stolen token is the sum of its rules, and the common mistakes are cluster-admin, wildcards, and the verbs that let an identity grant itself more.

## The 5 acts

### 1. The nightly job that was given cluster-admin

The shop's namespace has 56 things a token could be used for: 7 verbs on 8 kinds of object. If the nightly-export-admin token is stolen it reaches all 56, and the whole cluster as well. If the exporter token is stolen it reaches 2 of 56: get and list pods.
It can also read one config map by name, export-settings, but not another one, payment-keys, and it cannot read secrets.

### 2. The wildcard trap: read everything

The reporter's role says get and list on all resources, which sounds read-only and harmless. But the reporter can read secrets, every password and key in the namespace. It reaches 16 of 56, and "everything" also includes things that did not exist when the role was written.
A rule that names its resources keeps meaning what it said.

### 3. Ways to become more powerful

The exporter has no escalation paths. The ci-deployer has four: it can create pods, which can mount any secret or service account token in the namespace, it can create role bindings, bind roles it does not hold, and edit roles to add permissions it does not hold.
It cannot read secrets directly, but it can create a pod that mounts them. So "create pods" is as powerful as "read secrets", and "bind" or "escalate" is as powerful as owning the namespace.

### 4. A cluster-wide role through a namespaced binding

The role shop-viewer is a ClusterRole, and the support agent holds it through a RoleBinding in the shop namespace. The agent can list pods in shop, cannot list pods in warehouse, and cannot delete pods in shop.
The cluster-admin job can list pods in warehouse because a cluster binding ignores namespaces. The binding decides the reach, not the role.

### 5. A linter for the habits that cause incidents

The linter finds six problems in the shop manifests: a binding to cluster-admin, a wildcard on resources, the escalate and bind verbs, and two service accounts whose token is mounted into every pod. A linter finds wildcards and cluster-admin, not a role that is too wide but spelled out in full,
so ask the cluster too, with kubectl auth can-i --list.

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
