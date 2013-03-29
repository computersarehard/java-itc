package computersarehard.itc;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ITCReaderTest {
    private static final String[] ARGB_MD5 = new String[] {
        "7ce5de67cf332101ae02171ff1ee87cf",
        "43ce0f9ffc9811ff65e9356bb6265c3c",
        "74803dfa6d64aa3c30e64a5313be1f53"
    };
    private static MessageDigest md5;

    @BeforeClass
    public static void beforeAll () throws Exception {
        md5 = MessageDigest.getInstance("MD5");
    }

    @Test
    public void testReadARGB () throws Exception {
        ITCImageReader reader = null;
        try {
            reader = new ITCImageReader(new FileInputStream("src/test/itc/argb-test.itc"));
            List<ITCImage> images = reader.readAll();

            assertEquals("argb file should have 3 images", 3, images.size());
            for (int i = 0; i < images.size(); i++) {
                ITCImage image = images.get(i);
                assertEquals("Each image should be ARGB", ITCImage.Format.ARGB, image.getFormat());

                assertEquals("Checksum should match", ARGB_MD5[i], md5sum(image.getData()));

                java.io.FileOutputStream out = new java.io.FileOutputStream("/tmp/test" + i + ".png");
                image.writeToStream(out);
                out.close();
            }

        } finally {
            closeQuietly(reader);
        }
    }

    private static String md5sum (byte[] data) {
        md5.reset();
        md5.update(data);

        return new BigInteger(1, md5.digest()).toString(16);
    }

    private static void closeQuietly (Closeable c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (Exception e) {}
    }
}