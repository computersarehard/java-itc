package computersarehard.itc;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A class that encapsulates the raw image information that was extracted from an .itc stream.
 *
 * <p>
 *  The extracted information includes, a format classifier, width and height dimensions, and raw data. Depending on the
 *  format, the raw data may require additional processing before it can be read by other programs.
 * </p>
 *
 * <p>
 *  The extracted file information can be written to a stream via {@link #writeToStream(OutputStream)} and the
 *  implementation will perform the necessary processing to produce an image file that can be read by external
 *  programs.
 * </p>
 *
 * @author Peter Rebholz
 */
public class ITCImage {
    private final Format format;
    private final byte[] data;
    private final long width;
    private final long height;

    /**
     * Constructs a new instance with a specified format, width, height, and data array.
     *
     * @param format A format identifier for the data as it was represented in the .itc stream.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param data The raw image data that was extracted from the stream.
     */
    public ITCImage (Format format, long width, long height, byte[] data) {
        this.format = format;
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public final Format getFormat () {
        return format;
    }

    public final long getWidth() {
        return width;
    }

    public final long getHeight () {
        return height;
    }

    public final byte[] getData () {
        return data;
    }

    /**
     * Writes the image to the specified {@link OutputStream} and while performing any additional processing
     * necessary to produce a well formatted and compatible image file.
     *
     * <p>
     *  Some .itc image formats (such as JPEG and PNG) don't require any additional processing and their data will
     *  be directly written to the stream.
     * </p>
     *
     * @param output A valid {@link OutputStream} to which the image data will be written.
     * @throws IOException If there is an underlying issue writing to the supplied stream.
     */
    public void writeToStream (OutputStream output) throws IOException {
        output.write(data);
    }

    /**
     * An enumeration of known image formats used in .itc files. Also functions as a factory for {@link ITCImage}
     * instances so each format could theoretically use different {@link ITCImage} subclasses.
     */
    public enum Format {
        PNG("png"),
        JPEG("jpg"),
        ARGB("png") {
            @Override
            public ITCImage newImage (long width, long height, byte[] data) {
                return new ARGBImage(this, width, height, data);
            }
        };

        private String extension;

        private Format (String extension) {
            this.extension = extension;
        }

        /**
         * A factory method for creating new {@link ITCImage} instances for the format. Each format type could
         * possibly return a different {@link ITCImage} implementation.
         *
         * @param width The width of the image in pixels.
         * @param height The height of the image in pixels.
         * @param data The raw image data that was read from the .itc stream.
         * @return A newly created {@link ITCImage} with the specified parameters.
         */
        public ITCImage newImage (long width, long height, byte[] data) {
            return new ITCImage(this, width, height, data);
        }

        /**
         * Returns a {@link Format} instance for a 4 byte format specifier read from an .itc stream.
         *
         * @param format A 4 byte format specifier.
         * @return A {@link Format} instance for a 4 byte format specifier read from an .itc stream.
         * @throws UnknownFormatException If the format specifier was not a known format.
         */
        public static Format valueOf (byte[] format) {
            String formatString = new String(format);
            if ("PNGf".equals(formatString) || format[3] == 0x0e) {
                return PNG;
            } else if (format[3] == 0x0d) {
                return JPEG;
            } else if ("ARGb".equals(formatString)) {
                return ARGB;
            } else {
                throw new UnknownFormatException("Unknown format: " + formatString);
            }
        }
    }

    /**
     * An exception that is thrown when an unknown image format is encountered.
     */
    public static class UnknownFormatException extends RuntimeException {
        /**
         * Creates a new instance with the specified message.
         *
         * @param message The exception's message
         */
        public UnknownFormatException (String message) {
            super(message);
        }
    }
}
