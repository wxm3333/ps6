import java.util.*;
class STMFirewall{
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
    final double pngFraction = Float.parseFloat(args[9]);    
    final double acceptingFraction = Float.parseFloat(args[10]);
    final int maxQueueSize = 256;


    @SuppressWarnings({"unchecked"})
    StopWatch timer = new StopWatch();
    PacketGenerator source = new PacketGenerator(numAddressLog,
                                                  numTrainsLog,
                                                  meanTrainSize,
                                                  meanTrainsPerComm,
                                                  meanWindow,
                                                  meanCommsPerAddress,
                                                  meanWork,
                                                  configFraction,
                                                  pngFraction,
                                                  acceptingFraction);

    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
    Hashtable<Integer, Hashtable<Integer, Boolean>> rTable = new Hashtable <Integer, Hashtable<Integer, Boolean>>();
    Hashtable<Integer, Boolean> pngTable = new Hashtable<Integer, Boolean>();
    Hashtable<Long, Integer>  histogram = new Hashtable<Long, Integer> ();

    WaitFreeQueue<Packet> queue = new WaitFreeQueue<Packet>(maxQueueSize);
    
    STMPacketWorker workerData = new STMPacketWorker(done, queue, rTable, pngTable, histogram);
    Thread workerThread = new Thread(workerData);
    for(int j = 0; j < (1 <<(int)(numAddressLog * 1.5)); j++){
        Packet pkt = source.getConfigPacket();
        Config config = pkt.config;
        int address = config.address;
        pngTable.put(new Integer(address), new Boolean(config.personaNonGrata));
        Hashtable<Integer, Boolean> permissions = rTable.get(new Integer(address));
        if(permissions == null){
          permissions = new Hashtable<Integer, Boolean>();
        }
        for (int i = config.addressBegin; i < config.addressEnd; i++){
            permissions.put(new Integer(i), new Boolean(config.acceptingRange));
          
        }
        rTable.put(new Integer(address), permissions);
      }
    Dispatcher d = new Dispatcher(done, source, queue);

    timer.startTimer();
    Thread dispatcherThread = new Thread(d);
    dispatcherThread.start();

    workerThread.start();
    try {
      Thread.sleep(numMilliseconds);
    } catch (InterruptedException ignore) {;}
    done.value = true;
    memFence.value = true;
    try {
      workerThread.join();
    } catch (InterruptedException ignore) {;}      
    try {
      dispatcherThread.join();
    } catch (InterruptedException ignore) {;}      


    timer.stopTimer();
    final long totalCount = workerData.totalPackets;
    System.out.println("count: " + totalCount);
    System.out.println("time: " + timer.getElapsedTime());
    System.out.println(totalCount/timer.getElapsedTime() + " pkts / ms");
  }
  
}


