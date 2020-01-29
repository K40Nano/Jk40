package jk40;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class K40main {

    public static void main(String[] args) {
        K40Device p = new K40Device();
        p.open();
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("test.png"));
        }
        catch (IOException e) {
        }
        RasterElement re = GraphicFunction.convert(image);
        GraphicFunction.convert(re,GraphicFunction.GRAYSCALE_LUMINENCE);

        p.home();
        p.raster_start();
        ArrayList<Byte> bytes = new ArrayList<Byte>();
        bytes.add((byte) 50);
        bytes.add((byte) 75);
        bytes.add((byte) 255);
        bytes.add((byte) 0);
        bytes.add((byte) 52);
        p.execute();
        p.close();
        System.out.println("Main thread has ended.");
    }

}
