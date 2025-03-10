# Rain Alert Android Project Guide

## Build Commands
- Build project: `./gradlew build`
- Clean build: `./gradlew clean build`
- Install debug APK: `./gradlew installDebug`
- Run unit tests: `./gradlew test`
- Run single unit test: `./gradlew test --tests "com.stoneCode.rain_alert.ExampleUnitTest.addition_isCorrect"`
- Run instrumented tests: `./gradlew connectedAndroidTest`
- Run lint checks: `./gradlew lint`

## Weather Event Determination Algorithm Documentation

### Algorithm Overview

#### Multi-Station Data Collection
The app uses data from multiple weather stations to improve reliability and accuracy:
1. Identifies nearby weather stations using the user's location
2. Fetches real-time observations from each station via the National Weather Service API
3. Aggregates and analyzes data from multiple sources to reduce false positives/negatives
4. Falls back to traditional forecast API when station data is unavailable

#### Proximity Weighting Calculation
The app uses inverse distance weighting to prioritize closer weather stations:
1. Calculates distance between user and each station using the Haversine formula:
   ```
   a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
   c = 2 * atan2(√a, √(1-a))
   distance = R * c  // where R is Earth's radius (6371 km)
   ```
2. Assigns weight inversely proportional to distance: `weight = 1.0 / max(distance, 1.0)`
3. Calculates weighted percentage: `(sum of weights for positive stations / sum of weights for all stations) * 100`

#### Alert Threshold Values
The following thresholds determine when alerts are triggered:
1. Rain Detection:
   - Precipitation: > 0.01 inches in the last hour
   - Station rain text indicators: "rain", "shower", "drizzle", etc.
   - Default weighted station threshold: 50% (configurable)
   - Fallback forecast probability: 50% (configurable)

2. Freeze Warning:
   - Temperature threshold: < 35°F (configurable)
   - Duration threshold: 4 hours (configurable)
   - Station weighted threshold: 50% (hardcoded)

### Technical Implementation

#### Code Structure
The weather event determination is implemented across several key components:

1. **MultiStationWeatherService**: Core analysis algorithms
   - `analyzeForRain()`: Implements distance-weighted station analysis for rain
   - `analyzeForFreeze()`: Similar weighted analysis for freezing conditions

2. **WeatherRepository**: Central component orchestrating various detection methods
   - `checkForRain()`: Primary function that attempts multiple detection strategies
   - `checkForFreezeWarning()`: Checks for freezing conditions with duration requirements
   - Handles fallback mechanisms between multiple approaches

3. **StationObservation**: Contains logic to interpret station data
   - `isRaining()`: Evaluates precipitation values and text descriptions
   - `isFreezing()`: Checks temperature against threshold values

4. **WeatherStationFinder**: Proximity calculations and station discovery
   - Implements Haversine formula for calculating distances
   - Sorts stations by distance from user location

5. **RainService/WeatherCheckWorker**: Background monitoring components
   - Continuous or periodic checks using the detection algorithms
   - Triggers notifications when conditions are met

#### Data Flow
The data flow through the system follows this pattern:

1. User location → WeatherStationFinder → List of nearby stations
2. API requests → StationObservation objects with current conditions
3. MultiStationWeatherService → Weighted analysis of conditions
4. WeatherRepository → Decision on whether to alert based on thresholds
5. RainService/WeatherCheckWorker → Notification generation if conditions met

#### Key Calculations with Examples

1. **Distance Calculation Example**:
   For a user at coordinates (40.7128, -74.0060) and a station at (40.7000, -74.0100):
   ```
   Δlat = 0.0128 radians
   Δlon = 0.0040 radians
   a = sin²(0.0128/2) + cos(40.7128) * cos(40.7000) * sin²(0.0040/2)
   distance ≈ 1.5 km
   ```

2. **Weighted Station Analysis Example**:
   For 3 stations at distances 1.5 km, 3.0 km, and 7.0 km:
   ```
   weights = [1/1.5, 1/3.0, 1/7.0] = [0.667, 0.333, 0.143]
   total weight = 1.143
   
   If stations 1 and 3 report rain:
   weighted percentage = ((0.667 + 0.143) / 1.143) * 100 = 70.9%
   Result: Above 50% threshold → Rain alert triggered
   ```

3. **Freeze Warning Duration Check Example**:
   ```
   Current hour: 35°F
   Forecast next 4 hours: [32°F, 31°F, 30°F, 31°F]
   
   All hours below threshold of 35°F for duration of 4 hours
   Result: Freeze warning triggered
   ```

### Potential Improvements

#### Enhancing Accuracy
1. **Dynamic Thresholds**:
   - Implement time-of-day adjusted thresholds (e.g., higher sensitivity at night)
   - Seasonal threshold adjustments based on local climate patterns
   - User feedback loop to adjust sensitivity based on past alert accuracy

2. **Improved Weighting Functions**:
   - Experiment with different distance-weighting functions (quadratic, Gaussian)
   - Add reliability weighting based on historical station accuracy
   - Consider topography and elevation differences between user and stations

3. **Better Text Analysis**:
   - Expand rain text indicators with language model categorization
   - Consider intensity modifiers in text descriptions (e.g., "light" vs "heavy")
   - Implement NLP techniques for more nuanced interpretation of weather descriptions

#### Additional Data Sources
1. **Radar Data Integration**:
   - Incorporate real-time radar imagery analysis for precise precipitation detection
   - Analyze radar movement patterns to predict imminent rain (30-60 minutes ahead)
   - Use radar pixel density and color to estimate precipitation intensity

2. **Community Weather Networks**:
   - Integrate with personal weather station networks (Weather Underground, etc.)

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

## Development Summary

### Firebase Integration
We've enhanced the Rain Alert app with Firebase services to collect detailed analytics data:

1. **Firestore Database Integration**
   - Added FirestoreManager.kt to store comprehensive alert data
   - Implemented document structure for tracking individual alert instances
   - Created data models to store station contributions to each alert decision

2. **User Feedback System**
   - Developed AlertFeedbackManager.kt to manage user accuracy ratings
   - Created FeedbackActivity.kt with a simple UI for gathering feedback
   - Implemented a notification follow-up system to prompt for feedback

3. **Enhanced Logging**
   - Expanded FirebaseLogger.kt with detailed metrics tracking
   - Added station-specific contribution metrics
   - Implemented decision path tracking to analyze algorithm performance

4. **Analytics Framework**
   - Created structure for analyzing algorithm performance over time
   - Implemented logic to track false positives and negatives
   - Added correlation analysis between station data and alert accuracy

### Key Learnings

1. **Algorithm Enhancement Opportunities**
   - Identified need for dynamic thresholds based on time of day
   - Discovered importance of reliability weighting based on historical station accuracy
   - Found that textual description analysis provides valuable signals

2. **Performance Insights**
   - Established that closer stations don't always provide the most accurate data
   - Confirmed that using multiple data sources significantly reduces false positives
   - Identified optimal station count (3-5) for balancing accuracy and API load

3. **Technical Improvements**
   - Implemented more efficient background processing with WorkManager
   - Optimized API request batching to reduce network overhead
   - Enhanced persistence strategy for intermittent connectivity

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