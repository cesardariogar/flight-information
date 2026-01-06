# FlightInformation

A Java Spring Boot application that integrates with the [OpenSky Network](https://opensky-network.org/) API to retrieve real-time flight data and aircraft state vectors.

## Overview

FlightInformation provides a Java wrapper for the OpenSky Network REST API, enabling retrieval of global air traffic data including ADS-B, Mode-S, ADS-C, FLARM, and VHF information. The application supports both authenticated and anonymous access with built-in rate limiting.

## Features

- **Real-time Flight Data**: Retrieve current state vectors for aircraft worldwide
- **Geographic Filtering**: Query flights within specific bounding boxes
- **ICAO24 Filtering**: Filter results by aircraft ICAO24 addresses
- **Authenticated Access**: Support for authenticated requests with custom credentials
- **Rate Limiting**: Built-in rate limit enforcement to comply with OpenSky Network policies
- **Sensor Data**: Access data from your own sensors (authenticated access only)

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- OpenSky Network account (optional, for authenticated access)

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd FlightInformation
```

2. Build the project:
```bash
./mvnw clean install
```

3. Configure your OpenSky Network credentials (optional):
```bash
export OPENSKY_CLIENTID="your-client-id"
export OPENSKY_CLIENTSECRET="your-client-secret"
```

## Configuration

The application uses environment variables for OpenSky API authentication:

| Variable | Description | Required |
|----------|-------------|----------|
| `OPENSKY_CLIENTID` | Your OpenSky Network client ID | No (anonymous access available) |
| `OPENSKY_CLIENTSECRET` | Your OpenSky Network client secret | No (anonymous access available) |

Configuration is managed in `src/main/resources/application.properties`.

## OpenSky Network API Information

This application interacts with the OpenSky Network REST API to retrieve flight data. The following API endpoints are used:

### Base URL
```
https://opensky-network.org/api
```

### Endpoints

#### 1. Get All States (`/states/all`)
Retrieves state vectors for all aircraft currently tracked by OpenSky Network.

**Request Parameters:**

| Parameter | Type | Description | Required |
|-----------|------|-------------|----------|
| `time` | Integer | Unix timestamp (seconds since epoch). Use `0` for most recent data | No |
| `icao24` | String | ICAO24 address to filter by. Can be specified multiple times | No |
| `lamin` | Double | Minimum latitude for bounding box (WGS-84, degrees) | No |
| `lomin` | Double | Minimum longitude for bounding box (WGS-84, degrees) | No |
| `lamax` | Double | Maximum latitude for bounding box (WGS-84, degrees) | No |
| `lomax` | Double | Maximum longitude for bounding box (WGS-84, degrees) | No |

**Response Fields:**

The API returns a JSON object containing:
- `time`: Unix timestamp of the response
- `states`: Array of state vectors, each containing:

| Field | Type | Description |
|-------|------|-------------|
| `icao24` | String | Unique ICAO 24-bit address (hex string) |
| `callsign` | String | Aircraft callsign (can be null) |
| `origin_country` | String | Country inferred from ICAO24 address |
| `time_position` | Double | Unix timestamp of last position update (seconds since epoch) |
| `last_contact` | Double | Unix timestamp of last received message (seconds since epoch) |
| `longitude` | Double | WGS-84 longitude in degrees (can be null) |
| `latitude` | Double | WGS-84 latitude in degrees (can be null) |
| `baro_altitude` | Double | Barometric altitude in meters (can be null) |
| `on_ground` | Boolean | True if aircraft is on ground |
| `velocity` | Double | Ground speed in m/s (can be null) |
| `true_track` | Double | Heading in decimal degrees (0° = North) (can be null) |
| `vertical_rate` | Double | Vertical rate in m/s (positive = climbing) (can be null) |
| `sensors` | Array | Serial numbers of contributing sensors (can be null) |
| `geo_altitude` | Double | Geometric altitude in meters (can be null) |
| `squawk` | String | Transponder code (can be null) |
| `spi` | Boolean | Special purpose indicator |
| `position_source` | Integer | Origin of position (0=ADS-B, 1=ASTERIX, 2=MLAT, 3=FLARM) |

#### 2. Get Own States (`/states/own`)
Retrieves state vectors from your own sensors (requires authentication).

**Request Parameters:**

| Parameter | Type | Description | Required |
|-----------|------|-------------|----------|
| `time` | Integer | Unix timestamp (seconds since epoch). Use `0` for most recent data | No |
| `icao24` | String | ICAO24 address to filter by. Can be specified multiple times | No |
| `serials` | Integer | Sensor serial number. Can be specified multiple times | No |

### Rate Limits

The OpenSky Network enforces the following rate limits:

| Access Type | Endpoint | Minimum Interval |
|-------------|----------|------------------|
| Anonymous | `/states/all` | 10 seconds |
| Authenticated | `/states/all` | 5 seconds |
| Authenticated | `/states/own` | 1 second |

This application automatically enforces these limits to prevent API errors.

### Authentication

Authenticated requests provide:
- Higher rate limits
- Access to `/states/own` endpoint
- Priority processing

Authentication is performed using client credentials (client ID and client secret).

## Usage

### Basic Example

```java
// Create controller with authentication
OpenSkyApiController controller = new OpenSkyApiController(
    "your-client-id",
    "your-client-secret"
);

// Get all current states worldwide
PlaneStates worldwideStates = controller.getStates(0, null);

// Get states for specific ICAO24 addresses
String[] icao24List = {"abc9f3", "3c6444"};
PlaneStates filteredStates = controller.getStates(0, icao24List);
```

### Geographic Filtering

```java
// Define bounding box (Switzerland example)
BoundingBox switzerland = new BoundingBox(
    45.8389,  // minLatitude
    47.8229,  // maxLatitude
    5.9962,   // minLongitude
    10.5226   // maxLongitude
);

// Get states within bounding box
PlaneStates swissStates = controller.getStates(0, null, switzerland);
```

### Accessing State Vector Data

```java
PlaneStates states = controller.getStates(0, null);

for (StateVector sv : states.getStateVectors()) {
    System.out.println("Callsign: " + sv.getCallsign());
    System.out.println("Latitude: " + sv.getLatitude());
    System.out.println("Longitude: " + sv.getLongitude());
    System.out.println("Altitude: " + sv.getBaroAltitude() + "m");
    System.out.println("Velocity: " + sv.getVelocity() + "m/s");
    System.out.println("Heading: " + sv.getHeading() + "°");
    System.out.println("Country: " + sv.getOriginCountry());
    System.out.println("On Ground: " + sv.isOnGround());
}
```

### Using Your Own Sensors

```java
// Requires authentication
String[] icao24List = {"abc9f3"};
Integer[] sensorSerials = {1234, 5678};

PlaneStates myStates = controller.getMyStates(0, icao24List, sensorSerials);
```

## Running the Application

Execute the Spring Boot application:

```bash
./mvnw spring-boot:run
```

The default implementation retrieves worldwide states and Switzerland-specific states, displaying the count of aircraft in each area.

## API Models

### StateVector

Represents the state of a single aircraft at a given time, including:
- Position (latitude, longitude, altitudes)
- Velocity and heading
- Aircraft identification (ICAO24, callsign)
- Status (on ground, squawk code)
- Metadata (last contact, origin country)

### PlaneStates

Container for a collection of state vectors with a timestamp.

### BoundingBox

Defines a geographic rectangle using min/max latitude and longitude coordinates (WGS-84).

## Dependencies

- Spring Boot 3.5.5
- Spring Web
- Spring Boot Actuator
- Jackson (JSON processing)
- JUnit 4.13.1 (testing)

## Project Structure

```
src/
├── main/
│   ├── java/com/flightsinfo/tar1090/
│   │   ├── Application.java              # Main application entry point
│   │   ├── config/
│   │   │   └── OpenSkyProperties.java    # Configuration properties
│   │   ├── controller/
│   │   │   └── OpenSkyApiController.java # API client controller
│   │   ├── model/
│   │   │   ├── BoundingBox.java          # Geographic bounding box
│   │   │   ├── PlaneStates.java          # State vector collection
│   │   │   └── StateVector.java          # Individual aircraft state
│   │   ├── mappers/
│   │   │   └── OpenSkyStatesDeserializer.java # JSON deserializer
│   │   └── enums/
│   │       ├── PositionSource.java       # Position data source types
│   │       └── RequestType.java          # API request types
│   └── resources/
│       └── application.properties        # Application configuration
└── test/
    └── java/com/example/FlightInformation/
        ├── controller/
        │   └── TestOpenSkyApi.java       # API tests
        └── mapper/
            └── TestOpenSkyStatesDeserializer.java # Deserializer tests
```

## Testing

Run the test suite:

```bash
./mvnw test
```

## Contributing

When committing changes, please include:
```
Co-Authored-By: Warp <agent@warp.dev>
```

## Resources

- [OpenSky Network Website](https://opensky-network.org/)
- [OpenSky Network API Documentation](https://opensky-network.org/apidoc/)
- [OpenSky Historical Database](https://opensky-network.org/data)
- [Aircraft Database](https://opensky-network.org/aircraft-database)

## License

This project is a demonstration application for the OpenSky Network API.

## Acknowledgments

This application uses data from the [OpenSky Network](https://opensky-network.org/), which provides open air traffic data for research and non-commercial purposes.
