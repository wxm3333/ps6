import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReadWriteLock;


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

class ParallelPacketWorker implements PacketWorker {
	  PaddedPrimitiveNonVolatile<Boolean> done;
	  ConcurrentHashMap<Integer, ConcurrentSkipListSet<Integer>> rTable;
	  ConcurrentHashMap<Integer, Boolean> pngTable;
	  ConcurrentHashMap<Long, Integer> histogram;
	  long totalPackets = 0;
	  WaitFreeQueue<Packet>[] queues;
	  Fingerprint fingerprint;
	  ReadWriteLock tableLock;
	  public ParallelPacketWorker(
	    PaddedPrimitiveNonVolatile<Boolean> done,
	    WaitFreeQueue<Packet>[] queues,
	    ConcurrentHashMap<Integer, ConcurrentSkipListSet<Integer>> rTable,
	    ConcurrentHashMap<Integer, Boolean> pngTable,
	    ConcurrentHashMap<Long, Integer> histogram,
	    ReadWriteLock tableLock) {
	    this.done = done;
	    this.rTable = rTable;
	    this.pngTable = pngTable;
	    this.histogram = histogram;
	    this.queues = queues;
	    fingerprint = new Fingerprint();
	    this.tableLock = tableLock;
	  }
	  
	  public void run() {
		Random rand = new Random();
	    Packet tmp = null;
	    int i = rand.nextInt(queues.length);
	    boolean locked = false;
	    while(!done.value || tmp != null) {

	      try {
	    	if (!locked){
	    		while (!queues[i].lock.tryLock()){
	    			i = rand.nextInt(queues.length);
	    		}
	    		locked = true;
	    	}
	        tmp = this.queues[i].deq();

	      } catch (EmptyException e) {
	        tmp = null;
	        queues[i].lock.unlock();
			locked=false;
			i = rand.nextInt(queues.length);

	        }
	      if(tmp != null){
	        
	        switch(tmp.type) {
	          case ConfigPacket: 
	        	tableLock.writeLock().lock();
	            processConfig(tmp);
	            tableLock.writeLock().unlock();
	            break;
	          case DataPacket:
	        	tableLock.readLock().lock();
	            processData(tmp);
	            tableLock.readLock().unlock();
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

	  private void processConfig(Packet pkt){
		    totalPackets++;
		    Config config = pkt.config;
		    int address = config.address;
		    pngTable.put( new Integer(address), new Boolean(config.personaNonGrata));
		    ConcurrentSkipListSet<Integer> permissions = rTable.get(new Integer(address));
		    if(permissions == null){
		      permissions = new ConcurrentSkipListSet<Integer>();
		    
		    }
		    for (int i = config.addressBegin; i < config.addressEnd; i++){
		    	if (config.acceptingRange) {
		    		permissions.add(new Integer(i));
		    	}
		    	else {
		    		permissions.remove(new Integer(i));
		    	}
		      
		    }
		    rTable.put(new Integer(address), permissions);
		  
		  }

		  private void processData(Packet pkt){
			  
		      totalPackets++;
		      Header header = pkt.header;
		      if (pngTable.get(header.source) == null || pngTable.get(header.source)){
		          return;
		      }
		      ConcurrentSkipListSet<Integer> permissions = rTable.get(new Integer(header.dest));
		      if(permissions == null) { 
		          //System.out.println("no permission table");
		      }else{
		        Boolean accept = permissions.contains(header.source);
		        
		        if(!accept){
		            //System.out.println("accept==false");
		            return;
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

class AwesomeParallelPacketWorker implements PacketWorker {
	  PaddedPrimitiveNonVolatile<Boolean> done;
	  ConcurrentHashMap<Integer, SkipList> rTable;
	  ConcurrentHashMap<Integer, Boolean> pngTable;
	  ConcurrentHashMap<Long, Integer> histogram;
	  long totalPackets = 0;
	  WaitFreeQueue<Packet>[] queues;
	  Fingerprint fingerprint;
	  ReadWriteLock tableLock;
	  public AwesomeParallelPacketWorker(
	    PaddedPrimitiveNonVolatile<Boolean> done,
	    WaitFreeQueue<Packet>[] queues,
	    ConcurrentHashMap<Integer, SkipList> rTable,
	    ConcurrentHashMap<Integer, Boolean> pngTable,
	    ConcurrentHashMap<Long, Integer> histogram,
	    ReadWriteLock tableLock) {
	    this.done = done;
	    this.rTable = rTable;
	    this.pngTable = pngTable;
	    this.histogram = histogram;
	    this.queues = queues;
	    fingerprint = new Fingerprint();
	    this.tableLock = tableLock;
	  }
	  
	  public void run() {
		Random rand = new Random();
	    Packet tmp = null;
	    int i = rand.nextInt(queues.length);
	    boolean locked = false;
	    while(!done.value || tmp != null) {

	      try {
	    	if (!locked){
	    		while (!queues[i].lock.tryLock()){
	    			i = rand.nextInt(queues.length);
	    		}
	    		locked = true;
	    	}
	        tmp = this.queues[i].deq();

	      } catch (EmptyException e) {
	        tmp = null;
	        queues[i].lock.unlock();
			locked=false;
			i = rand.nextInt(queues.length);

	        }
	      if(tmp != null){
	        
	        switch(tmp.type) {
	          case ConfigPacket: 
	        	tableLock.writeLock().lock();
	            processConfig(tmp);
	            tableLock.writeLock().unlock();
	            break;
	          case DataPacket:
	        	tableLock.readLock().lock();
	            processData(tmp);
	            tableLock.readLock().unlock();
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

	  private void processConfig(Packet pkt){
		    totalPackets++;
		    Config config = pkt.config;
		    int address = config.address;
		    pngTable.put( new Integer(address), new Boolean(config.personaNonGrata));
		    SkipList permissions = rTable.get(new Integer(address));
		    if(permissions == null){
		    	if (config.acceptingRange) {
		    		permissions = new SkipList(config.addressBegin, config.addressEnd, true);
		    	}
		    	else {
		    		permissions = new SkipList(config.addressBegin, config.addressEnd, false);
		    	}
		    
		    }
		    else {
		    	if (config.acceptingRange) {
		    		permissions.add(config.addressBegin, config.addressEnd);
		    	}
		    	else {
		    		permissions.subtract(config.addressBegin, config.addressEnd);
		    	}
		    }

		    rTable.put(new Integer(address), permissions);
		  
		  }

		  private void processData(Packet pkt){
			  
		      totalPackets++;
		      Header header = pkt.header;
		      if (pngTable.get(header.source) == null || pngTable.get(header.source)){
		          return;
		      }
		      SkipList permissions = rTable.get(new Integer(header.dest));
		      if(permissions == null) { 
		          //System.out.println("no permission table");
		      }else{
		        Boolean accept = permissions.contains(header.source);
		        if(!accept){
		            //System.out.println("accept==false");
		            return;
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