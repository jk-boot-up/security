# The bank's deployment policy. Everything is denied unless it passes: a missing field is a denial, not a pass.
# The same file is read by Conftest (the object is the input) and by an admission webhook (the object is in input.request.object).
package main

registry := "registry.bank.example/"

allowed_kinds := {"Deployment", "Service", "ConfigMap", "NetworkPolicy"}

object := input.request.object if input.request

object := input if not input.request

containers contains c if some c in object.spec.template.spec.containers

# R1: an allowed kind
deny contains msg if {
	not object.kind in allowed_kinds
	msg := sprintf("R1 kind %v is not allowed here", [object.kind])
}

# R2: the bank's registry
deny contains msg if {
	some c in containers
	not startswith(c.image, registry)
	msg := sprintf("R2 image %v: foreign registry", [c.image])
}

# R3: runAsNonRoot is true
deny contains msg if {
	some c in containers
	not c.securityContext.runAsNonRoot == true
	msg := sprintf("R3 container %v may run as root", [c.name])
}

# R4: not privileged
deny contains msg if {
	some c in containers
	c.securityContext.privileged == true
	msg := sprintf("R4 container %v is privileged", [c.name])
}

# R5: a cpu limit
deny contains msg if {
	some c in containers
	not c.resources.limits.cpu
	msg := sprintf("R5 container %v has no cpu limit", [c.name])
}

# R5: a memory limit
deny contains msg if {
	some c in containers
	not c.resources.limits.memory
	msg := sprintf("R5 container %v has no memory limit", [c.name])
}

# R6: an owner label
deny contains msg if {
	object.kind == "Deployment"
	not object.metadata.labels.owner
	msg := "R6 the Deployment has no owner label"
}

# R7: no host network
deny contains msg if {
	object.spec.template.spec.hostNetwork == true
	msg := "R7 the pod uses the host network"
}

# R8: no latest tag
deny contains msg if {
	some c in containers
	endswith(c.image, ":latest")
	msg := sprintf("R8 image %v uses the latest tag", [c.image])
}

# R9: not the default namespace
deny contains msg if {
	object.metadata.namespace == "default"
	msg := "R9 the default namespace is not used"
}

# R1: a kind at all
deny contains msg if {
	not object.kind
	msg := "R1 the object has no kind"
}

# R10: at least one container
deny contains msg if {
	object.kind == "Deployment"
	count(containers) == 0
	msg := "R10 a Deployment must declare containers"
}

allow if count(deny) == 0
