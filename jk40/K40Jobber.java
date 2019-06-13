/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jk40;

import org.usb4java.LibUsbException;

/**
 *
 * @author Tat
 */
public class K40Jobber {
    
    StringBuffer buffer = new StringBuffer();

    private Thread thread = null;

    boolean is_shutdown = false;
    boolean shutdown_when_finished = false;
    K40Usb usb = new K40Usb();

    public void send(String message) {
        buffer.append(message);
    }

    public void process(String message) {
        buffer.append(message);
        pad_buffer();
    }

    public void pad_buffer() {
        int pad = buffer.length() % K40Usb.PAYLOAD_LENGTH;
        buffer.append("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".subSequence(0, pad));
    }

    public void flush() {
        send_complete_packets();
        send_incomplete_packets();
    }

    public void start() {
        if (thread != null) {
            return;
        }
        is_shutdown = false;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    usb.open();
                    while (!is_shutdown) {
                        if (send_complete_packets()) {
                            if (shutdown_when_finished) {
                                break;
                            } else {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException ex) {
                                }
                            }
                        }
                    }
                } catch (LibUsbException e) {
                    //USB broke at some point.
                }
                usb.close();
                thread = null;
                is_shutdown = false;
            }
        });
        thread.start();

    }

    public void shutdown() {
        is_shutdown = true;
    }

    public void setShutdownWhenFinished(boolean b) {
        shutdown_when_finished = b;
    }

    public int size() {
        return buffer.length();
    }

    private boolean send_complete_packets() {
        if (buffer.length() < K40Usb.PAYLOAD_LENGTH) {
            return false;
        }
        while (buffer.length() >= K40Usb.PAYLOAD_LENGTH) {
            usb.wait_for_ok();
            usb.send_packet(buffer.subSequence(0, K40Usb.PAYLOAD_LENGTH));
            buffer.delete(0, K40Usb.PAYLOAD_LENGTH);
        }
        return true;
    }

    private boolean send_incomplete_packets() {
        if (buffer.length() == 0) {
            return false;
        }
        if (buffer.length() < K40Usb.PAYLOAD_LENGTH) {
            pad_buffer();
            usb.wait_for_ok();
            usb.send_packet(buffer.subSequence(0, K40Usb.PAYLOAD_LENGTH));
            buffer.delete(0, K40Usb.PAYLOAD_LENGTH);
        }
        return true;
    }
}
