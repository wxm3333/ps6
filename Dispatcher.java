import java.util.ArrayList;

public class Dispatcher implements Runnable{

  public WaitFreeQueue<Packet> queue;
  PaddedPrimitiveNonVolatile<Boolean> done;
  final PacketGenerator pkt;
  public Dispatcher(
    PaddedPrimitiveNonVolatile<Boolean> done,
    PacketGenerator pkt,
    WaitFreeQueue<Packet> queue) {
    this.done = done;
    this.pkt = pkt;
    this.queue = queue;
  }
  public void run() {
    Packet tmp;
    while(!done.value ) {
        tmp = pkt.getPacket();
        try {
          this.queue.enq(tmp);
        } catch (FullException e) {;}
    }
  }

 }

