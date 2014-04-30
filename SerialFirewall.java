
class SerialFirewall {
	public static void main(String[] args) {

	    final int numMilliseconds = Integer.parseInt(args[0]);    
	    final int numAddressLog = Integer.parseInt(args[1]);
	    final int numTrainsLog = Integer.parseInt(args[2]);
	    final double meanTrainSize = Float.parseFloat(args[3]);
	    final double meanTrainsPerComm = Float.parseFloat(args[4]);
	    final int meanWindow = Integer.parseInt(args[5]);
	    final int meanCommsPerAddress = Integer.parseInt(args[6]);
	    final int meanWork = Integer.parseInt(args[7]);
	    final double configFraction = Float.parseFloat(args[8]);
	    final double pngFraction= Float.parseFloat(args[9]);
	    final double acceptingFraction = Float.parseFloat(args[10]);
	    
	    final int maxBucketSize = 5;

	    @SuppressWarnings({"unchecked"})
	    StopWatch timer = new StopWatch();
	    PacketGenerator source = new PacketGenerator(numAddressLog, numTrainsLog,
	    											 meanTrainSize, meanTrainsPerComm,
	    											 meanWindow, meanCommsPerAddress, 
	    											 meanWork, configFraction, pngFraction, 
	    											 acceptingFraction);
	    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
	    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
	    boolean[] pngTable = new boolean[1<<numAddressLog];
	    SerialHashTable<Packet> rTable = new SerialHashTable<Packet>(1, maxBucketSize);
	    
	    for( int i = 0; i < (1<<(int)Math.ceil(numAddressLog*1.5)); i++ ) {
	      Packet pkt = source.getConfigPacket();
	      table.add(pkt.mangleKey(), pkt.getItem());
	    }
	    SerialHashPacketWorker workerData = new SerialHashPacketWorker(done, source, table);
	    Thread workerThread = new Thread(workerData);
	    
	    workerThread.start();
	    timer.startTimer();
	    try {
	      Thread.sleep(numMilliseconds);
	    } catch (InterruptedException ignore) {;}
	    done.value = true;
	    memFence.value = true;
	    try {
	      workerThread.join();
	    } catch (InterruptedException ignore) {;}      
	    timer.stopTimer();
	    final long totalCount = workerData.totalPackets;
	    System.out.println("count: " + totalCount);
	    System.out.println("time: " + timer.getElapsedTime());
	    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
	  }
}

class SerialHashPacketWorker implements Runnable {
	  PaddedPrimitiveNonVolatile<Boolean> done;
	  final PacketGenerator source;
	  HashTable<T> pngTable;
	  HashTable<T> rTable;
	  long totalPackets = 0;
	  long residue = 0;
	  Fingerprint fingerprint;
	  public SerialHashPacketWorker(
	    PaddedPrimitiveNonVolatile<Boolean> done, 
	    PacketGenerator source,
	    SerialHashTable<Packet> table) {
	    this.done = done;
	    this.source = source;
	    this.table = table;
	    fingerprint = new Fingerprint();
	  }
	  
	  public void run() {
	    Packet pkt;
	    while( !done.value ) {
	      totalPackets++;
	      pkt = source.getPacket();
	      residue += fingerprint.getFingerprint(pkt.getItem().iterations,pkt.getItem().seed);
	      switch(pkt.getType()) {
	        case Add: 
	          table.add(pkt.mangleKey(),pkt.getItem());
	          break;
	        case Remove:
	          table.remove(pkt.mangleKey());
	          break;
	        case Contains:
	          table.contains(pkt.mangleKey());
	          break;
	      }
	    }
	  }  
	}

class PacketProcessor<T> {
	HashTable<T> pngTable;
	HashTable<T> rTable;
	public PacketProcessor(HashTable<T> t1, HashTable<T> t2) {
		pngTable = t1;
		rTable = t2;		
	}
	public void process(Packet pkt) {
		
	}
}