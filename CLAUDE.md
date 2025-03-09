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

## Code Modification Guidelines for Claude

When modifying code files, please follow these practices to ensure efficient operations:

### File Navigation
1. Always use `read_file` before attempting to modify a file
2. Note exact line numbers of interest when reading a file
3. Check the directory structure with `directory_tree` at the start of a session to understand project organization

### Code Editing
1. For small changes, use targeted `edit_file` with minimal context (a few lines before and after the change)
2. For larger changes, consider using `write_file` but be cautious about completely rewriting files
3. When using string replacement, use unique anchoring text (like function signatures or distinctive comments)
4. Be careful with whitespace-sensitive replacements - use line numbers when possible

### File References
1. Use absolute paths when referencing files to avoid confusion
2. When searching for files, use specific patterns with `search_files`
3. Use `read_multiple_files` for efficiency when examining related files

### Error Handling
1. If an update fails, try a more focused approach with smaller text blocks
2. Use command-line tools like `grep` or `sed` only as a last resort
3. If repeated failures occur, consider breaking the task into smaller steps

### Additional Best Practices
1. Add reference markers in key code sections as anchors for easier modification (e.g., `// CLAUDE-MARKER: WeatherMapScreen parameters`)
2. Break down complex tasks into separate, simpler modifications
3. When requesting changes, specify exactly which part needs to be changed with clear references
4. Include relevant code snippets in prompts to help provide context
5. Always read files before attempting edits and read the directory tree when starting a new chat

"Code Modification Best Practices for AI Tools
When Editing Files with AI Assistants

Use Line Numbers When Possible

Specify exact line numbers for insertions/modifications
Example: Insert at line 45: class RateLimitException(Exception):

Provide Sufficient Context

Include 3-5 lines before and after your target section
Example: Find the section after import statements and before class definitions

Handle Line Ending Differences

Windows files use CRLF (\r\n) while Unix uses LF (\n)
Specify if files have specific line endings when providing context

Multiple Approaches

Provide both targeted changes and full file replacements as fallbacks
Example: "Try making this targeted change; if that fails, use this complete file"

Use Pattern Matching

Look for function signatures, class definitions, or comments as anchors
Example: Find the function starting with 'def connect_arena'

Work with Temporary Files

Create new files and validate before replacing originals
Example: "Create temp file first, then replace original after verification"

Regular Expressions

For complex pattern matching, use regex with clear documentation
Example: Find pattern: class\s+ArenaAPIClient.*?def\s+init"