## Rain Alert - Android Weather App

Rain Alert is an Android application built using Kotlin and Jetpack Compose that helps users stay informed about potential rain and freezing conditions in their area. The app runs a background service to periodically check the weather and notifies the user when:

*   Rain is detected based on simulated weather data.
*   The temperature is forecasted to be below 35°F for more than 4 hours (freeze warning).

**Key Features:**

*   **Background Service:** Efficiently monitors weather conditions in the background using a foreground service with location type.
*   **Rain Alerts:** Notifies the user when rain is detected (currently simulated based on location).
*   **Freeze Warnings:** Alerts the user when freezing conditions are likely.
*   **Manual Refresh:** Allows users to manually refresh the weather data.
*   **Weather Website Integration:** Provides a button to quickly access the weather.gov website for detailed local weather information.
*   **Runtime Permissions:** Handles runtime permissions for location access (fine location), foreground service (location), and notifications.
*   **Alarm Scheduling:** Uses `AlarmManager` to schedule a repeating alarm for nighttime checks.
*   **Modern UI:** Built using Jetpack Compose for a declarative and responsive user interface.
*   **MVVM Architecture:** Employs the Model-View-ViewModel (MVVM) architecture with a `WeatherViewModel` to manage UI state and data.

**Future Improvements:**

*   **Real Weather API Integration:** Integrate with a real weather API (e.g., OpenWeatherMap, National Weather Service API) to provide accurate and up-to-date weather data.
*   **User Settings:** Allow users to customize notification preferences, including the frequency of weather checks, temperature thresholds for freeze warnings, and notification sounds.
*   **Enhanced UI:** Improve the user interface with more detailed weather information, icons, and potentially weather maps.
*   **Dependency Injection:** Implement a dependency injection framework (e.g., Hilt) for better code organization and testability.

**Build Instructions:**

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on an emulator or a physical device running Android 8.0 (API level 26) or higher.
4. Grant the necessary permissions (location and notifications) when prompted.

**Dependencies:**

*   Kotlin
*   Jetpack Compose
*   AndroidX Lifecycle
*   AndroidX Core KTX
*   ... (List any other dependencies from your `build.gradle.kts` file)
