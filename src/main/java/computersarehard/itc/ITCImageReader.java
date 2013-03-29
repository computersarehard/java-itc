package computersarehard.itc;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A 'one shot' class that scans an {@link InputStream} containg iTunes ITC image cache files looking for the
 * embedded image entries.
 *
 * <p>
 * The next {@link ITCImage} is read from the stream each time {@link #readImage()} is called. Until no more
 * images are found and {@code null} is returned. If desired, all images can be read at once with {@link #readAll()}.
 * </p>
 *
 * @author Peter Rebholz, based on the itc python script by Simon Kennedy: https://launchpad.net/itc
 */
public class ITCImageReader implements Closeable {
    private static final String ITCH_FRAME = "itch";
    private static final String ARTW_FRAME = "artw";
    private static final String ITEM_FRAME = "item";
    private static final int ITUNES_9 = 208;
    private static final int ITUNES_OLD = 216;

    private boolean startedReading = false;
    private InputStream input;

    /**
     * Constructs a new {@link ITCImageReader} that will read from the supplied {@link InputStream}.
     *
     * @param stream A stream of the contents of an iTunes .itc file.
     */
    public ITCImageReader (InputStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("Stream cannot be null");
        }
        // Wrap the supplied InputStream with something that supports mark if it does not already support mark. We'll
        // need that functionality later on.
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }

        input = stream;
    }

    /**
     * Reads and returns the next image from the .itc stream. {@code null} will be returned if the stream has
     * reached the end and no more images have been found.
     *
     * @return The next image in the .itc stream or null if the stream does not contain any more images.
     * @throws IOException If the underlying stream encounters an IOException.
     */
    public ITCImage readImage () throws IOException {
        startedReading = true;

        while (true) {
            // Read a frame, if there still are some.
            Frame frame = readFrame();
            if (frame == null) {
                return null;
            }

            // Try to extract an image from the frame, if it is an image frame. Otherwise continue and get the next
            // frame.
            ITCImage nextImage = handleFrame(frame);
            if (nextImage != null) {
                return nextImage;
            }
        }
    }

    /**
     * Reads the stream fully and returns all of the images contained within. This method can only be called once and
     * must be called prior to any calls to {@link #readImage()}.
     *
     * @return All images contained within the stream, or an empty {@link List} if the stream contained zero images.
     * @throws IOException If the underlying stream encounters an IOException.
     * @throws IllegaStateException If the stream has already been read from.
     */
    public List<ITCImage> readAll () throws IOException {
        if (startedReading) {
            throw new IllegalStateException("Cannot perform readAll() after readImage() has been called.");
        }

        List<ITCImage> images = new ArrayList<ITCImage>(3);
        ITCImage image = readImage();
        while (image != null) {
            images.add(image);
            image = readImage();
        }

        return images;
    }

    private Frame readFrame () throws IOException {
        byte[] data = new byte[8];
        int read = input.read(data);
        if (read < data.length) {
            return null;
        }

        long size = readUnsignedInt(data, 0);
        String name = new String(data, 4, 4);

        return new Frame(size, name);
    }

    private ITCImage handleFrame (Frame frame) throws IOException {
        // Attempt to find an image within the next frame.
        if (ITCH_FRAME.equals(frame.name)) {
            return parseItch(frame);
        } else if (ARTW_FRAME.equals(frame.name)) {
            return parseArtw(frame);
        } else if (ITEM_FRAME.equals(frame.name)) {
            return parseItem(frame);
        } else {
            throw new UnexpectedFrameException("Encountered unexpected frame.", frame);
        }
    }

    private ITCImage parseItch (Frame frame) throws IOException {
        input.skip(16);

        String subframeName = new String(readBytes(4, true));
        // Return the result of handling the subframe, just in case it is an image.
        return handleFrame(new Frame(frame.size, subframeName));
    }

    private ITCImage parseArtw (Frame frame) throws IOException {
        // This section contains no data; assumed obsolete section per itc.py
        input.skip(256);

        return null;
    }

    private ITCImage parseItem (Frame frame) throws IOException {
        // Mark current position the offset field is relative to current position.
        input.mark(Integer.MAX_VALUE);
        long offset = readUnsignedInt();

        // Skip the info preamble, we aren't going to use it.
        if (offset == ITUNES_9) {
            input.skip(16);
        } else if (offset == ITUNES_OLD) {
            input.skip(20);
        }

        // Skip library, track, and method fields
        input.skip(8 + 8 + 4);

        byte[] formatBytes = readBytes(4, true);
        ITCImage.Format format = ITCImage.Format.valueOf(formatBytes);

        input.skip(4);
        long width = readUnsignedInt(), height = readUnsignedInt();

        // Restore position and skip to the image data.
        input.reset();
        // Offset is relative to the start of the frame (including size and name).
        input.skip(offset - 8);

        long size = frame.size - offset;

        // Create an appropriate image instance from the format specified in the frame.
        return format.newImage(width, height, readBytes((int)size, true));
    }

    private byte[] readBytes (int number, boolean exact) throws IOException {
        byte[] data = new byte[number];
        int read = input.read(data);
        if (read != number && exact) {
            throw new IOException(String.format("Expected to read %d bytes but instead got %d", number, read));
        }
        return data;
    }

    private long readUnsignedInt () throws IOException {
        return readUnsignedInt(readBytes(4, true), 0);
    }

    private long readUnsignedInt (byte[] bytes, int offset) {
        if (bytes == null || bytes.length < 4) {
            throw new IllegalArgumentException("Bytes cannot be null or less than 4 bytes in length");
        }

        return ((long)bytes[offset] & 0xFF) << 24
            | ((long)bytes[offset + 1] & 0xFF) << 16
            | ((long)bytes[offset + 2] & 0xFF) << 8
            | ((long)bytes[offset + 3] & 0xFF);
    }

    @Override
    public void close () throws IOException {
        input.close();
    }

    /**
     * A simple class to encapsulate frame information.
     */
    private static class Frame {
        private long size;
        private String name;

        private Frame (long size, String name) {
            this.size = size;
            this.name = name;
        }

        @Override
        public String toString () {
            return String.format("{Frame -> size: %d, name: %s}", size, name);
        }
    }

    /**
     * An exception thrown when the next 'frame' read from an .itc stream was in an expected format.
     */
    public static class UnexpectedFrameException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private Frame frame;

        /**
         * Creates a new instance with a message and a reference to the bad frame.
         *
         * @param message A (hopefully) descriptive error message.
         * @param frame The {@link Frame} that was invalid.
         */
        public UnexpectedFrameException(String message, Frame frame) {
            super(message);
            this.frame = frame;
        }
    }
}
