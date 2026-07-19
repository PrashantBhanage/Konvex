package io.konvex.util;

/**
 * Geographic calculation helpers.
 */
public final class GeoUtils {

	/** Mean Earth radius in kilometres (WGS84 approximation used by Haversine). */
	private static final double EARTH_RADIUS_KM = 6371.0;

	private GeoUtils() {
	}

	/**
	 * Computes the great-circle distance in kilometres between two points on Earth
	 * using the Haversine formula.
	 *
	 * <p>Latitude and longitude are angular coordinates on a sphere, not planar
	 * Cartesian axes. Euclidean distance ({@code √((Δlat)² + (Δlon)²)}) treats those
	 * degrees as if they were flat metres on a plane, which underestimates or
	 * overestimates real separation — especially as latitude changes (a degree of
	 * longitude shrinks toward the poles) and over longer ranges. Haversine accounts
	 * for Earth's curvature by converting the angular separation of the two points
	 * into an arc length along the sphere's surface.
	 *
	 * @param lat1 latitude of the first point in degrees
	 * @param lon1 longitude of the first point in degrees
	 * @param lat2 latitude of the second point in degrees
	 * @param lon2 longitude of the second point in degrees
	 * @return distance between the points in kilometres
	 */
	public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
		double lat1Rad = Math.toRadians(lat1);
		double lat2Rad = Math.toRadians(lat2);
		double deltaLat = Math.toRadians(lat2 - lat1);
		double deltaLon = Math.toRadians(lon2 - lon1);

		double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
				+ Math.cos(lat1Rad) * Math.cos(lat2Rad)
				* Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS_KM * c;
	}
}
