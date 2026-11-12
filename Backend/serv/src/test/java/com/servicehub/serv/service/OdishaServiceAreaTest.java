package com.servicehub.serv.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OdishaServiceAreaTest {

    private final OdishaServiceArea serviceArea =
            new OdishaServiceArea();

    @Test
    void acceptsCoordinatesInsideOdisha() {
        assertTrue(serviceArea.contains(20.2961, 85.8245));
        assertTrue(serviceArea.contains(21.4667, 83.9667));
    }

    @Test
    void rejectsCoordinatesOutsideOdisha() {
        assertFalse(serviceArea.contains(22.5726, 88.3639));
        assertFalse(serviceArea.contains(23.3441, 85.3096));
    }

    @Test
    void rejectsNonFiniteCoordinates() {
        assertFalse(serviceArea.contains(Double.NaN, 85.8245));
        assertFalse(serviceArea.contains(20.2961, Double.POSITIVE_INFINITY));
    }
}
