package jk40;

import org.usb4java.LibUsbException;

public class K40main {

    public static void main(String[] args) {
        K40Usb usb = null;
        try {
            usb = new K40Usb();
            usb.open();
            usb.write("IPP");   
        } catch (LibUsbException ignored) {
        }
        finally {
            if (usb != null) usb.close();
        }
    }

}
