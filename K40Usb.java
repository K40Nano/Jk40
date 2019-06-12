import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.ConfigDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.Interface;
import org.usb4java.InterfaceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public class K40Usb implements AutoCloseable {
    ByteBuffer packet = ByteBuffer.allocateDirect(34);
    IntBuffer transfered = IntBuffer.allocate(1);
    StringBuffer buffer = new StringBuffer();
    
    Context context;
    DeviceHandle handle;
    boolean kernel_detached = false;
    
    public static final int K40VENDERID = 0x1A86;
    public static final int K40PRODUCTID = 0x5512;
    public static final byte K40_ENDPOINT_WRITE = (byte)0x02;
    public static final byte K40_ENDPOINT_READ = (byte)0x82;
    
    public static final int PACKET_SIZE = 30;
    public static final int TIMEOUT = 200;
    
    
    int interface_number = 0;

        
    int[] crc_table = new int[] {
        0x00, 0x5E, 0xBC, 0xE2, 0x61, 0x3F, 0xDD, 0x83,
        0xC2, 0x9C, 0x7E, 0x20, 0xA3, 0xFD, 0x1F, 0x41,
        0x00, 0x9D, 0x23, 0xBE, 0x46, 0xDB, 0x65, 0xF8,
        0x8C, 0x11, 0xAF, 0x32, 0xCA, 0x57, 0xE9, 0x74
    };


    private byte crc(ByteBuffer line) {
        int crc = 0;
        for (int i = 2; i < 32; i++) {
            crc = line.get(i) ^ crc;
            crc = crc_table[crc & 0x0f] ^ crc_table[16 + ((crc >> 4) & 0x0f)];
        }
        return (byte)crc;
    }
    
    public Device getK40() throws LibUsbException {
        DeviceList list = new DeviceList();
        try {
            int results;
            results = LibUsb.getDeviceList(context, list);
            if (results < LibUsb.SUCCESS) {
                throw new LibUsbException("Can't read device list.", results);
            }
            System.out.println("list read.");
            for (Device d : list) {
                DeviceDescriptor describe = new DeviceDescriptor();
                results = LibUsb.getDeviceDescriptor(d, describe);
                if (results < LibUsb.SUCCESS) {
                    throw new LibUsbException("Can't read device descriptor.", results);
                }
                if ((describe.idVendor() == K40VENDERID) && (describe.idProduct() == K40PRODUCTID)) {
                    System.out.println("device found.");
                    return d;
                }
            }
        } finally {
            LibUsb.freeDeviceList(list, true);
        }
        System.out.println("no device found.");
        throw new LibUsbException("Device was not found.", 0);
    }



    public void write(String message) {
        System.out.println("writing data");
        buffer.append(message);
        check_if_packet_send();
    }
    
    public void check_if_packet_send() {
        System.out.println("packetizing data");
        int results;
        System.out.println(buffer.length() + " vs " +  PACKET_SIZE);
        packet.put(0,(byte)166);
        packet.put(32,(byte)166);
        packet.put(1,(byte)0);

        while (buffer.length() >= PACKET_SIZE) {
            CharSequence sub = buffer.subSequence(0,PACKET_SIZE);
            for (int i = 2; i < PACKET_SIZE; i++) {
                packet.put(i, (byte)sub.charAt(i-2));
            }
            buffer.delete(0, PACKET_SIZE);
            System.out.println("Direct? " + packet.isDirect());
            packet.put(33,crc(packet));
            results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, packet, transfered, TIMEOUT);
            if (results < LibUsb.SUCCESS) {
                //throw new LibUsbException("Data move failed.", results);
            }
            System.out.println("packet sent.");
            byte[] bytes = new byte[34];
            packet.get(bytes,0,34);
            for (int q = 0; q < bytes.length; q++) {
                System.out.print(bytes[q] + " ");
            }
            System.out.println(bytes);
        }
    }
    
    public void flush() {
        System.out.append("Flushing.");
        while (buffer.length() < PACKET_SIZE) {
            buffer.append("F");
        }
        System.out.println(buffer.length() + " vs " +  PACKET_SIZE);
        check_if_packet_send();
    }

    public void open() throws LibUsbException {
        System.out.println("OPEN.");
        context = new Context();
        int results = LibUsb.init(context);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not initialize.", results);
        }
        System.out.println("contexted.");
        Device device = getK40();
        handle = new DeviceHandle();
        results = LibUsb.open(device, handle);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not open device handle.", results);
        }
        System.out.println("deviced.");
        
        ConfigDescriptor config = new ConfigDescriptor();
        results = LibUsb.getActiveConfigDescriptor(device, config);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("configuration was not found.", results);
        }
        System.out.println(config.toString());
        Interface iface = config.iface()[0];
        InterfaceDescriptor setting = iface.altsetting()[0];
        
        System.out.println("settings: ");
        System.out.println(setting.toString());
        
        interface_number = setting.bInterfaceNumber();
        LibUsb.freeConfigDescriptor(config);
        
        if (LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)
                && LibUsb.kernelDriverActive(handle, interface_number) != 0) {
            results = LibUsb.detachKernelDriver(handle, interface_number);
            if (results < LibUsb.SUCCESS) {
                throw new LibUsbException("Could not remove kernel driver.", results);
            }
            kernel_detached = true;
            System.out.println("detached");
        }
        System.out.println("detached (if needed)");
       
        
        results = LibUsb.claimInterface(handle, interface_number);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not claim the interface.", results);
        }
        System.out.println("claimed.");
    }

    @Override
    public void close() {
        System.out.println("closing");
        try {
            flush();
            LibUsb.handleEventsCompleted(context,
                                        null);
        } catch (LibUsbException e) {
            //Couldn't flush. Move along.
            e.printStackTrace();
        }
        if (kernel_detached) {
            int results = LibUsb.attachKernelDriver(handle, interface_number);
            if (results < LibUsb.SUCCESS) {
                //throw new LibUsbException("Could not reattach kernel driver", results);
            }
            kernel_detached = false;
            System.out.println("reattached.");
        }
        if (handle != null) {
            LibUsb.close(handle);
            System.out.println("handleclosed.");
        }
        LibUsb.exit(context);
        System.out.println("exited.");
        context = null;
    }
}
