# Rain Alert Android Project Guide

## Build Commands
- Build project: `./gradlew build`
- Clean build: `./gradlew clean build`
- Install debug APK: `./gradlew installDebug`
- Run unit tests: `./gradlew test`
- Run single unit test: `./gradlew test --tests "com.stoneCode.rain_alert.ExampleUnitTest.addition_isCorrect"`
- Run instrumented tests: `./gradlew connectedAndroidTest`
- Run lint checks: `./gradlew lint`

## Code Style Guidelines
- Use Kotlin naming conventions: classes in PascalCase, functions/variables in camelCase
- Package structure: `com.stoneCode.rain_alert.[feature]`
- Organize imports by type (Android/androidx first, then project-specific)
- Use 4-space indentation and line wrapping for readability
- Follow MVVM architecture pattern with clear separation of concerns
- Use extension functions for reusable functionality
- Handle errors with appropriate logging and user feedback (Toast/Snackbar)
- Utilize Kotlin's null safety features consistently (safe calls, elvis operator)
- Document complex logic with comments
- Keep functions small and focused on single responsibility