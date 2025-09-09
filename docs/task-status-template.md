# 🛠 Task Status Template

> Timestamp: {{YYYY-MM-DD}}
> Owner: {{Name / Squad}}
> Objective: {{Concise, outcome-focused statement}}

---
## 🎯 Why This Matters
Brief business / architectural justification. What risk or gap are we closing? What value / narrative (portfolio, reliability, etc.) is improved?

---
## 🗺 High-Level Plan
1. {{Step 1}}
2. {{Step 2}}
3. {{Step 3}}
4. {{Stretch / Optional}}

---
## 🧱 Scope (In / Out)
| Area | In Scope | Deferred |
|------|----------|----------|
| Reliability | {{Yes}} | {{Out-of-scope items}} |
| Schema / Contracts | {{}} | {{}} |
| Observability | {{}} | {{}} |
| Domain Model | {{}} | {{}} |
| Resilience | {{}} | {{}} |
| Ops / Admin | {{}} | {{}} |

---
## 📦 Deliverables Status
List concrete artifacts (code units, docs, migrations). Use emoji legend below.
- 🟢 {{Example Deliverable Done}}
- 🟡 {{In Progress Deliverable}}
- 🔴 {{Not Started Deliverable}}

---
## ✅ Status Legend
| Emoji | Meaning |
|-------|---------|
| 🟢 | Done |
| 🟡 | In Progress / Partial |
| 🔴 | Not Started |
| 🧪 | Testing / Verifying |
| 📌 | Blocked / Decision Needed |

---
## 📋 Task Board
### 1. {{Category A}}
- 🔴 {{Task}}
- 🟡 {{Task}}

### 2. {{Category B}}
- 🔴 {{Task}}

### 3. {{Category C}}
- 🔴 {{Task}}

(Replicate categories as needed: Core Impl, Reliability, Domain, Tests, Metrics, Docs, Stretch)

---
## 🧪 Test Matrix
| Test Name | Category | Purpose | Status |
|-----------|----------|---------|--------|
| {{test_create}} | Integration | Ensures X | 🔴 |
| {{test_validation}} | Unit | Rejects invalid input | 🔴 |
| {{test_retry}} | Integration | Retry logic works | 🔴 |

---
## 🔐 Design Notes
Bullets summarizing patterns / constraints:
- Pattern: {{Transactional Outbox / Saga / CQRS}}
- Backoff: {{formula}}
- Serialization: {{JSON / Avro}}
- Idempotency: {{Key derivation}}

---
## ⚠ Risks & Mitigations
| Risk | Impact | Mitigation | Owner |
|------|--------|-----------|-------|
| {{Risk}} | {{High/Med/Low}} | {{Mitigation}} | {{}} |

---
## 📊 Metrics (Planned)
List metric names early for consistency.
- {{service.outbox.pending}}
- {{service.latency.ms}}
- {{domain.event.publish.attempt{status}}}

---
## 🧵 Trace & Correlation
How traceId / correlationId flows, any headers, propagation gaps.

---
## ⏭ Next Sprint Focus
1. {{Immediate next actionable}}
2. {{Secondary}}
3. {{Stretch}}

---
## 🔄 Progress Log
| Time | Action | Notes |
|------|--------|-------|
| {{YYYY-MM-DD}} | Created | Initial scaffold |
| {{YYYY-MM-DD}} | Update | {{Milestone}} |

---
## 📥 Dependencies / Libraries
- {{lib:name}} (purpose)

---
## 🧭 Acceptance Criteria
| Criterion | Status | Evidence |
|-----------|--------|----------|
| {{No afterCommit}} | 🔴 | {{Link / Test}} |
| {{Retry backoff}} | 🔴 | {{Test name}} |
| {{Contract validated}} | 🔴 | {{Schema test}} |

---
## 📝 Example Envelope (If Event Work)
```json
{
  "eventId": "...",
  "type": "...",
  "version": "v1",
  "occurredAt": "...",
  "aggregateType": "...",
  "aggregateId": "...",
  "traceId": "...",
  "data": {"...": "..."}
}
```

---
## 📎 Pending Decisions
| Topic | Needed By | Options | Chosen | Notes |
|-------|-----------|---------|--------|-------|
| {{Schema registry?}} | {{Date}} | {{A/B/C}} | {{?}} | {{}} |

---
## 📚 Appendix (Optional)
- Sequence Diagram: `docs/diagrams/{{name}}.puml`
- ADR References: `docs/adr/{{id}}-*.md`

---
Fill placeholders, prune unused sections, and keep concise. Keep this template versioned for consistency across services.

