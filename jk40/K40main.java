 

import org.usb4java.LibUsbException;

public class K40main {

    public static void main(String[] args) {
        K40Usb usb = null;
        try {
            usb = new K40Usb();
            usb.open();
            System.out.println("opened.");
            usb.write("IPP");
            usb.flush();
            usb.write("IBzzS1P");
            usb.flush();
            usb.write("IRzzS1P");
            System.out.println("written.");
        } catch (LibUsbException ignored) {
            ignored.printStackTrace();
        }
        finally {
            if (usb != null) {
                
                usb.close();
                        System.out.println("Closed.");
                    }
        }
    }

}
