# Jk40

K40 java support from scratch.

The K40Usb class connects to through the usb device, does crc generation, and verifies the state are correct and sends 30 byte packets to the machine.

* open()
* close()
* send_packet();
* wait(), wait_for_ok(), wait_for_finished();

---

The K40Queue queues up a bunch of micro jobs. These can be the two things, sending packets, or waiting for them to finish. Both are encoded in Strings. These can be used to queue up any sized job, even those that wouldn't be accepted as a valid .EGV file. The commands do not need to be concatinated. It does this itself when it processes the queue to build the buffer.

The wait_for_finish() is encoded as "-\n". "-" means wait for finish. And "\n" means pad the packet for sending. This means that any events that can be done with the device can be encoded as a string.

---

The K40Device builds LHYMICRO-GL code and tracks the expected machine state. It's by far the biggest and most important class. And will be the main jumping off point for building commands. These will be sent to the queue.

---

This project has currently exists as the main functions of a driver in https://github.com/t-oster/LibLaserCut (it might still only exist at https://github.com/tatarize/LibLaserCut, but it's a pretty killer feature and there's an open pull request (circa 6/17/19)). LibLaserCut is the main library of Visicut ( https://github.com/tatarize/LibLaserCut ) which I have now successfully used to control my K40 with stock controller. The LibLaserCut driver is LGPL licensed. The code here is MIT licensed.
