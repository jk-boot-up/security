# Runtime Security, Explained

## The idea in one sentence

A detector only knows what you told it is normal.

## The 5 acts

### 1. Falco on this machine

The input: rules/bank-runtime-rules.yaml with three rules (a shell, an unexpected write, an unexpected outbound connection), and Falco 0.45.1 run as a container.
The result: falco --validate accepts the rules. The kernel of Docker Desktop's virtual machine has no BTF, the type information the modern eBPF driver needs, and the driver stops with BPF_TRACE_RAW_TP is not supported.
So the demo maps each rule to one kind of Linux audit record: exec (a program started), write (a file opened for writing) and connect (a connection made).

### 2. Normal work, no alert

The input: the baseline (writes to /var/log/payments/, /tmp/ and /dev/; connects to 2 addresses, the ledger-db service and the cluster DNS; no shell) and three commands in payments-api: wget to itself, tee to its log, nc to the ledger on port 5432.
The result: the sensor saw exec, write and connect from the pod, and no alert.
How one record is read: the connect record's socket address starts 0200 1538. 02 00 read low byte first is family 2, IPv4; 15 38 read high byte first is 0x1538 = 5432, the ledger's port. The process is nc, in a container whose id the cgroup path gives, which kubectl maps to payments-api. The ledger-db address is on the baseline, so no alert.

### 3. A shell in the container

The input: kubectl exec -i payments-api -- sh, then id -u. The shell answers 0, the root user.
The result: one alert: rule Shell in container, pod payments-api, detail sh.
Why: the exec record names the process sh, sh is in the shell list (ash, bash, dash, sh, zsh), and the service was started as httpd, never by a shell, so any shell is an alert.

### 4. A write where it never writes

The input: tee /usr/bin/miner in payments-api. The result: one alert, Unexpected write in container, /usr/bin/miner, because the path starts with none of /var/log/payments/, /tmp/ and /dev/.
The same input in payments-api-ro, whose root file system is read-only: the kernel refuses it (Read-only file system). The result: one alert all the same, for /usr/bin/ (refused): a refused open names only the folder the file was to be made in.

### 5. A connection it never makes

The input: nc to a server outside the cluster, port 4444, from payments-api.
The result: one alert, Unexpected outbound connection, the outside server:4444.
How the record is read: the socket address starts 0200 115C: family 2, IPv4, and 0x115C = 4444. The 4 address bytes are not one of the 2 addresses on the baseline (the ledger-db service and the DNS).

## The verdict

Start the service without a shell, make the root file system read-only, deny the network by default, and write a rule for each thing the service must never do, with the baseline in the same file and an owner for each alert.

## How to recognise this in code you did not write

- A container image with a shell and curl in production.
- A rule with no baseline that fires all day.
- A cluster with detection and nobody on call.
- A driver that was never tested on the kernel that runs it.

## Where you have already met this

- A container that ran a miner for a month.
- A rule that fired a hundred times a day and was muted.
- A debug shell left in production.
- A log line nobody read.
