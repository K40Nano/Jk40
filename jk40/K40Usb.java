package jk40;

//MIT License.
import java.nio.Buffer;
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

/*
* K40Usb is the core driver for communcating with the K40 Stock Nano board.
* It will process the USB interfacing claim and perform background operations to
* support the protocol required.

* The only commands you should need are:

* open() : Opens connection, initializes the device.
* close() : Closes connection.

* send_packet(data) : sends exactly 30 bytes of payload to the device. 

* wait_for_finish() : wait until the state is STATUS_FINISH.
* wait_for_ok() : wait until the state is STATUS_OK
 */
public class K40Usb implements BaseUsb {

    public static final int K40VENDERID = 0x1A86;
    public static final int K40PRODUCTID = 0x5512;

    public static final byte K40_ENDPOINT_WRITE = (byte) 0x02; //0x02  EP 2 OUT
    public static final byte K40_ENDPOINT_READ = (byte) 0x82; //0x82  EP 2 IN
    public static final byte K40_ENDPOINT_READ_I = (byte) 0x81; //0x81  EP 1 IN

    public static final int PAYLOAD_LENGTH = 30;

    private final IntBuffer transfered = IntBuffer.allocate(1);
    private final ByteBuffer request_status = ByteBuffer.allocateDirect(1);
    private final ByteBuffer packet = ByteBuffer.allocateDirect(34);

    private Context context = null;
    private Device device = null;
    private DeviceHandle handle = null;
    private boolean kernel_detached = false;
    private int interface_number = 0;

    public static final int STATUS_OK = 206;
    public static final int STATUS_PACKET_REJECTED = 207;

    public static final int STATUS_FINISH = 236;
    public static final int STATUS_BUSY = 238;
    public static final int STATUS_POWER = 239;

    public static final int STATUS_DEVICE_ERROR = -1;

    public int byte_0 = 0;
    public int status = 0;
    public int byte_2 = 0;
    public int byte_3 = 0;
    public int byte_4 = 0;
    public int byte_5 = 0;

    public int recovery = 0;

    /**
     * ******************
     * CRC function via: License: 2-clause "simplified" BSD license Copyright
     * (C) 1992-2017 Arjen Lentz
     * https://lentz.com.au/blog/calculating-crc-with-a-tiny-32-entry-lookup-table
     * *******************
     */
    static final int[] CRC_TABLE = new int[]{
        0x00, 0x5E, 0xBC, 0xE2, 0x61, 0x3F, 0xDD, 0x83,
        0xC2, 0x9C, 0x7E, 0x20, 0xA3, 0xFD, 0x1F, 0x41,
        0x00, 0x9D, 0x23, 0xBE, 0x46, 0xDB, 0x65, 0xF8,
        0x8C, 0x11, 0xAF, 0x32, 0xCA, 0x57, 0xE9, 0x74
    };

    public static byte crc(ByteBuffer line) {
        int crc = 0;
        for (int i = 2; i < 32; i++) {
            crc = line.get(i) ^ crc;
            crc = CRC_TABLE[crc & 0x0f] ^ CRC_TABLE[16 + ((crc >> 4) & 0x0f)];
        }
        return (byte) crc;
    }
    //*//

    @Override
    public void open() throws LibUsbException {
        openContext();
        findK40();
        openHandle();
        checkConfig();
        detatchIfNeeded();
        claimInterface();
        LibUsb.controlTransfer(handle, (byte) 64, (byte) 177, (short) 258, (short) 0, packet, 50);
    }

    @Override
    public void close() throws LibUsbException {
        releaseInterface();
        closeHandle();
        if (kernel_detached) {
            reattachIfNeeded();
        }
        closeContext();
    }

    public void error(String error) {
        System.out.println(error);
    }

    @Override
    public void send_packet(CharSequence cs) {
        if (cs.length() != PAYLOAD_LENGTH) {
            throw new LibUsbException("Packets must be exactly " + PAYLOAD_LENGTH + " bytes.", 0);
        }
        create_packet(cs);
        int count = 0;
        do {
            transmit_packet();
            update_status();
            if (count >= 50) {
                throw new LibUsbException("All packets are being rejected.", 0);
            }
            count++;
        } while (status == STATUS_PACKET_REJECTED);
    }

    private void create_packet(CharSequence cs) {
        ((Buffer) packet).clear(); // Explicit cast for cross compatibility with JDK9
        packet.put((byte) 166);
        packet.put((byte) 0);
        for (int i = 0; i < cs.length(); i++) {
            packet.put((byte) cs.charAt(i));
        }
        packet.put((byte) 166);
        packet.put(crc(packet));

    }

    private void transmit_packet() {
        ((Buffer) transfered).clear(); // Explicit cast for cross compatibility with JDK9
        int results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, packet, transfered, 5000L);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Packet Send Failed.", results);
        }
    }

    private boolean waitForStatus() {
        recovery += 1;
        Thread waitForStatusThread = new Thread() {
            int count = 0;

            @Override
            public void run() {
                int results = -1;
                synchronized (this) {
                    while (results != LibUsb.SUCCESS) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                        }
                        ((Buffer) transfered).clear(); // Explicit cast for cross compatibility with JDK9
                        request_status.put(0, (byte) 160);
                        if (handle == null) {
                            //If device not found and restart fails there might no longer be a handle.
                            //If this is the case, our state is ERROR_NO_DEVICE.
                            results = LibUsb.ERROR_NO_DEVICE;
                        } else {
                            results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, request_status, transfered, 5000L);
                        }
                        switch (results) {
                            case LibUsb.ERROR_NO_DEVICE:
                                error("Device was not found. Attempting restart.");
                                try {
                                    close();
                                    open();
                                } catch (LibUsbException e) {
                                    error("Restart failed because: " + e.getLocalizedMessage());
                                }
                                break;
                            case LibUsb.ERROR_PIPE:
                                error("USB pipe failed.");
                                break;
                            case LibUsb.ERROR_TIMEOUT:
                                error("USB timedout.");
                                break;
                            case LibUsb.SUCCESS:
                                notify();
                                return;// Okay, we're back on track.
                        }
                        count++;
                        if (count >= 15) {
                            throw new LibUsbException("Failed to recover from USB errors.", LibUsb.ERROR_TIMEOUT);
                        }
                    }
                }
            }
        };
        waitForStatusThread.start();
        synchronized (waitForStatusThread) {
            try {
                error("A problem getting status was detected. We will wait for the device.");
                waitForStatusThread.wait();
            } catch (Exception e) {
                return false;
            }
        }
        //Status must have been successful.
        return true;
    }

    private void update_status() {
        int results;

        ((Buffer) transfered).clear(); // Explicit cast for cross compatibility with JDK9
        request_status.put(0, (byte) 160);
        results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_WRITE, request_status, transfered, 5000L);
        //While the device is fast moving this packet will not be accepted.

        if (results < LibUsb.SUCCESS) {
            boolean recoverable = waitForStatus(); //put in holding pattern.
            if (!recoverable) {
                throw new LibUsbException("Status Request Failed.", results);
            }
        }
        if (handle == null) {
            throw new LibUsbException("Status Request Failed.", results);
        }
        ByteBuffer read_buffer = ByteBuffer.allocateDirect(6);
        results = LibUsb.bulkTransfer(handle, K40_ENDPOINT_READ, read_buffer, transfered, 5000L);
        if (results < LibUsb.SUCCESS) {
            //If the read failed, after successfully sending request, we say status is error.
            status = STATUS_DEVICE_ERROR;
            error("Status read failed. After 160 sent.");
            return;
        }

        if (transfered.get(0) == 6) {
            int next_0 = read_buffer.get(0) & 0xFF;
            int next_1 = read_buffer.get(1) & 0xFF;
            int next_2 = read_buffer.get(2) & 0xFF;
            int next_3 = read_buffer.get(3) & 0xFF;
            int next_4 = read_buffer.get(4) & 0xFF;
            int next_5 = read_buffer.get(5) & 0xFF;

            /*
        //Other than byte 1 being status these aren't known. They change
        //sometimes, but what they mean is somewhat mysterious.
        System.out.println(String.format("%d %d %d %d %d %d", next_0, next_1, next_2, next_3, next_4, next_5));
             */
            byte_0 = next_0;
            status = next_1;
            byte_2 = next_2;
            byte_3 = next_3;
            byte_4 = next_4;
            byte_5 = next_5;
        }
    }

    @Override
    public void wait_for_finish() {
        wait(STATUS_FINISH);
    }

    @Override
    public void wait_for_ok() {
        wait(STATUS_OK);
    }

    public void wait(int state) {
        while (true) {
            update_status();
            if (status == state) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

    //************************
    //USB Functions.
    //************************
    private void findK40() throws LibUsbException {
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
        throw new LibUsbException("Device was not found.", LibUsb.ERROR_NO_DEVICE);
    }

    private void openContext() throws LibUsbException {
        context = new Context();
        int results = LibUsb.init(context);

        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not initialize.", results);
        }
    }

    private void closeContext() {
        if (context != null) {
            LibUsb.exit(context);
            context = null;
        }
    }

    private void closeHandle() {
        if (handle != null) {
            LibUsb.close(handle);
            handle = null;
        }
    }

    private void openHandle() {
        handle = new DeviceHandle();
        int results = LibUsb.open(device, handle);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not open device handle.", results);
        }
    }

    private void claimInterface() {
        int results = LibUsb.claimInterface(handle, interface_number);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not claim the interface.", results);
        }
    }

    private void releaseInterface() {
        if (handle != null) {
            LibUsb.releaseInterface(handle, interface_number);
        }
    }

    private void detatchIfNeeded() {
        if (LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER)
                && LibUsb.kernelDriverActive(handle, interface_number) != 0) {
            int results = LibUsb.detachKernelDriver(handle, interface_number);
            if (results < LibUsb.SUCCESS) {
                throw new LibUsbException("Could not remove kernel driver.", results);
            }
            kernel_detached = true;
        }
    }

    private void reattachIfNeeded() {
        int results = LibUsb.attachKernelDriver(handle, interface_number);
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Could not reattach kernel driver", results);
        }
        kernel_detached = false;
    }

    private void checkConfig() {
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

    /*
    This is a valid endpoint, but I don't know what it should actually do.
    This shouldn't be called.
     */
    private void get_interupt() {
        transfered.clear();
        ByteBuffer read_buffer = ByteBuffer.allocateDirect(32);
        int results = LibUsb.interruptTransfer(handle, K40_ENDPOINT_READ_I, read_buffer, transfered, 500L);
        if (results == LibUsb.ERROR_TIMEOUT) {
            return;
        }
        if (results < LibUsb.SUCCESS) {
            throw new LibUsbException("Data move failed.", results);
        }
    }

}
