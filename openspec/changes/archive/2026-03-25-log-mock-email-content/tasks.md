## 1. Mock Mail Logging

- [x] 1.1 Update `MockMailSender` to emit application logs for each mock email while preserving the existing in-memory sent email record.
- [x] 1.2 Ensure the mock email log output includes recipient, subject, and body in a readable format that can be searched during local verification.

## 2. Verification

- [x] 2.1 Add or update automated tests to verify mock mail sending still records `sentEmails()` and also emits the expected log content.
- [x] 2.2 Run a mock-mode mail flow locally and confirm the application log shows the email recipient, subject, and body without affecting SMTP behavior.
