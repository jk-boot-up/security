package com.jk.explore.security.searchdetect;

/** A place with coordinates, for distance calculations. */
public record City(String name, double lat, double lon) {

    public static final City PARIS = new City("Paris", 48.8566, 2.3522);
    public static final City BERLIN = new City("Berlin", 52.52, 13.405);
    public static final City LONDON = new City("London", 51.5074, -0.1278);
    public static final City MADRID = new City("Madrid", 40.4168, -3.7038);
    public static final City NEW_YORK = new City("New York", 40.7128, -74.006);
    public static final City FRANKFURT = new City("Frankfurt", 50.1109, 8.6821);
    public static final City SYDNEY = new City("Sydney", -33.8688, 151.2093);
    public static final City SINGAPORE = new City("Singapore", 1.3521, 103.8198);

    private static final java.util.List<City> ALL = java.util.List.of(PARIS, BERLIN, LONDON, MADRID, NEW_YORK, FRANKFURT, SYDNEY, SINGAPORE);

    public static City byName(String name) {
        return ALL.stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    /** Great-circle distance in kilometres (haversine). */
    public double kmTo(City o) {
        double r = 6371.0;
        double dLat = Math.toRadians(o.lat - lat);
        double dLon = Math.toRadians(o.lon - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(o.lat)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
