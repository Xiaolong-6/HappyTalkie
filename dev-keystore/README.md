# Development signing key

This directory contains a development-only PKCS#12 key encoded as Base64.

It exists so the phone and Wear OS APKs are signed by the same stable identity across local and GitHub Actions builds. Wear OS Data Layer requires matching package names and matching signatures.

Credentials:

- alias: happytalkie
- store password: happytalkie-dev
- key password: happytalkie-dev

Do not use this key for a production or Play Store release.
