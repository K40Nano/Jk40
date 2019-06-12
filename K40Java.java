public class K40Java {

    public static void main(String[] args) {
        try(K40Usb usb = new K40Usb()) {
            System.out.println("OPENING.");
            usb.open();
            usb.write("IPP");   
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
