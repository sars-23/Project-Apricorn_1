# Judge Engine Sandbox Threat Model & Security Specification

## Threat Model & Implemented Mitigations

| Threat | Risk Level | Mitigation Strategy | Implemented Mechanism |
|---|---|---|---|
| **Arbitrary Host Execution** | CRITICAL | Untrusted code must never execute on application host OS or inside Spring Boot JVM. | Code runs exclusively inside an isolated Docker container. |
| **Network Probing / Exfiltration** | CRITICAL | Malicious code attempting sockets, HTTP requests, or external connections. | `--network none` flag disables all network interfaces. |
| **Host Filesystem Access** | HIGH | Code attempting to read `/etc/passwd`, environment variables, or host files. | Host volume mount is strictly scoped to single submission directory (`/app`), with no Docker socket access. |
| **Resource Exhaustion (CPU)** | HIGH | Infinite loops (`while(true)`) attempting to lock CPU cores. | `--cpus 0.5` limit + hard execution process timeout (`timeLimitMs`). Container killed forcibly. |
| **Resource Exhaustion (Memory)** | HIGH | Allocation of gigabyte arrays or heap memory. | `--memory 256m` and `--memory-swap 256m` limits. |
| **Fork Bombs / Process Exhaustion** | HIGH | Recursive `Runtime.getRuntime().exec` or thread creation. | `--pids-limit 64` prevents process multiplication. |
| **Privilege Escalation** | HIGH | Container escape or root capability abuse. | `--cap-drop ALL`, `--security-opt no-new-privileges`, non-privileged execution. |
| **Output Flooding** | MEDIUM | Infinite stdout printing filling disk or memory buffers. | Bounded output stream reader caps stdout/stderr at 1 MB max. |
| **Hidden Test Case Leakage** | HIGH | Leaking secret judge test data in stdout/stderr. | Diagnostic output sanitized; test inputs/expected outputs never included in public errors or logs. |

## Defense in Depth & Future Hardening Recommendations

While Docker containers provide effective isolation for typical competitive programming submissions, container escapes remain a theoretical risk in multi-tenant cloud environments.

Future hardening options for production scale:
1. **gVisor (runsc)**: Lightweight user-space kernel sandbox intercepting Linux system calls.
2. **Firecracker MicroVMs**: Hardware-assisted lightweight virtual machines offering hypervisor-level isolation per submission.
3. **Isolated Worker Fleet**: Running judge engine execution on dedicated, ephemeral worker VMs isolated from control-plane Spring Boot backend servers.
