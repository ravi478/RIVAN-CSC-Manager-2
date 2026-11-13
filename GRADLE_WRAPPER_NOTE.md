# Gradle launcher note

This project includes Windows and Unix launchers pinned to Gradle 8.11.1.
They download the Gradle distribution into the user's Gradle cache on first
run and then execute the build.

This is a lightweight bootstrap launcher, not the stock Gradle Wrapper JAR
implementation. It requires network access on first run. GitHub Actions uses
the same launcher so local and CI build commands are consistent.
