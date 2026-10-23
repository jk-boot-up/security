package bank.zerotrust

# Every request to the ledger API is denied unless this policy says yes.
# Input: user, roles, device_compliant, method, path, hour (0-23, the proxy's clock).

default allow := false

# which role may call which operation
grants := {
	{"method": "GET", "resource": "balance", "role": "teller"},
	{"method": "GET", "resource": "balance", "role": "auditor"},
	{"method": "GET", "resource": "balance", "role": "admin"},
	{"method": "POST", "resource": "transfer", "role": "teller"},
	{"method": "GET", "resource": "audit", "role": "auditor"},
	{"method": "GET", "resource": "audit", "role": "admin"},
	{"method": "POST", "resource": "freeze", "role": "admin"},
}

resource := "balance" if {
	input.method == "GET"
	glob.match("/accounts/*/balance", ["/"], input.path)
}

resource := "transfer" if {
	input.method == "POST"
	input.path == "/transfers"
}

resource := "audit" if {
	input.method == "GET"
	input.path == "/audit/entries"
}

resource := "freeze" if {
	input.method == "POST"
	input.path == "/admin/freeze"
}

business_hours if {
	input.hour >= 8
	input.hour < 18
}

role_ok if {
	some role in input.roles
	{"method": input.method, "resource": resource, "role": role} in grants
}

reasons contains "unknown operation" if not resource

# a missing claim is not a compliant device: "!= true" would be undefined and let the request through
device_ok if input.device_compliant == true

reasons contains "device not compliant" if not device_ok

reasons contains "role not allowed" if {
	resource
	not role_ok
}

# money moves only in business hours
reasons contains "outside business hours" if {
	resource == "transfer"
	role_ok
	not business_hours
}

allow if count(reasons) == 0

decision := {"allow": allow, "reasons": sort(reasons)}
