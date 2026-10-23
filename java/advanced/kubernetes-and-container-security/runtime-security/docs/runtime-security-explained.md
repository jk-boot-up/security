# Runtime Security, Explained

## The idea in one sentence

A detector only knows what you told it is normal.

## The 6 acts

### 1. Falco on this machine

Falco 0.45.1 is run as a container. falco --validate accepts the bank's 3 rules. Started with the modern eBPF driver it stops with BPF_TRACE_RAW_TP is not supported: the kernel of Docker Desktop's VM has no BTF. The demo then applies the same three tests to Linux audit records.

### 2. The service doing its work

The payments pod calls itself, writes to its log folder and calls the ledger service. The sensor sees all 3 kinds of syscall (exec, write, connect) and the rules raise 0 alerts: normal work is not an alert.

### 3. A shell in the container

Someone opens a shell in the payments pod with kubectl exec and types id -u. The service was started without a shell, so a shell is never normal: Shell in container, pod payments-api, sh.

### 4. A write where it never writes

A program is written to /usr/bin in the payments pod. The service writes only to its log folder and /tmp: Unexpected write in container, /usr/bin/miner. The log folder is written the same way and raises nothing.

### 5. A connection it never makes

The payments pod connects to a server outside the cluster on port 4444. The service calls only the ledger and the cluster DNS: Unexpected outbound connection to the outside server. The ledger call raised nothing.

### 6. Prevent and detect

The same write in a pod with a read-only root file system is refused by the kernel. The sensor still sees the attempt: Unexpected write in container, /usr/bin/ (refused). Prevention and detection work together.

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
