# Release Readiness Checklist

Run this quick checklist before a release demo or stakeholder walkthrough.

## Pre-Release Environment Check

- [ ] Backend is running at `http://localhost:8080`
- [ ] Frontend is running at `http://localhost:3000`
- [ ] Cloud SQL proxy is running (if required for environment)
- [ ] Firebase/GCP credentials are valid for release environment
- [ ] Browser cache/session is clean or intentionally prepared

## Critical Flow Checks (Must Pass)

- [ ] App landing page loads without console-breaking errors
- [ ] Email/password login works
- [ ] Google login works (if enabled for release environment)
- [ ] Main page loads wall sections
- [ ] Wall section opens and problems display
- [ ] Problem detail page opens
- [ ] Comment can be posted by authenticated user
- [ ] Solution beta upload flow works (or known fallback explained)
- [ ] Account page loads current account info
- [ ] Admin account role change works (if admin flow is in release scope)

## Role-Specific Checks

- [ ] Guest can browse but cannot perform authenticated actions
- [ ] Setter role sees problem-management controls
- [ ] Admin role sees account-management and wall-section admin controls

## API/Health Checks

- [ ] `GET /api/health` returns healthy response
- [ ] `GET /api/v1/meta` returns app metadata

## Release Readiness

- [ ] Known limitations list is ready to explain
- [ ] Backup validation path prepared (in case upload/cloud dependency fails)
- [ ] Test accounts are available and credentials verified
- [ ] Presenter has role-switching plan for showing permission differences

## Sign-Off

- **Date:**
- **Tester:**
- **Result:** Pass / Fail / Conditional
- **Notes:**
