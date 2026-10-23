package bank.zerotrust_test

import data.bank.zerotrust

teller := {"user": "alice", "roles": ["teller"], "device_compliant": true, "hour": 10}
auditor := {"user": "bob", "roles": ["auditor"], "device_compliant": true, "hour": 10}
admin := {"user": "carol", "roles": ["admin"], "device_compliant": true, "hour": 10}

call(who, method, path) := object.union(who, {"method": method, "path": path})

test_teller_reads_a_balance if zerotrust.allow with input as call(teller, "GET", "/accounts/1001/balance")
test_teller_makes_a_transfer if zerotrust.allow with input as call(teller, "POST", "/transfers")
test_auditor_reads_the_audit if zerotrust.allow with input as call(auditor, "GET", "/audit/entries")
test_admin_freezes if zerotrust.allow with input as call(admin, "POST", "/admin/freeze")
test_auditor_cannot_transfer if not zerotrust.allow with input as call(auditor, "POST", "/transfers")
test_teller_cannot_read_the_audit if not zerotrust.allow with input as call(teller, "GET", "/audit/entries")
test_admin_is_not_a_teller if not zerotrust.allow with input as call(admin, "POST", "/transfers")
test_teller_cannot_freeze if not zerotrust.allow with input as call(teller, "POST", "/admin/freeze")
test_unmanaged_device_is_denied if not zerotrust.allow with input as call(object.union(teller, {"device_compliant": false}), "GET", "/accounts/1001/balance")
test_missing_posture_is_denied if not zerotrust.allow with input as call(object.remove(teller, ["device_compliant"]), "GET", "/accounts/1001/balance")
test_night_transfer_is_denied if not zerotrust.allow with input as call(object.union(teller, {"hour": 23}), "POST", "/transfers")
test_night_read_is_allowed if zerotrust.allow with input as call(object.union(teller, {"hour": 23}), "GET", "/accounts/1001/balance")
test_unknown_path_is_denied if not zerotrust.allow with input as call(admin, "GET", "/nope")
test_no_roles_is_denied if not zerotrust.allow with input as call(object.union(teller, {"roles": []}), "GET", "/accounts/1001/balance")
test_reasons_are_named if zerotrust.decision.reasons == ["device not compliant", "role not allowed"] with input as call(object.union(auditor, {"device_compliant": false}), "POST", "/transfers")
