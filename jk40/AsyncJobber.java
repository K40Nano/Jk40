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
public class AsyncJobber extends K40Queue {

    private Thread thread = null;

    boolean is_shutdown = false;
    boolean shutdown_when_finished = false;

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
                        execute();
                        if (shutdown_when_finished) {
                            break;
                        } else {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException ex) {
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

}
