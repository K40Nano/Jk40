/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jk40;

import java.awt.image.BufferedImage;

public class GraphicFunction {

    public static final int GRAYSCALE_LUMINENCE = 0;
    public static final int GRAYSCALE_LUMA = 1;
    

    public static RasterElement convert(BufferedImage image) {
        int[] rgbArray = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), rgbArray, 0, image.getWidth());
        RasterElement re = new RasterElement(image.getWidth(), image.getHeight(), 8, 4);
        int i = 0;
        for (int rgb : rgbArray) {
            re.setPixel(i++, 0, rgb);
        }
        return re;
    }

    public static RasterElement convert(RasterElement re, int function) {

        switch (function) {

            case GRAYSCALE_LUMINENCE:
                RasterElement gray = new RasterElement(re.getWidth(), re.getHeight(), 8, 1);
                for (int i = 0, ie = re.getWidth() * re.getHeight(); i < ie; i++) {
                    gray.setPixel(i, 0, (int) (0xFF * ColorFunctions.getLuminance(re.getPixel(i, 0))));
                }
                return gray;

        }
        return null;

    }
}
