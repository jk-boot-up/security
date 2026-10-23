# Runs inside a privileged container that shares the kernel's process view (Docker Desktop's Linux VM).
# It switches on Linux audit for three kinds of syscall and copies the records that belong to a
# Kubernetes container (found by the container's cgroup) to /events.log.
apk add -q audit
auditctl -D >/dev/null 2>&1
auditctl -b 8192 >/dev/null
auditctl -e 1 >/dev/null
auditctl -a always,exit -F arch=b64 -S execve -k exec
auditctl -a always,exit -F arch=b64 -S openat -F "a2&1" -k write
auditctl -a always,exit -F arch=b64 -S openat -F "a2&2" -k write
auditctl -a always,exit -F arch=b64 -S openat -F "a2&64" -k write
auditctl -a always,exit -F arch=b64 -S connect -k connect
mkdir -p /var/log/audit
: > /var/log/audit/audit.log
: > /events.log
auditd -n >/dev/null 2>&1 &
sleep 1
echo ready > /ready
# Only shell builtins below: a program started for each record would itself be audited, and so on without end.
exec 3>> /events.log 2>/dev/null
tail -n0 -F /var/log/audit/audit.log | while IFS= read -r line; do
  case "$line" in
    *"type=SYSCALL"*)
      pid=${line#* pid=}; pid=${pid%% *}
      ppid=${line#* ppid=}; ppid=${ppid%% *}
      cg=""
      read -r cg < /proc/$pid/cgroup || read -r cg < /proc/$ppid/cgroup
      case "$cg" in
        *cri-containerd-*) echo "$line CG=$cg" >&3 ;;
      esac ;;
    # The other records of an event can be written before or after its SYSCALL record, so they are all copied.
    *"type=EXECVE"*|*"type=PATH"*|*"type=SOCKADDR"*|*"type=CWD"*) echo "$line" >&3 ;;
  esac
done
