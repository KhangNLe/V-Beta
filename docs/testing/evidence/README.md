# Testing Evidence

Store committed test evidence files here so reviewers can verify results without requiring local `target/` build artifacts.

## Suggested Structure

- `backend/`
  - `mvn-test-summary.png`
  - `surefire-html-summary.png`
- `frontend/`
  - `npm-test-summary.png`
  - `lint-summary.png`

## Notes

- Keep screenshots legible and include date/time in terminal where possible.
- Add one short caption in the related report markdown pointing to each evidence file.
