
public interface ParallelDispatcher extends Runnable{
	public void run();
}
class ParallelPipelineDispatcher implements ParallelDispatcher {
	private WaitFreeQueue<Packet>[] queues;
	private PacketGenerator source;
	private PaddedPrimitiveNonVolatile<Boolean> done;
	public int totalPackets;
	private int numConfig;

	public ParallelPipelineDispatcher(WaitFreeQueue<Packet>[] queues, PacketGenerator source,
			PaddedPrimitiveNonVolatile<Boolean> done, int numConfig) {
		this.numConfig = numConfig;
		this.queues = queues;
		this.source = source;
		this.done = done;
		this.totalPackets = 0;
	}

	public void run() {
		Packet p;
		int configC = 0;
		int dataC = 0;
		while (!done.value) {
			p = source.getPacket();
			switch (p.type) {
			case ConfigPacket:
				configC ++;
				try {
					queues[(configC) % numConfig + queues.length - numConfig].enq(p);
					totalPackets += 1;
					p = null;
					break;
				} catch (FullException e) {
				}
			
			break;
			case DataPacket:
				dataC ++;
				try {
					queues[dataC % (queues.length)].enq(p);
					totalPackets += 1;
					p = null;
					break;
				} catch (FullException e) {
				}						
			break;

			}
			
		}
	}
}

class ParallelOneDispatcher implements ParallelDispatcher {
	private WaitFreeQueue<Packet>[] queues;
	private PacketGenerator source;
	private PaddedPrimitiveNonVolatile<Boolean> done;
	public int totalPackets;

	public ParallelOneDispatcher(WaitFreeQueue<Packet>[] queues, PacketGenerator source,
			PaddedPrimitiveNonVolatile<Boolean> done) {
		this.queues = queues;
		this.source = source;
		this.done = done;
		this.totalPackets = 0;
	}

	public void run() {
		Packet p;
		while (!done.value) {
			for (int i=0; i<queues.length; i++){
				p = source.getPacket();
				try {
			          this.queues[i].enq(p);
			        } catch (FullException e) {;}
			}
			
		}
	}
}
