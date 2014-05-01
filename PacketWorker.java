import java.util.*;
import org.deuce.Atomic;

public interface PacketWorker<T> extends Runnable {
  public void run();
}

class SerialPacketWorker implements PacketWorker {
  PaddedPrimitiveNonVolatile<Boolean> done;
  final PacketGenerator source;
  Hashtable<Integer, Hashtable<Integer, Boolean>> rTable;
  Hashtable<Integer, Boolean> pngTable;
  Hashtable<Long, Integer> histogram;
  long totalPackets = 0;
  Fingerprint fingerprint;
  public SerialPacketWorker(
    PaddedPrimitiveNonVolatile<Boolean> done, 
    PacketGenerator source,
    Hashtable<Integer, Hashtable<Integer, Boolean>> rTable,
    Hashtable<Integer, Boolean> pngTable,
    Hashtable<Long, Integer> histogram) {
    this.done = done;
    this.source = source;
    this.rTable = rTable;
    this.pngTable = pngTable;
    this.histogram = histogram;
    fingerprint = new Fingerprint();
  }
  
  public void run() {
    while( !done.value ) {
      Packet pkt = source.getPacket();
      switch(pkt.type) {
        case ConfigPacket: 
          processConfig(pkt);
          break;
        case DataPacket: 
          processData(pkt);
          break;
        default:
          break;
      }
    }
  }  
  private void processConfig(Packet pkt){
    totalPackets++;
    Config config = pkt.config;
    int address = config.address;
    pngTable.put( new Integer(address), new Boolean(config.personaNonGrata));
    Hashtable<Integer, Boolean> permissions = rTable.get(new Integer(address));
    if(permissions == null){
      permissions = new Hashtable<Integer, Boolean>();
    
    }
    for (int i = config.addressBegin; i < config.addressEnd; i++){
        permissions.put(new Integer(i), new Boolean(config.acceptingRange));
      
    }
    rTable.put(new Integer(address), permissions);
  
  }

  private void processData(Packet pkt){

      totalPackets++;
      Header header = pkt.header;
      if (pngTable.get(header.source) == null || pngTable.get(header.source)){
          return;
      }
      Hashtable<Integer, Boolean> permissions = rTable.get(new Integer(header.dest));
      if(permissions == null) { 
          //System.out.println("no permission table");
      }else{
        Boolean accept = permissions.get(header.source);
        if(accept == null){
          //System.out.println("no accept specified");
          return;
        } else{
          if(!accept){
            //System.out.println("accept==false");
            return;
          }
        }
      }

      long f = fingerprint.getFingerprint(pkt.body.iterations,pkt.body.seed);
      int count = 0;
      Integer c = histogram.get(f);
      if (c != null){
        count = c;
      }
      count += 1;
      histogram.put(f, count);
  }
}

class STMPacketWorker implements PacketWorker {
  PaddedPrimitiveNonVolatile<Boolean> done;
  Hashtable<Integer, Hashtable<Integer, Boolean>> rTable;
  Hashtable<Integer, Boolean> pngTable;
  Hashtable<Long, Integer> histogram;
  long totalPackets = 0;
  WaitFreeQueue<Packet> queue;
  Fingerprint fingerprint;
  public STMPacketWorker(
    PaddedPrimitiveNonVolatile<Boolean> done,
    WaitFreeQueue<Packet> queue,
    Hashtable<Integer, Hashtable<Integer, Boolean>> rTable,
    Hashtable<Integer, Boolean> pngTable,
    Hashtable<Long, Integer> histogram) {
    this.done = done;
    this.rTable = rTable;
    this.pngTable = pngTable;
    this.histogram = histogram;
    this.queue = queue;
    fingerprint = new Fingerprint();
  }
  
  public void run() {
    Packet tmp = null;
    while(!done.value || tmp != null) {

      try {
        this.queue.lock.lock();
        tmp = this.queue.deq();
        this.queue.lock.unlock();

      } catch (EmptyException e) {

        tmp = null;

        this.queue.lock.unlock();

        }
      if(tmp != null){
        
        switch(tmp.type) {
          case ConfigPacket: 
            processConfig(tmp);
            break;
          case DataPacket: 
            processData(tmp);
            break;
          default:
            System.out.println("ERR: Packet has no type");
            break;
        }
      }
      else{
      }
    }
  }

  @Atomic
  private void processConfig(Packet pkt){
	    totalPackets++;
	    Config config = pkt.config;
	    int address = config.address;
	    pngTable.put( new Integer(address), new Boolean(config.personaNonGrata));
	    Hashtable<Integer, Boolean> permissions = rTable.get(new Integer(address));
	    if(permissions == null){
	      permissions = new Hashtable<Integer, Boolean>();
	    
	    }
	    for (int i = config.addressBegin; i < config.addressEnd; i++){
	        permissions.put(new Integer(i), new Boolean(config.acceptingRange));
	      
	    }
	    rTable.put(new Integer(address), permissions);
	  
	  }

	  private void processData(Packet pkt){

	      totalPackets++;
	      Header header = pkt.header;
	      if (pngTable.get(header.source) == null || pngTable.get(header.source)){
	          return;
	      }
	      Hashtable<Integer, Boolean> permissions = rTable.get(new Integer(header.dest));
	      if(permissions == null) { 
	          //System.out.println("no permission table");
	      }else{
	        Boolean accept = permissions.get(header.source);
	        if(accept == null){
	          //System.out.println("no accept specified");
	          return;
	        } else{
	          if(!accept){
	            //System.out.println("accept==false");
	            return;
	          }
	        }
	      }

	      long f = fingerprint.getFingerprint(pkt.body.iterations,pkt.body.seed);
	      int count = 0;
	      Integer c = histogram.get(f);
	      if (c != null){
	        count = c;
	      }
	      count += 1;
	      histogram.put(f, count);
	  }
}


