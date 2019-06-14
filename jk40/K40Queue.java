package jk40;

import java.util.ArrayList;

/**
 * The main purpose of this task is the cut the packets up and and track the
 * various events needed for that process. Adding a null to the queue will
 * signal that it must wait for the laser to finish the current work before
 * sending anything additional.
 *
 * add_default automatically pads the packets so they can be sent immediately as
 * you would want for default mode commands. When a wait for finish null is
 * sent, the current buffer is padded so the packet will be sent. The wait is
 * performed and then it moves along to the next contiguous block of code.
 *
 */
public class K40Queue {

    final ArrayList<String> queue = new ArrayList<>();
    final StringBuilder buffer = new StringBuilder();
    MockUsb usb;

    public void open() {
        usb = new MockUsb();
        usb.open();
    }

    public void close() {
        usb.close();
        usb = null;
    }

    public void add_default(String element) {
        int len = K40Usb.PAYLOAD_LENGTH;
        int pad = (len - (buffer.length() % len)) % len;
        element += "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".subSequence(0, pad);
        queue.add(element);
    }

    private void pad_buffer() {
        int len = K40Usb.PAYLOAD_LENGTH;
        int pad = (len - (buffer.length() % len)) % len;
        buffer.append("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".subSequence(0, pad));
    }

    public void add(String element) {
        queue.add(element);
    }

    void wait_for_finish() {
        add(null);
    }

    public void execute() {
        while (true) {
            if (!queue.isEmpty()) {
                int v = 0;
                for (int s = queue.size(); v < s; v++) {
                    String element = queue.get(v);
                    if (element == null) {
                        break;
                    }
                    buffer.append(element);
                }
                queue.subList(0, v).clear();
                pad_buffer();
            } //moved as much of the queue to the buffer as we could.
            while (buffer.length() >= K40Usb.PAYLOAD_LENGTH) {
                if (usb != null) {
                    usb.wait_for_ok();
                    usb.send_packet(buffer.subSequence(0, K40Usb.PAYLOAD_LENGTH));
                    buffer.delete(0, K40Usb.PAYLOAD_LENGTH);
                }

            } //all sendable packets sent.
            if (queue.isEmpty()) {
                break; //We finished.
            }
            if (queue.get(0) == null) { //a wait was requested.
                queue.remove(0);
                usb.wait_for_finish();
            }
        }

    }

    public int size() {
        return queue.size();
    }

}
