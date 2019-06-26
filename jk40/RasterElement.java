package jk40;

public class RasterElement {

    private byte[] imageData;
    private int stride;
    private int width;
    private int height;
    private int bitDepth;
    private int samplesPerPixel;

    public RasterElement(int width, int height) {
        this(width, height, 1, 1);
    }

    public RasterElement(int width, int height, int bitDepth) {
        this(width, height, bitDepth, 1);
    }

    public RasterElement(int width, int height, int bitDepth, int samplesPerPixel) {
        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.samplesPerPixel = samplesPerPixel;
        this.stride = (int) Math.ceil(bitDepth * samplesPerPixel * ((float) width) / 8.0);
        this.imageData = new byte[stride * height];
    }

    public int getPixel(int x, int y) {
        return getPixel(x, y, 0, false);
    }

    public int setPixel(int x, int y, int v) {
        return getPixel(x, y, v, true);
    }

    private int getPixel(int x, int y, int replace, boolean set) {
        int offset = stride * y;
        int pixelLengthInBits = samplesPerPixel * bitDepth;
        int startPosInBits = (offset * 8) + x * pixelLengthInBits;
        int endPosInBits = startPosInBits + pixelLengthInBits - 1;
        int startPosInBytes = startPosInBits / 8;
        int endPosInBytes = endPosInBits / 8;
        long value = 0;
        for (int i = startPosInBytes; i <= endPosInBytes; i++) {
            value <<= 8;
            value |= (imageData[i] & 0xFF);
        }
        int unusedBitsRightOfSample = (8 - (endPosInBits + 1) % 8) % 8;
        long maskSampleBits = (1L << pixelLengthInBits) - 1;
        long pixel = (value >> unusedBitsRightOfSample) & maskSampleBits;
        if (!set) {
            return (int) pixel;
        }

        value &= ~(maskSampleBits << unusedBitsRightOfSample);
        value |= (replace & maskSampleBits) << unusedBitsRightOfSample;
        for (int i = endPosInBytes; i >= startPosInBytes; i--) {
            imageData[i] = (byte) (value & 0xff);
            value >>= 8;
        }
        return (int) pixel;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int getSamplesPerPixel() {
        return samplesPerPixel;
    }

}
