# Third-party notices

Tanim Boss owns only original materials legally owned by Tanim Boss. The
proprietary project license does not replace any third-party license.

## Runtime components

| Component family | Upstream attribution | License |
| --- | --- | --- |
| AndroidX Activity, Compose, Material 3, Lifecycle, DataStore, and transitive AndroidX libraries | The Android Open Source Project / AndroidX authors | Apache-2.0 |
| OkHttp 5.3.2 (Live WebSocket/HTTPS), MockWebServer 5.3.2 (test only) | Square, Inc. and contributors | Apache-2.0 |
| Gson 2.15.0 | Google Inc. and contributors | Apache-2.0 |
| Okio (transitive DataStore/OkHttp dependency) | Copyright 2013 Square, Inc. and contributors | Apache-2.0 |
| Shaded Protocol Buffers (DataStore preferences) | Copyright 2008 Google Inc. and contributors | BSD-3-Clause (licenses/Protobuf-BSD-3-Clause.txt) |
| Kotlin standard library and Kotlin annotations | JetBrains s.r.o. and Kotlin contributors | Apache-2.0 |
| kotlinx.coroutines | JetBrains s.r.o. and contributors | Apache-2.0 |
| JSpecify annotations | The JSpecify Authors | Apache-2.0 |
| Guava ListenableFuture, Error Prone annotations | Google Inc. / Google LLC | Apache-2.0 |
| JetBrains annotations | JetBrains s.r.o. | Apache-2.0 |

The full Apache License 2.0 text is in licenses/Apache-2.0.txt. Upstream archive
license and notice resources remain with their corresponding artifacts; do not
strip notices when changing packaging. A machine-readable runtime dependency
report can be produced with `./gradlew :app:dependencies --configuration debugRuntimeClasspath`.

## Test-only components

JUnit 4.13.2 is distributed under EPL-1.0 (licenses/EPL-1.0.txt), copyright
JUnit contributors. Hamcrest 1.3 is distributed under the BSD 3-Clause license
(licenses/Hamcrest-BSD-3-Clause.txt), copyright 2000–2006 www.hamcrest.org.
AndroidX Test and Compose testing components use Apache-2.0. These dependencies
are for tests and are not intentionally included in the main application APK.

## Build tools

The generated Gradle wrapper scripts and wrapper JAR are third-party Gradle
materials under Apache-2.0, copyright Gradle contributors. The official wrapper
script copyright/license headers are retained unchanged. Gradle distributions
include their own third-party license inventory. Android Gradle Plugin and Kotlin
Gradle/Compose plugins retain their upstream licenses and are not original
Tanim Boss code. Java and Android SDK tools are installed separately, under their
vendor terms; installation does not transfer ownership or relicense them.

No legacy Jerry, Maya, Zoya, or Tech GPTx code or protected UI assets were used.
The Gemini wire client is original Kotlin code using the public Google API; no Google GenAI SDK code was copied. No wake-word or app-specific integration SDK is present. Review attribution whenever dependencies are added or changed.
