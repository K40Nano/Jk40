package jk40;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import static jk40.K40Usb.PAYLOAD_LENGTH;
import static jk40.K40Usb.STATUS_FINISH;
import static jk40.K40Usb.STATUS_OK;
import static jk40.K40Usb.STATUS_PACKET_REJECTED;
import org.usb4java.LibUsbException;

public class WinUsb implements BaseUsb {

    CH341DLL lib = null;
    int index = 0;
    int status = 0;
    private final ByteBuffer packet = ByteBuffer.allocateDirect(32);
    private final ByteBuffer read_buffer = ByteBuffer.allocateDirect(6);

    public WinUsb(int index) {
        this.index = index;
        if (Platform.isWindows()) {
            lib = (CH341DLL) Native.load("CH341DLL.dll", CH341DLL.class);
        }
    }

    public interface CH341DLL extends Library {
        //Function CH341OpenDevice(iIndex: cardinal): integer; Stdcall; external 'CH341DLL.DLL';
        public int CH341OpenDevice(int index);
        //procedure CH341CloseDevice(iIndex: cardinal); Stdcall; external 'CH341DLL.DLL';
        public void CH341CloseDevice(int index);
        //Function CH341EppWriteData(iIndex:cardinal; iBuffer:pvoid; ioLength:PULONG ):boolean;Stdcall; external 'CH341DLL.DLL';
        public int CH341EppWriteData(int index, byte[] buffer, int length);
        //Function CH341GetStatus(iIndex:cardinal; iStatus: PULONG ): boolean; Stdcall; external 'CH341DLL.DLL';
        public int CH341GetStatus(int index, byte[] obuf);
        //Function CH341GetVerIC(iIndex:cardinal):cardinal;Stdcall; external 'CH341DLL.DLL';
        public int CH341GetVerIC(int index);
        //Function CH341InitParallel(iIndex: cardinal; iMode :cardinal): boolean; Stdcall; external 'CH341DLL.DLL';
        public int CH341InitParallel(int index, int mode); // USE 1. iMode: 0 = EPP/EPP V1.7 ; 1 = EPP V1.9, 2 = MEM
    }

    @Override
    public void open() {
        if (lib == null) {
            return;
        }
        lib.CH341OpenDevice(index);
        lib.CH341InitParallel(index, 1);
    }

    @Override
    public void close() {
        if (lib == null) {
            return;
        }
        lib.CH341CloseDevice(index);
    }
    
    private void update_status() {
        byte[] obuf = new byte[6];
        lib.CH341GetStatus(index, obuf);
        status = obuf[1];
    }

    @Override
    public void send_packet(CharSequence cs) {
        if (lib == null) {
            return;
        }
        if (cs.length() != PAYLOAD_LENGTH) {
            throw new LibUsbException("Packets must be exactly " + PAYLOAD_LENGTH + " bytes.", 0);
        }
        create_packet(cs);
        int count = 0;
        do {
            transmit_packet();
           
            if (count >= 50) {
                throw new LibUsbException("All packets are being rejected.", 0);
            }
            count++;
        } while (status == STATUS_PACKET_REJECTED);
    }
    
    private void create_packet(CharSequence cs) {
        ((Buffer) packet).clear(); // Explicit cast for cross compatibility with JDK9
        packet.put((byte) 0);
        for (int i = 0; i < cs.length(); i++) {
            packet.put((byte) cs.charAt(i));
        }
        packet.put(K40Usb.crc(packet));
    }

    private void transmit_packet() {
        lib.CH341EppWriteData(index, packet.array(), packet.capacity());
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
}