# The first draft of two rules, written as lists of what is forbidden. A missing field makes the comparison undefined, so nothing is denied.
package naive

deny contains msg if {
	some c in input.spec.template.spec.containers
	c.securityContext.runAsNonRoot == false
	msg := "R3 container may run as root"
}

deny contains msg if {
	some c in input.spec.template.spec.containers
	c.resources.limits.memory == ""
	msg := "R5 no memory limit"
}
