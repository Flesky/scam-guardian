# Agent rules

- Kotlin and Jetpack Compose only.
- No INTERNET permission. All inference runs on-device with LiteRT-LM.
- Model inference runs on one dedicated background thread.
- The `Embedder` implementation is the only code that uses LiteRT-LM.
- Business logic goes in `ph.scamguardian.core`, with no Android imports.
- Read the relevant skill in `.agents/skills/` before Android or Compose work.
- Use the official docs for LiteRT-LM APIs.
- Pin exact dependency versions in the version catalog.
- Ask before adding any dependency.
- Never use `git commit --no-verify`.
- One feature per task. Every core feature has a unit test.
- Done = `spotlessCheck`, `detekt`, `lintDebug` and `testDebugUnitTest` all pass.
