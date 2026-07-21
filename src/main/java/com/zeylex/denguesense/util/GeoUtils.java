package com.zeylex.denguesense.util;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GeoUtils {
    // SRID 4326 = WGS84, standard lat/lng
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    public static Point toPoint(double latitude, double longitude) {
        // JTS Point takes (x, y) = (longitude, latitude) — easy to get backwards
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}