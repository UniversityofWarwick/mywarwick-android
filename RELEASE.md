# Android release

1. Increment the `buildNumber` in `app/build.gradle` and create a release branch.
2. Create a pull request to master.
3. Once approved and merged download the Release artifact from Bamboo.
4. Go to the Google Play Console (`https://play.google.com/apps/publish`) and create a new Beta release.
5. Attach the Release APK and fill in the release notes (ask if unsure what to put here).
6. Release to Beta. This will start a Pre-launch report. Wait for that to finish.
7. Download the mapping files for the appropriate build from the Bamboo release artifacts and upload them to the PLay Console (Android vitals -> Deobfuscation files).
8. If there's nothing untoward on the report release the beta to production. The details will be copied over from the beta but you need to change the `Roll-out percentage` to 100% after the Review step.
9. Checkout the master branch and tag the release: `git tag -a 36` for example, then `git push --tags`.
10. Release the version in JIRA.