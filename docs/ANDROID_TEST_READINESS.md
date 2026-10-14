# Android test readiness and remaining API dependencies

Changes on android-ui-development:

- App Lock uses the Android phone screen lock, not a separately stored four-digit app PIN. Enable/disable requires system credential authentication. UI relocks after the authenticated application stops; attendance monitoring runs independently. Enabled app lock also hides the app window from screenshots/recents. Biometric preference uses an enrolled strong biometric with explicit phone-PIN fallback. These need device tests on supported Android versions.
- Request forms display CRM submission progress/result and prevent repeat taps. Unsupported attachment selection is disabled; no upload or success is invented. Password reset explains the CRM workflow rather than having a dead click target.
- GPS evidence retains CRM's accuracy limit. Outside readings with matching office Wi-Fi, missing Wi-Fi evidence, or GPS uncertainty overlapping the radius produce no event. Uncertainty cancels unconfirmed absence grace. Departure requires successive fresh outside observations, confirmed required-Wi-Fi mismatch/disconnection, and CRM grace timing. During absence verification, bounded fixes are requested every 45 seconds; the 90-second observation freshness guard is a sensor safety constraint, not an office attendance policy. These observations do not mathematically prove continuous physical absence.
- Confirmed BREAK_START uses the observed absence-grace start as its effective monotonic time, while the confirmation time is kept separately. It is not backdated to an unobserved doorway crossing. Server acceptance of this timestamp must be exercised against the deployed test API.
- Checkout classification is deferred after an observation gap/restart until enough new outside evidence covers the configured maximum absence interval. This may delay classification rather than assuming absence during an unobserved period.
- Release builds require an explicit HTTPS testing API URL. The old production CRM URL is no longer a silent release default.

## UI audit

| Pages | Source / behavior | Remaining dependency |
| --- | --- | --- |
| Splash/Launch/Login/Onboarding | Existing authenticated startup and device flow | Hosted test auth/device/refresh/bootstrap APIs |
| Dashboard/Attendance History/Details | Accepted CRM events; actual generic break timeline | Deployed mobile attendance read API; latest 90 recorded days only |
| Requests/History/Details/Notifications | CRM text request POST + review decisions; visible result states | Migration 015 and mobile request API; attachments pending; approval does not automatically regularize attendance |
| Profile/Registered Device | CRM profile and existing device diagnostic | Department/shift data not supplied by current API |
| Offline Data/Sync/Event Log | User/device-filtered Room records | Offline evaluation still needs validated cached-policy and durable office binding work |
| Permissions/Battery/System Health/Configuration | Android diagnostics and CRM office flags | Office presence readiness is not inferred from hardware being enabled |
| App Lock/Biometrics | Android credential/biometric prompts | Physical-device lifecycle/authentication tests |
| On Duty | Explicit unavailable state; navigation only for actual eligible assignments | Real CRM assignment API/model still missing |
| Help/About | Installed version and honest behavior descriptions | CRM helpdesk contact not supplied |

No phone/CRM records were cleared or repaired. Existing false-break UUIDs should stay available for audit; correcting future detection does not repair historical totals automatically.

## Build locally before the real-time test

Run in the Windows Android checkout after pulling:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

When the hosted TEST API URL is available:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease -PbodhiApiBaseUrl=https://YOUR_TEST_BACKEND/
```

Replace the URL placeholder. Sign the release APK with your own keystore using Android Studio's Generate Signed Bundle / APK flow. Keep signing credentials local. A debug APK can also use the hosted URL for the first diagnostic run with the same Gradle property; USB is not required for its network calls.

This environment cannot download the Gradle 9.6.0 distribution, so Android build/unit tests did not execute and no APK was generated. Source checks alone are insufficient for release readiness. The release test remains blocked on the hosted test URL, deployed API/migration, signing configuration and successful local build.

## Required real-device checks

1. Remain stationary indoors with office Wi-Fi: no false breaks.
2. Open/close app, rotate screen, background it, cancel and complete PIN/biometric prompts: never bypass lock; monitoring continues.
3. Leave physically, including near-boundary and office Wi-Fi still reachable cases: inspect distance/accuracy/Wi-Fi diagnostic logs and verify true departure grace.
4. Return before grace: no break; return after confirmed break: one BREAK_END.
5. Confirm Room UUID/status matches CRM accepted event and trusted effective time.
6. Disable network, restart app/service and restore network: inspect known offline/binding limitations rather than assuming no missed event.
