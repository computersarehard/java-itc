# iTunes ITC Image Extractor

This project provides a simple way of extracting images (album artwork) from iTunes `.itc` files and writing them to
a stream. For example:


    ITCImageReader reader = new ITCImageReader(new FileInputStream("my-itc.itc"));
    List<ITCImage> images = reader.readAll();

    int index = 0;
    for (ITCImage image : images) {
        java.io.FileOutputStream out = new java.io.FileOutputStream(
            String.format("/tmp/extracted-image-%d.%s", index, image.getFormat().getFileExtension())
        );
        image.writeToStream(out);
        out.close();
        index++;
    }

Much of the code in this library is based on the work of Simon Kennedy for the Python
[itc](https://launchpad.net/itc) utility. Thanks for doing all of the hard work, Simon.

This project is currently intended to be a Java API and only handles reading of images. If you need a command line tool,
I would recommend using the previously mentioned Python tool.
