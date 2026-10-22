package main_test

import data.main

good := {
	"kind": "Deployment",
	"metadata": {"name": "payments", "namespace": "bank", "labels": {"owner": "payments-team"}},
	"spec": {"template": {"spec": {"containers": [{
		"name": "payments",
		"image": "registry.bank.example/payments:2.3.1",
		"securityContext": {"runAsNonRoot": true},
		"resources": {"limits": {"cpu": "500m", "memory": "256Mi"}},
	}]}}},
}

with_container(field, value) := object.union(good, {"spec": {"template": {"spec": {"containers": [object.union(good.spec.template.spec.containers[0], {field: value})]}}}})

has_rule(prefix, o) if {
	some msg in main.deny with input as o
	startswith(msg, prefix)
}

test_good_is_allowed if main.allow with input as good

test_r1_unknown_kind_denied if has_rule("R1", object.union(good, {"kind": "ClusterRoleBinding"}))

test_r2_foreign_registry_denied if has_rule("R2", with_container("image", "docker.io/library/nginx:1.27"))

test_r3_missing_security_context_denied if has_rule("R3", json.remove(with_container("name", "payments"), ["spec/template/spec/containers/0/securityContext"]))

test_r3_root_denied if has_rule("R3", with_container("securityContext", {"runAsNonRoot": false}))

test_r4_privileged_denied if has_rule("R4", with_container("securityContext", {"runAsNonRoot": true, "privileged": true}))

test_r5_no_limits_denied if has_rule("R5", json.remove(good, ["spec/template/spec/containers/0/resources"]))

test_r6_no_owner_denied if has_rule("R6", json.remove(good, ["metadata/labels"]))

test_r7_host_network_denied if has_rule("R7", object.union(good, {"spec": {"template": {"spec": {"hostNetwork": true}}}}))

test_r8_latest_denied if has_rule("R8", with_container("image", "registry.bank.example/payments:latest"))

test_r9_default_namespace_denied if has_rule("R9", object.union(good, {"metadata": {"name": "payments", "namespace": "default", "labels": {"owner": "x"}}}))

test_admission_review_is_read if main.allow with input as {"request": {"object": good}}

test_admission_review_denies if not main.allow with input as {"request": {"object": object.union(good, {"kind": "Secret"})}}

test_r5_missing_cpu_only_denied if {
	o := json.remove(good, ["spec/template/spec/containers/0/resources/limits/cpu"])
	has_rule("R5", o)
	some msg in main.deny with input as o
	contains(msg, "cpu")
}

test_r5_missing_memory_only_denied if {
	o := json.remove(good, ["spec/template/spec/containers/0/resources/limits/memory"])
	some msg in main.deny with input as o
	contains(msg, "memory")
}

test_r1_no_kind_denied if has_rule("R1", {})

test_r10_no_containers_denied if has_rule("R10", json.remove(good, ["spec/template/spec/containers"]))
