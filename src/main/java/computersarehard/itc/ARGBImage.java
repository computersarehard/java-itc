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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * A {@link ITCimage} subclass that provides additional processing for ARGB images extracted from .itc streams.
 *
 * <p>
 *  ARGB is not a commonly used file format and thus requires additional processing before it can be read by most image
 *  aware programs. Fortunately, converting ARGB to PNG format is fairly straight forward. All files produced by
 *  {@link writeToStream(OutputStream} are valid PNG files.
 * </p>
 */
public class ARGBImage extends ITCImage {
    private static final byte COLOR_TYPE_ALPHA = 6;
    private static final Charset US_ASCII;
    private static final int DEFAULT_COMPRESSION = Deflater.NO_COMPRESSION;

    static {
        try {
            US_ASCII = Charset.forName("US-ASCII");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ARGBImage (Format format, long width, long height, byte[] data) {
        super(format, width, height, data);
    }

    @Override
    public void writeToStream (OutputStream output) throws IOException {
        writeToStream(output, DEFAULT_COMPRESSION);
    }

    /**
     * An alternate methods to {@link writeToStream(OutputStream) which allows the caller to specify the
     * compresion level (see {@link Deflater}) used while encoding the image to PNG.
     *
     * @param output The stream to which the encoded image will be written.
     * @param deflaterCompressionLevel The compression level to use while encoding the PNG image.
     * @throws IOException If the underlying stream encounters an IOException.
     */
    public void writeToStream (OutputStream output, int deflaterCompressionLevel) throws IOException {
        // Write the PNG header.
        output.write(new byte[] {
            (byte)0x89,
            (byte)0x50,
            (byte)0x4e,
            (byte)0x47,
            (byte)0x0d,
            (byte)0x0a,
            (byte)0x1a,
            (byte)0x0a
        });
        // Settings are all hardcoded...
        writeHeader(output, (byte)8, COLOR_TYPE_ALPHA, (byte)0, (byte)0, (byte)0);
        writeARGBData(output, deflaterCompressionLevel);
        writeBox(output, "IEND", new byte[0]);
    }

    private void writeHeader (OutputStream output, byte depth, byte colorType, byte compression, byte filter,
        byte interlace) throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        writeUnsignedInt(buf, getWidth());
        writeUnsignedInt(buf, getHeight());
        buf.write(new byte[] {depth, colorType, compression, filter, interlace});
        writeBox(output, "IHDR", buf.toByteArray());
    }

    private void writeBox (OutputStream output, String name, byte[] data) throws IOException {
        CRC32 crc32 = new CRC32();
        byte[] nameBytes = name.getBytes(US_ASCII);
        crc32.update(nameBytes);

        if (data.length > 0) {
            crc32.update(data);

            writeUnsignedInt(output, data.length);
            output.write(nameBytes);
            output.write(data);
            writeSignedInt(output, crc32.getValue());
        } else {
            writeUnsignedInt(output, 0);
            output.write(nameBytes);
            writeSignedInt(output, crc32.getValue());
        }
    }

    private void writeUnsignedInt (OutputStream out, long value) throws IOException {
        out.write(new byte[] {
            (byte)(0xFF & (value >> 24)),
            (byte)(0xFF & (value >> 16)),
            (byte)(0xFF & (value >> 8)),
            (byte)(0xFF & (value)),
        });
    }

    private void writeSignedInt (OutputStream out, long value) throws IOException {
        int intValue = (int)value;
        out.write(new byte[] {
            (byte)(0xFF & (intValue >> 24)),
            (byte)(0xFF & (intValue >> 16)),
            (byte)(0xFF & (intValue >> 8)),
            (byte)(0xFF & (intValue)),
        });
    }

    private void writeARGBData (OutputStream output, int compressionLevel) throws IOException {
        byte[] data = getData();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        for (long y = 0, height = getHeight(); y < height; y++) {
            buf.write(0);
            for (long x = 0, width = getWidth(); x < width; x++) {
                int offset = (int)((y * width) + x) * 4;
                buf.write(data[offset + 1]);
                buf.write(data[offset + 2]);
                buf.write(data[offset + 3]);
                buf.write(data[offset]);
            }
        }

        writeData(output, buf.toByteArray(), compressionLevel);
    }

    private void writeData (OutputStream output, byte[] data, int compressionLevel) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        DeflaterOutputStream compressor = new DeflaterOutputStream(compressed, new Deflater(compressionLevel));
        compressor.write(data);
        compressor.finish();

        writeBox(output, "IDAT", compressed.toByteArray());
    }
}
