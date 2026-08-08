# Tasf.B2B — Baggage Routing & Planning System

Full-stack logistics optimization system built for **Tasf.B2B**, a fictional B2B 
air-cargo carrier that transfers baggage between airports across the Americas, 
Europe, and Asia. Developed as the term project for the Software Design & 
Development course (1INF54) at PUCP, following the NTP-ISO/IEC 29110-5-1-2 
(VSE) process standard.

## Problem

Tasf.B2B commits to strict delivery windows (1 day same-continent, 2 days 
cross-continent) across a network with limited flight and warehouse capacity, 
and needs to react to flight cancellations without breaking those commitments. 
The system must solve three operating scenarios: real-time day-to-day 
operations, a compressed period simulation (3/5/7 days run in 30–90 minutes), 
and a stress simulation that runs until the network collapses.

## Architecture

- **Frontend** — React + Vite, dark control-center UI, interactive airport map, 
  traffic-light indicators (green ≤60%, amber 61–85%, red >85%), KPI dashboard, 
  real-time updates via WebSocket.
- **Backend** — Java Spring Boot, exposing the planning engine as a REST/WebSocket 
  service.
- **Planning engine** — Two competing metaheuristics (Ant Colony Optimization 
  and Tabu Search) implemented and benchmarked via numerical experimentation 
  (15 runs/algorithm, Shapiro-Wilk → Fisher → Mann-Whitney). **Tabu Search was 
  selected** as the sole production algorithm: ~185× faster with statistically 
  equivalent deadline compliance.
- **Database** — PostgreSQL, AWS RDS in production, local instance for 
  development (Spring profile separation).
- **Infrastructure** — Deployed on a PUCP VM behind Nginx; backend run as a 
  systemd-managed service (in progress) for persistent env vars and 
  auto-restart on reboot.

## Repository structure
```
dp1-app/
├── frontend/ # React + Vite + Zustand
└── backend/ # Spring Boot, Maven, Java 21
```

## Status

Active development — deployed to a staging environment on PUCP VM. See 
`/docs` for the full requirements set (Definición del Producto, Lista de 
Exigencias, Informe de Selección de Algoritmos, Comparación de Productos) and 
open issues for pending items (deploy automation, backend service migration).
