/*
  Copyright 2013 Peter Rebholz

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
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
        "4afb22e3c83b402019d7048b81169754",
        "5d586d843c17dabb2ad9533344e117ea",
        "d59ff916678641db0c8fee505b1a2124"
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
