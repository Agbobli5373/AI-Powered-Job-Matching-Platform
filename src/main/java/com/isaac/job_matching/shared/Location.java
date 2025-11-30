package com.isaac.job_matching.shared;

import jakarta.persistence.Embeddable;

/**
 * Value object representing a geographical location.
 * 
 * <p>
 * Supports both city/state/country representation and
 * optional latitude/longitude coordinates for distance calculations.
 */
@Embeddable
public record Location(
        String city,
        String state,
        String country,
        Double latitude,
        Double longitude) {

    /**
     * Creates a Location with only city, state, and country.
     * 
     * @param city    the city name
     * @param state   the state/province
     * @param country the country
     * @return Location without coordinates
     */
    public static Location of(String city, String state, String country) {
        return new Location(city, state, country, null, null);
    }

    /**
     * Creates a Location with coordinates.
     * 
     * @param city      the city name
     * @param state     the state/province
     * @param country   the country
     * @param latitude  the latitude coordinate
     * @param longitude the longitude coordinate
     * @return Location with coordinates
     */
    public static Location withCoordinates(String city, String state, String country,
            double latitude, double longitude) {
        return new Location(city, state, country, latitude, longitude);
    }

    /**
     * Returns a formatted string representation of the location.
     * 
     * @return formatted location (e.g., "San Francisco, CA, USA")
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        if (city != null && !city.isBlank()) {
            sb.append(city);
        }
        if (state != null && !state.isBlank()) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(state);
        }
        if (country != null && !country.isBlank()) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(country);
        }
        return sb.toString();
    }

    /**
     * Checks if this location has valid coordinates.
     * 
     * @return true if both latitude and longitude are present
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Calculates the distance in kilometers to another location.
     * Uses the Haversine formula for great-circle distance.
     * 
     * @param other the other location
     * @return distance in kilometers, or null if coordinates are missing
     */
    public Double distanceToKm(Location other) {
        if (!this.hasCoordinates() || !other.hasCoordinates()) {
            return null;
        }

        final double R = 6371.0; // Earth's radius in kilometers

        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Checks if this location is the same city as another.
     * 
     * @param other the other location
     * @return true if city and country match (case-insensitive)
     */
    public boolean isSameCity(Location other) {
        if (other == null)
            return false;
        return city != null && city.equalsIgnoreCase(other.city) &&
                country != null && country.equalsIgnoreCase(other.country);
    }

    /**
     * Checks if this location is in the same country as another.
     * 
     * @param other the other location
     * @return true if country matches (case-insensitive)
     */
    public boolean isSameCountry(Location other) {
        if (other == null)
            return false;
        return country != null && country.equalsIgnoreCase(other.country);
    }
}
