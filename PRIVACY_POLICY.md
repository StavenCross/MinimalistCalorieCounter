# Privacy Policy for Minimalist Calorie Counter

Minimalist Calorie Counter is built with a local-first privacy model. The app does not operate a server, does not sell data, and does not share health data with advertisers, data brokers, or analytics providers.

## Health Connect Integration

Health Connect integration is optional and controlled by Android Health Connect permissions.

If you enable Health Connect permissions, the app may write Nutrition records and may read data you authorize, including nutrition, weight, height, body fat, lean body mass, and other readable Health Connect record types used for export.

The app uses Health Connect data locally to:

- write meal nutrition records you explicitly add or import;
- display Meals history for selected dates;
- avoid duplicate app-owned meal imports;
- refresh unlocked Goals fields from body metrics;
- delete app-owned Nutrition records after an explicit preview and confirmation;
- export CSV files that you request.

The app follows the Health Connect Permissions Policy, including Limited Use requirements.

## Local Storage

The app stores meal backups, goals, preferences, Health Connect write outbox rows, and import/export job history on the device. Current builds use Room and may also mirror selected state to legacy CSV files during the migration window.

Android backup and device-transfer behavior depends on your device and Google settings. When enabled by Android, app settings and local database files may be included in platform-managed backup or restore.

## Exports

CSV exports are created only when you request them. Exports are written locally to the device Downloads folder.

The app supports redacted export by default for check-ins. Redacted exports omit Health Connect record ids, client record ids, package names, recording method, last-modified time, and raw record text while keeping the nutrition and goal values needed for review.

Raw or full Health Connect export can include sensitive health data and should be shared only with services or people you trust.

## Debug Builds

Debug builds include a localhost-only automation bridge for development and testing. This bridge is not part of release builds and is intended to be accessed through adb port forwarding on a development machine.

Contact: message.makstuff@outlook.com
