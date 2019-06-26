package jk40;

public interface BaseUsb {

    void open();

    void close();

    void wait_for_ok();

    void wait_for_finish();

    void send_packet(CharSequence s);
}
