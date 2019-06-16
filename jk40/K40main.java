package jk40;

import java.util.ArrayList;

public class K40main {

    public static void main(String[] args) {
        K40Device p = new K40Device();
        p.open();
        p.setSpeed(5);
        p.move_absolute(1, 1);
        p.cut_absolute(50, 25);
        p.cut_absolute(100, 100);
        p.cut_absolute(50, 50);
        p.setSpeed(40);
        p.move_absolute(0, 0);
        p.move_absolute(50, 50);
        p.move_absolute(0, 0);
        p.cut_absolute(100, 100);
        p.execute();
        p.home();
        p.raster_start();
        ArrayList<Byte> bytes = new ArrayList<Byte>();
        bytes.add((byte)50);
        bytes.add((byte)75);
        bytes.add((byte)255);
        bytes.add((byte)0);
        bytes.add((byte)52);
        p.scanline_raster(bytes, false);
        p.scanline_raster(new ArrayList<Byte>(bytes), true);
        p.scanline_raster(bytes, false);
        p.scanline_raster(new ArrayList<Byte>(bytes), true);
        p.execute();
        p.close();
        System.out.println("Main thread has ended.");
    }

}
