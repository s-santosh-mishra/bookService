package com.servicehub.serv.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class OdishaServiceArea {

    private static final double BOUNDARY_TOLERANCE = 1.0e-8;

    private final List<double[]> boundary = loadBoundary();

    public boolean contains(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            return false;
        }

        boolean inside = false;
        for (int i = 0, j = boundary.size() - 1; i < boundary.size(); j = i++) {
            double[] first = boundary.get(i);
            double[] second = boundary.get(j);
            double firstLongitude = first[0];
            double firstLatitude = first[1];
            double secondLongitude = second[0];
            double secondLatitude = second[1];

            if (isOnSegment(
                    longitude,
                    latitude,
                    firstLongitude,
                    firstLatitude,
                    secondLongitude,
                    secondLatitude)) {
                return true;
            }

            boolean crossesLatitude = (firstLatitude > latitude)
                    != (secondLatitude > latitude);
            if (crossesLatitude) {
                double crossingLongitude = firstLongitude
                        + (secondLongitude - firstLongitude)
                                * (latitude - firstLatitude)
                                / (secondLatitude - firstLatitude);
                if (longitude < crossingLongitude) {
                    inside = !inside;
                }
            }
        }

        return inside;
    }

    private static List<double[]> loadBoundary() {
        ClassPathResource resource = new ClassPathResource(
                "geo/odisha-boundary.csv");
        List<double[]> coordinates = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] pair = line.split(",", -1);
                if (pair.length != 2) {
                    throw new IllegalStateException(
                            "Invalid coordinate in the Odisha service-area boundary.");
                }

                double longitude = Double.parseDouble(pair[0]);
                double latitude = Double.parseDouble(pair[1]);
                if (!Double.isFinite(longitude) || !Double.isFinite(latitude)) {
                    throw new IllegalStateException(
                            "Invalid coordinate in the Odisha service-area boundary.");
                }
                coordinates.add(new double[] { longitude, latitude });
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load the Odisha service-area boundary.",
                    exception);
        }

        if (coordinates.size() < 4) {
            throw new IllegalStateException(
                    "The Odisha service-area boundary is incomplete.");
        }

        return List.copyOf(coordinates);
    }

    private boolean isOnSegment(
            double longitude,
            double latitude,
            double firstLongitude,
            double firstLatitude,
            double secondLongitude,
            double secondLatitude) {
        double crossProduct = (latitude - firstLatitude)
                * (secondLongitude - firstLongitude)
                - (longitude - firstLongitude)
                        * (secondLatitude - firstLatitude);
        if (Math.abs(crossProduct) > BOUNDARY_TOLERANCE) {
            return false;
        }

        return longitude >= Math.min(firstLongitude, secondLongitude)
                        - BOUNDARY_TOLERANCE
                && longitude <= Math.max(firstLongitude, secondLongitude)
                        + BOUNDARY_TOLERANCE
                && latitude >= Math.min(firstLatitude, secondLatitude)
                        - BOUNDARY_TOLERANCE
                && latitude <= Math.max(firstLatitude, secondLatitude)
                        + BOUNDARY_TOLERANCE;
    }
}
