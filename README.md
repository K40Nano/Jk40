# Jk40

K40 java support from scratch.

(Needs some testing, might work).

Synch and Asynch support. Stores basic information about the current queue being processed which can be called if working in async mode.

send(LHYMICRO-GL): Adds LHYMICRO-GL code to the queue for processing.
process(LHYMICRO-GL): Adds a padded packet for immediate sending. 
flush(): (blocking) sends all pending packets
open(): (blocking) opens the channel to the device.
close(): (blocking) closes the channel to the device.
start(): run the queue processor in asynch mode.
shutdown(): stop the queue processor.
size(): give the current size of the standing queue.
wait_for_finish(): wait until the status finish flag is sent.

Still a few things worth looking into. And this will still require some understanding of how the underlying protocols work. The wait_for_finish routine is quite problematic. Because rather than just feed everything in the queue constantly into the USB with resends etc, it requires that the queue be generally finished because you can't necessarily jump from a speedcode compact mode to a different speedcode without jobification. And there's something clear to the idea of doing this at the same level as the rest of this. This would allow for setting the speed to something else on the fly without telling anybody about the compact mode etc.

Goals
---

Ideally this should allow:

* set_speed(v);
* move(x,y)
* up()
* down()
* home()
* lock()
* unlock()
* pause()
* resume()

And a bunch of state queries, like how large is the current queue, where is the laser location, which job is it currently on, etc.

And this likely still needs porting to C++ so it needs to be kept simple, so thinking before acting is better.
