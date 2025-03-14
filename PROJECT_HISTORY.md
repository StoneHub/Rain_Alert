# Rain Alert Project History

This document provides a chronological summary of major changes and development sessions for the Rain Alert Android application. It serves as a reference for onboarding new team members and maintaining a history of project evolution.

## Recent Development Sessions

### March 9, 2025 - UI Reorganization

**Main Changes:**
- Redesigned app UI to use a vertical scrolling layout instead of carousel
- Moved service control to the app bar as a toggle switch
- Improved app bar design with semi-transparent background for better text visibility
- Simplified component hierarchy for better user experience:
  - Map component displayed directly in the vertical flow
  - Station list positioned below the map
  - Removed redundant quick access widget
- Preserved the WeatherCarousel component but marked it as unused for reference

**Key Files Modified:**
- Updated:
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/MainScreen.kt` - Complete overhaul of UI structure
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/WeatherCarousel.kt` - Marked as unused
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/map/SharedMapComponent.kt` - Updated documentation

**Technical Details:**
- Enhanced UX with more intuitive vertical flow
- Improved accessibility by placing service control toggle in app bar
- Added visual indicators for service state
- Maintained all functionality while streamlining navigation

### March 9, 2025 - Map Component and Coordinate System Fixes

**Main Changes:**
- Fixed weather layer alignment issues by implementing proper coordinate conversions between WGS84 and EPSG:3857 systems
- Added dynamic bounding box calculations to ensure overlays match the visible map area
- Reorganized UI components for better maintainability:
  - Split large `SharedMapComponent.kt` into smaller focused files
  - Created separate files for map display modes, weather controls, and station info dialog
  - Extracted core map rendering functionality to `MapContent.kt`
- Removed unused parameters and properties to eliminate compiler warnings

**Key Files Modified:**
- Created:
  - `/app/src/main/java/com/stoneCode/rain_alert/util/MapCoordinateUtils.kt`
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/map/MapDisplayMode.kt`
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/map/WeatherControls.kt`
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/map/StationInfoDialog.kt`
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/map/MapContent.kt`
- Updated:
  - `/app/src/main/java/com/stoneCode/rain_alert/repository/RadarMapRepository.kt`
  - `/app/src/main/java/com/stoneCode/rain_alert/viewmodel/RadarMapViewModel.kt`
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/map/DirectMapOverlay.kt`
  - `/app/src/main/java/com/stoneCode/rain_alert/ui/map/SharedMapComponent.kt`

**Technical Details:**
- Implemented coordinate transformation functions to convert between WGS84 (GPS) and EPSG:3857 (Web Mercator) systems
- Refactored map component architecture to improve separation of concerns
- Enhanced UI components with cleaner interfaces and better state management

## Previous Work

### March 5, 2025 - Map Component Integration

**Main Changes:**
- Fixed map component integration issues
- Unified map components with shared implementations
- Added back button to WeatherMapScreen
- Refactored weather map to focus on current weather

**Commits:**
- `86ca048 Fix map component integration issues`
- `8f179e3 Unify map components with shared implementations`
- `3923505 Add back button to WeatherMapScreen`
- `53f051a Refactor weather map to focus on current weather`