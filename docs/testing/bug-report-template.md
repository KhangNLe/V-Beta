# Bug Report Template

Use this template for consistent bug tracking across frontend, backend, and integration issues.

## Bug Title

Short, specific summary.

Example:

- `Setter cannot reset wall section after role promotion`

## Metadata

- **Reporter:**
- **Date:**
- **Environment:** (local / shared dev / staging)
- **Area:** (auth, account, wall/problems, discussion, API, infra, UI)
- **Severity:** (Critical / High / Medium / Low)
- **Priority:** (P0 / P1 / P2 / P3)

## Preconditions

Describe required state before reproduction:

- account role (guest/climber/setter/admin)
- test data assumptions
- route/page starting point
- service status (frontend/backend/proxy/cloud)

## Steps to Reproduce

1. ...
2. ...
3. ...

## Expected Result

What should happen.

## Actual Result

What actually happened.

## Evidence

- Screenshots/video:
- Console logs:
- Network/API response (status + payload):
- Backend logs/stack trace:

## Impact

- Who is affected (all users / specific role)
- Functional impact (blocked flow, incorrect data, security risk, etc.)

## Suspected Scope (Optional)

- likely component/module
- likely endpoint/service

## Workaround (Optional)

Temporary way to proceed, if available.

## Fix Verification Checklist

- [ ] Reproduced bug before fix
- [ ] Verified fix in same environment
- [ ] Verified no regression in related flow
- [ ] Added/updated relevant test case in `docs/testing/manual-test-cases.md`
