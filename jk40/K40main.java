package jk40;

import java.util.ArrayList;

public class K40main {

    public static void main(String[] args) {
        K40Device p = new K40Device();
        p.open();

        p.home();
        p.raster_start();
        ArrayList<Byte> bytes = new ArrayList<Byte>();
        bytes.add((byte)50);
        bytes.add((byte)75);
        bytes.add((byte)255);
        bytes.add((byte)0);
        bytes.add((byte)52);
        p.execute();
        p.close();
        System.out.println("Main thread has ended.");
    }

}
