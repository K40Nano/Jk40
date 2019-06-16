package jk40;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * The purpose of this class is the cut the packets up and and perform events 
 * needed for a robust laser process. The carriage return and the dash (-) are
 * not used in LHYMICRO-GL code. These have been repurposed to mean fill in the
 * remaining characters of this packet, and wait for the finish signal
 * respectively. This permits a single stream of text data to unambiguously 
 * control the laser.
 * 
 * 
 */
public class K40Queue {
    public boolean mock = true;

    final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    private final StringBuilder buffer = new StringBuilder();
    BaseUsb usb;

    public void open() {
        if (mock) {
            usb = new MockUsb();
        } else {
            usb = new K40Usb();
        }
        usb.open();
    }

    public void close() {
        usb.close();
        usb = null;
    }

    private void pad_buffer() {
        int len = K40Usb.PAYLOAD_LENGTH;
        int pad = (len - (buffer.length() % len)) % len;
        buffer.append("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".subSequence(0, pad));
    }

    public void add(String element) {
        queue.add(element);
    }

    void add_wait() {
        add("-\n");
    }

    public void execute() {
        while (true) {
            boolean wait = false;
            while (!queue.isEmpty()) {
                String element = queue.poll();
                if (element.endsWith("-\n")) {
                    buffer.append(element.subSequence(0, element.length() - 2));
                    pad_buffer();
                    wait = true;
                    break;
                } else if (element.endsWith("\n")) {
                    buffer.append(element.subSequence(0, element.length() - 1));
                    pad_buffer();
                } else {
                    buffer.append(element);
                }
            } //moved as much of the queue to the buffer as we could.
            while (buffer.length() >= K40Usb.PAYLOAD_LENGTH) {
                if (usb != null) {
                    usb.wait_for_ok();
                    usb.send_packet(buffer.subSequence(0, K40Usb.PAYLOAD_LENGTH));
                    buffer.delete(0, K40Usb.PAYLOAD_LENGTH);
                }
            } //all sendable packets sent.
            if (wait) {
                usb.wait_for_finish();
                wait = false;
            }
            if (queue.isEmpty()) {
                break; //We finished.
            }
        }
    }
}
