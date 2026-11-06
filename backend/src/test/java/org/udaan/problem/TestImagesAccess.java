package org.udaan.problem;

/** Lets tests in other packages create a small PNG. */
public final class TestImagesAccess {

    private TestImagesAccess() {
    }

    public static byte[] png() {
        return TestImages.png(40, 30);
    }
}
