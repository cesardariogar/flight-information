package com.flightsinfo.tar1090.model;

public record BoundingBox(double minLatitude,
                          double maxLatitude,
                          double minLongitude,
                          double maxLongitude) {

    public BoundingBox {
        checkLatitude(minLatitude);
        checkLatitude(maxLatitude);
        checkLongitude(minLongitude);
        checkLongitude(maxLongitude);
    }

    private void checkLatitude(double lat) {
        if (lat < -90 || lat > 90)
            throw new RuntimeException(String.format("Illegal latitude %f. Must be within [-90, 90]", lat));
    }

    private void checkLongitude(double lon) {
        if (lon < -180 || lon > 180)
            throw new RuntimeException(String.format("Illegal longitude %f. Must be within [-90, 90]", lon));
    }
}
