package dev.qynl.myfirstmod;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormationMathTest {

    @Test
    void testLineFormationCoordinates() {
        Vec3d basePos = new Vec3d(0, 64, 0);
        int cols = 4;
        int totalCount = 12;

        for (int i = 0; i < totalCount; i++) {
            int row = i / cols;
            int col = i % cols;

            double rowOffset = row * 2.5;
            double offsetX = -rowOffset;
            double offsetZ = (col - (cols / 2.0)) * 2.2;

            Vec3d pos = basePos.add(offsetX, 0, offsetZ);
            assertNotNull(pos);
            assertTrue(pos.x <= 0, "Row should advance along X axis");
            assertTrue(Math.abs(pos.z) <= (cols * 2.2), "Column spread should be bounded");
        }
    }

    @Test
    void testCircleFormationRadialDistribution() {
        Vec3d center = new Vec3d(100, 64, 100);
        int count = 16;
        double radius = 12.0;

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double px = center.x + radius * Math.cos(angle);
            double pz = center.z + radius * Math.sin(angle);

            double dist = Math.sqrt(Math.pow(px - center.x, 2) + Math.pow(pz - center.z, 2));
            assertEquals(radius, dist, 0.001, "All units in circle formation should be exactly at specified radius");
        }
    }

    @Test
    void testWedgeFormationSymmetry() {
        int count = 10;
        for (int i = 0; i < count; i++) {
            int row = (i + 1) / 2;
            int side = (i % 2 == 0) ? 1 : -1;
            double rightOffset = side * row * 1.5;

            if (i > 0) {
                assertTrue(row >= 1, "Wedge row index should increment");
                assertTrue(Math.abs(rightOffset) > 0, "Wedge side offset should spread outward");
            }
        }
    }
}
