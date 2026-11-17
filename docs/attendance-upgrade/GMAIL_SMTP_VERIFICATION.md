# Gmail / Google Workspace SMTP Verification Report

---

## 1. Transport Configuration

* **File Location**: [`CRM_TEST_Backend/src/utils/emailTransport.js`](file:///D:/Prodigit/CRM_TEST_Backend/src/utils/emailTransport.js)
* **Library**: Nodemailer (`v10.6.0`)
* **SMTP Settings**:
  ```env
  EMAIL_PROVIDER=smtp
  SMTP_HOST=smtp.gmail.com
  SMTP_PORT=587
  SMTP_SECURE=false
  SMTP_REQUIRE_TLS=true
  SMTP_USER=your_google_workspace_email@company.com
  SMTP_PASSWORD=your_google_app_password
  MAIL_FROM=Bodhi Attendance <notifications@company.com>
  MD_EMAIL=md@company.com
  ```

---

## 2. Security & Delivery Rules

1. **App Password Requirement**: Google Workspace accounts require an 16-character **App Password** created in Google Account Security.
2. **Password Secrecy**: Passwords are supplied exclusively via environment variables and never logged in console or diagnostic traces.
3. **Report Timings**: Dispatches 10:00 AM IST Morning Report and 8:30 PM IST Evening Report to `MD_EMAIL`. No individual event emails are sent.
