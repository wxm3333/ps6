import java.util.*;
class SerialFirewall{
  public static void main(String[] args) {
		if (args.length==2){
			System.out.println(args[0]+" "+args[1]);
			if (args[0].equals("1")){
				args=new String[]{"11","12","5","1","3","3","3822","0.24","0.04","0.96","2000",args[1]};
			}
			else if(args[0].equals("2")){
				args=new String[]{"12","10","1","3","3","1","2644","0.11","0.09","0.92","2000",args[1]};
			}
			else if(args[0].equals("3")){
				args=new String[]{"12","10","4","3","6","2","1304","0.10","0.03","0.90","2000",args[1]};
			}
			else if(args[0].equals("4")){
				args=new String[]{"14","10","5","5","6","2","315","0.08","0.05","0.90","2000",args[1]};
			}
			else if(args[0].equals("5")){
				args=new String[]{"15","14","9","16","7","10","4007","0.02","0.10","0.84","2000",args[1]};
			}
			else if(args[0].equals("6")){
				args=new String[]{"15","14","9","10","9","9","7125","0.01","0.20","0.77","2000",args[1]};
			}
			else if(args[0].equals("7")){
				args=new String[]{"15","15","10","13","8","10","5328","0.04","0.18","0.80","2000",args[1]};
			}
			else if(args[0].equals("8")){
				args=new String[]{"16","14","15","12","9","5","8840","0.04","0.19","0.76","2000",args[1]};
			}
		}
		final int numAddressLog = Integer.parseInt(args[0]);
		final int numTrainsLog=Integer.parseInt(args[1]);
	    final double meanTrainSize=Double.parseDouble(args[2]);
	    final double meanTrainsPerComm=Double.parseDouble(args[3]);
	    final int meanWindow=Integer.parseInt(args[4]);
	    final int meanCommsPerAddress=Integer.parseInt(args[5]);
	    final int meanWork=Integer.parseInt(args[6]);
	    final double configFraction=Double.parseDouble(args[7]);
	    final double pngFraction=Double.parseDouble(args[8]);
	    final double acceptingFraction=Double.parseDouble(args[9]);
	    final int numMilliseconds = Integer.parseInt(args[10]);
		//System.out.println("addresslog: "+numAddressLog);
		//System.out.println("meanwork: "+meanWork);
		//System.out.println("acceptingFraction: "+acceptingFraction);

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
    SerialPacketWorker workerData = new SerialPacketWorker(done, source, rTable, pngTable, histogram);
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

