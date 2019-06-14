package jk40;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * The main purpose of this class is the cut the packets up and and track
 * various events needed for a robust laser process. There are two distinct
 * changes in this regard from LHYMICRO-GL code. The carriage return and the
 * dash (-). These are suffix elements and control the queue.
 */
public class K40Queue {

    final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    private final StringBuilder buffer = new StringBuilder();
    MockUsb usb;

    public void open() {
        usb = new MockUsb();
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

    public int size() {
        return queue.size();
    }

}
