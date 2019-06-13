     

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

public class K40Usb {

    public static final int K40VENDERID = 0x1A86;
    public static final int K40PRODUCTID = 0x5512;
    public static final byte K40_ENDPOINT_WRITE = (byte) 0x02; //0x02  EP 2 OUT
    public static final byte K40_ENDPOINT_READ = (byte) 0x82; //0x82  EP 2 IN
    public static final byte K40_ENDPOINT_READ_I = (byte) 0x81; //0x81  EP 1 IN

    public static final int PACKET_SIZE = 30;

    ByteBuffer hello = ByteBuffer.allocateDirect(1);
    ByteBuffer packet = ByteBuffer.allocateDirect(34);
    IntBuffer transfered = IntBuffer.allocate(1);

    StringBuffer buffer = new StringBuffer();

    Context context = null;
    Device device = null;
    DeviceHandle handle = null;
    boolean kernel_detached = false;
    int interface_number = 0;
    
    public static final int STATUS_OK = 206;
    public static final int STATUS_CRC = 207;
    public static final int STATUS_BUSY = 238;
    
    public int byte_0 = 0;
    public int status = 0;
    public int byte_2 = 0;
    public int byte_3 = 0;
    public int byte_4 = 0;
    public int byte_5 = 0;
    

    int[] crc_table = new int[]{
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
        return (byte) crc;
    }

    public void write(String message) {
        System.out.println("Message: " + message);
        buffer.append(message);
        System.out.println("Buffer: " + buffer.toString());
        send_complete_packets();
    }

    public void create_packet(CharSequence cs) {
        System.out.println("Buffer: " + buffer.toString());
        System.out.println(cs.toString());
        packet.clear();
        packet.put((byte) 166);
        packet.put((byte) 0);
        for (int i = 0; i < cs.length(); i++) {
            packet.put((byte) cs.charAt(i));
        }
        while (packet.position() < 32) {
            packet.put((byte) 'F');
        }
        packet.put((byte) 166);
        packet.put(crc(packet));

    }

    public void send_complete_packets() {
        while (buffer.length() >= PACKET_SIZE) {
            update_status();
            if (status == STATUS_OK) {
                 create_packet(buffer.subSequence(0, PACKET_SIZE));
                 buffer.delete(0, PACKET_SIZE);
                 send_packet();
            }
        }
    }

    public void send_packet() {
        do {
            update_status();
        } while (status != STATUS_OK);
        do {
            transmit_packet();
            update_status();
        } while (status != STATUS_OK);
    }
    
    public void transmit_packet() {
        System.out.println("send packet");
        transfered.clear();
        if (handle == null) {
            throw new LibUsbException("Handle not set", 0);
        }
        int results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, packet, transfered, 500L);
        if (results < LibUsb.SUCCESS) {
            //throw new LibUsbException("Data move failed.", results);
            System.out.print("send results " + results);
        }
        System.out.println("" + transfered.get(0));
        System.out.println("Sent");
    }
    
    public void update_status() {
        if (handle == null) {
            throw new LibUsbException("Handle not set", 0);
        }
        transfered.clear();
        hello.put(0,(byte)160);
        int results = 0;
        do {
            results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, hello, transfered, 500L);
            try {
                Thread.sleep(10);
            } catch(Exception e) { }
        } while (results != LibUsb.SUCCESS);
        
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Data move failed.", results);
        }
        
        ByteBuffer read_buffer = ByteBuffer.allocateDirect(6);
        results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_READ, read_buffer, transfered, 500L);
        
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Data move failed.", results);
        }
        
        if (transfered.get(0) == 6) {
            byte_0 = read_buffer.get(0) & 0xFF;
            status = read_buffer.get(1) & 0xFF;
            byte_2 = read_buffer.get(2) & 0xFF;
            byte_3 = read_buffer.get(3) & 0xFF;
            byte_4 = read_buffer.get(4) & 0xFF;
            byte_5 = read_buffer.get(5) & 0xFF;
        }
        else {
            throw new LibUsbException("Weird.", results);
        }
        
        System.out.println(read_buffer);
        System.out.println("" + transfered.get(0));
        try { while (true) { System.out.print((read_buffer.get() & 0xFF) + " "); } } catch (Exception e) {System.out.println("");}
        System.out.println("Read Finished.");
        
    }
    
    public void get_interupt() {
        System.out.println("Start Read Interupt");
        transfered.clear();
        if (handle == null) {
            throw new LibUsbException("Handle not set", 0);
        }
    
        ByteBuffer read_buffer = ByteBuffer.allocateDirect(32);
        int results = LibUsb.interruptTransfer(handle, K40_ENDPOINT_READ_I, read_buffer, transfered, 500L);
        
        if (results < LibUsb.SUCCESS) {
            //throw new LibUsbException("Data move failed.", results);
            System.out.print("get results " + results);
        }
        
        System.out.println(read_buffer);
        System.out.println("" + transfered.get(0));
        System.out.println("Read Finished.");        
    }

    public void flush() {
        send_complete_packets();
        if (buffer.length() < PACKET_SIZE) {
            create_packet(buffer.subSequence(0, buffer.length()));
            buffer.delete(0, buffer.length());
            send_packet();
        }
    }

    public void open() throws LibUsbException {
        System.out.println("Opened.");
        openContext();
        findK40();
        openHandle();
        checkConfig();
        detatchIfNeeded();
        claimInterface();
        LibUsb.controlTransfer(handle, (byte)64, (byte)177, (short)258, (short)0, packet, 50);
    }

    public void close() {
        try {
            flush();
        } catch (LibUsbException e) {
            //Could still need to dispatch resources.
        }
        releaseInterface();
        closeHandle();
        if (kernel_detached) {
            reattachIfNeeded();
        }
        closeContext();
        System.out.println("Closed.");
    }

    public void findK40() throws LibUsbException {
        System.out.println("Finding.()");
        DeviceList list = new DeviceList();
        try {
            int results;
            results = LibUsb.getDeviceList(context, list);
            if (results < LibUsb.SUCCESS) {
                throw new LibUsbException("Can't read device list.", results);
            }
            for (Device d : list) {
                DeviceDescriptor describe = new DeviceDescriptor();
                results = LibUsb.getDeviceDescriptor(d, describe);
                if (results < LibUsb.SUCCESS) {
                    throw new LibUsbException("Can't read device descriptor.", results);
                }
                if ((describe.idVendor() == K40VENDERID) && (describe.idProduct() == K40PRODUCTID)) {
                    device = d;
                    return;
                }
            }
        } finally {
            LibUsb.freeDeviceList(list, true);
        }
        throw new LibUsbException("Device was not found.", 0);
    }

    public void openContext() throws LibUsbException {
        System.out.println("openContext()");
        context = new Context();
        int results = LibUsb.init(context);
        System.out.println(results);

        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not initialize.", results);
        }
    }

    public void closeContext() {
        System.out.println("closedContext()");
        if (context != null) {
            LibUsb.exit(context);
            context = null;
        }
    }

    public void closeHandle() {
        System.out.println("closeHandle()");
        if (handle != null) {
            LibUsb.close(handle);
            handle = null;
        }
    }

    public void openHandle() {
        System.out.println("openHandle()");
        handle = new DeviceHandle();
        int results = LibUsb.open(device, handle);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not open device handle.", results);
        }
    }

    public void claimInterface() {
        System.out.println("claimedInterface()");
        int results = LibUsb.claimInterface(handle, interface_number);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not claim the interface.", results);
        }
    }

    public void releaseInterface() {
        System.out.println("releasedInterface");
        if (handle != null) {
            LibUsb.releaseInterface(handle, interface_number);
        }
    }

    public void detatchIfNeeded() {
        System.out.println("detatch()");
        if (LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)
                && LibUsb.kernelDriverActive(handle, interface_number) != 0) {
            int results = LibUsb.detachKernelDriver(handle, interface_number);
            if (results < LibUsb.SUCCESS) {
                throw new LibUsbException("Could not remove kernel driver.", results);
            }
            kernel_detached = true;
        }
    }

    public void reattachIfNeeded() {
        System.out.println("attach()");
        int results = LibUsb.attachKernelDriver(handle, interface_number);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not reattach kernel driver", results);
        }
        kernel_detached = false;
    }

    public void checkConfig() {
                System.out.println("checkConfig()");
        ConfigDescriptor config = new ConfigDescriptor();
        int results = LibUsb.getActiveConfigDescriptor(device, config);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("configuration was not found.", results);
        }
        Interface iface = config.iface()[0];
        InterfaceDescriptor setting = iface.altsetting()[0];
        interface_number = setting.bInterfaceNumber();
        System.out.println(config);
        LibUsb.freeConfigDescriptor(config);
    }

}
