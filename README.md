# Jk40

K40 java support from scratch.
(Needs some testing, might work).
Synch and Asynch support. Asynch hasn't been tested yet.

The K40Usb class is does connections to through the Usb device, crc generation and packet resending on failure, and verifies the states is correct and sends 30 byte packets to the machine.
* open()
* close()
* send_packet();
* wait(), wait_for_ok(), wait_for_finished();

The K40Queue queues up a bunch of micro jobs. These can be the two things the machine does, sending packets, or waiting for it to finish. Both are encoded in Strings. These can be added ad infinitum, to queue up any sized job. Also, the commands do not need to be concatinated. It will do this itself when it puts them into the buffer. Neither the queue or buffer need to be run at all. The wait for finish is encoded as a null string, but it might be better to simply encode it as a carriage return "\n" or some other character. But, then there'd be no method of telling static move commands I<Direction><Distance>S1P commands from other methods. These are usually not done in EGV files rather opting to string all commands through resets and only using one job file.

The K40Device builds LHYMICRO-GL code and tracks the machine state. It takes a bunch of easy commands that end up producing these codes. It allows for two modes, default and compact. It does not use the reset methodology, opting instead to finish each time it performs a move. It resets the speed each time. It does not allow for non-laser cut moves at speed. 

The current goal for this project is to add this functionality to https://github.com/t-oster/LibLaserCut or something similar as a proof of concept. 
