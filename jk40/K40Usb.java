package jk40;

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
    public static final byte K40_ENDPOINT_WRITE = (byte) 0x02;
    public static final byte K40_ENDPOINT_READ = (byte) 0x82;

    public static final int PACKET_SIZE = 30;
    public static final int TIMEOUT = 200;

    ByteBuffer hello = ByteBuffer.allocateDirect(1);
    ByteBuffer packet = ByteBuffer.allocateDirect(34);
    IntBuffer transfered = IntBuffer.allocate(1);

    StringBuffer buffer = new StringBuffer();

    Context context = null;
    Device device = null;
    DeviceHandle handle = null;
    boolean kernel_detached = false;
    int interface_number = 0;

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
        buffer.append(message);
        send_complete_packets();
    }

    public void create_packet(CharSequence cs) {
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
            create_packet(buffer.subSequence(0, PACKET_SIZE));
            buffer.delete(0, PACKET_SIZE);
            send_packet();
        }
    }

    public void send_packet() {
        if (handle == null) {
            throw new LibUsbException("Handle not set", 0);
        }
        int results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, packet, transfered, TIMEOUT);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Data move failed.", results);
        }
    }

    public void say_hello() {
        if (handle == null) {
            throw new LibUsbException("Handle not set", 0);
        }
        int results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, hello, transfered, TIMEOUT);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Data move failed.", results);
        }
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
        hello.put((byte) 160);
        openContext();
        findK40();
        openHandle();
        checkConfig();
        detatchIfNeeded();
        claimInterface();
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
    }

    public void findK40() throws LibUsbException {
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
                }
            }
        } finally {
            LibUsb.freeDeviceList(list, true);
        }
        throw new LibUsbException("Device was not found.", 0);
    }

    public void openContext() throws LibUsbException {
        context = new Context();
        int results = LibUsb.init(context);

        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not initialize.", results);
        }
    }

    public void closeContext() {
        if (context != null) {
            LibUsb.exit(context);
            context = null;
        }
    }

    public void closeHandle() {
        if (handle != null) {
            LibUsb.close(handle);
            handle = null;
        }
    }

    public void openHandle() {
        handle = new DeviceHandle();
        int results = LibUsb.open(device, handle);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not open device handle.", results);
        }
    }

    public void claimInterface() {
        int results = LibUsb.claimInterface(handle, interface_number);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not claim the interface.", results);
        }
    }

    public void releaseInterface() {
        if (handle != null) {
            LibUsb.releaseInterface(handle, interface_number);
        }
    }

    public void detatchIfNeeded() {
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
        int results = LibUsb.attachKernelDriver(handle, interface_number);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not reattach kernel driver", results);
        }
        kernel_detached = false;
    }

    public void checkConfig() {
        ConfigDescriptor config = new ConfigDescriptor();
        int results = LibUsb.getActiveConfigDescriptor(device, config);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("configuration was not found.", results);
        }
        Interface iface = config.iface()[0];
        InterfaceDescriptor setting = iface.altsetting()[0];
        interface_number = setting.bInterfaceNumber();
        LibUsb.freeConfigDescriptor(config);
    }

}
