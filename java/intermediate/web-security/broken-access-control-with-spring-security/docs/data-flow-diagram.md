# Broken Access Control with Spring Security — Data Flow Diagram

Rules are tried in order; a request that matches none of them reaches denyAll().

![How the guarded filter chain decides](images/data-flow-diagram.png)


