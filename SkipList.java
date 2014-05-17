import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;


public class SkipList {
	private ConcurrentSkipListMap<Integer,Integer> ranges = new ConcurrentSkipListMap<Integer,Integer>();

	public SkipList(int addressBegin, int addressEnd, boolean acceptingRange) {
		//assuming addresses are fine if not stated otherwise
		if(acceptingRange){
			ranges.put(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
		else{
			ranges.put(Integer.MIN_VALUE, addressBegin);
			ranges.put(addressEnd, Integer.MAX_VALUE);

		}
	}

	public void add(int addressBegin, int addressEnd) {
		Entry<Integer, Integer> entry = ranges.floorEntry(addressBegin);
		if(entry.getValue()<addressBegin){
			entry=ranges.higherEntry(entry.getValue());
			ranges.put(addressBegin, addressEnd);
		}
		else if(entry.getValue()<=addressEnd){
			ranges.put(entry.getKey(), addressEnd);
			entry=ranges.higherEntry(entry.getValue());
		}
		else{
			entry=ranges.higherEntry(entry.getValue());
		}
		//clean up extra ranges
		while(entry!=null&&entry.getValue()<addressEnd){
			ranges.remove(entry.getKey());
			entry=ranges.higherEntry(entry.getKey());
		}
		if(entry!=null&&ranges.floorEntry(addressBegin).getValue()>=entry.getKey()){
			ranges.remove(entry.getKey());
			ranges.put(ranges.floorEntry(addressBegin).getKey(), entry.getValue());
		}
	}

	public void subtract(int addressBegin, int addressEnd) {
		Entry<Integer, Integer> entry = ranges.floorEntry(addressBegin);
		if(entry.getValue()>addressEnd){// if placing range inside of an old range 
			ranges.put(addressEnd, entry.getValue());
			if(entry.getKey()==addressBegin){
				ranges.remove(entry.getKey());
			}
			else{
				ranges.put(entry.getKey(),addressBegin);
			}
		}
		else{
			entry=ranges.higherEntry(entry.getKey());
			while(entry.getValue()<addressEnd){
				ranges.remove(entry.getKey());
				entry=ranges.higherEntry(entry.getKey());
			}
			if(entry.getKey()<addressEnd){
				ranges.put(addressEnd, entry.getValue());
				ranges.remove(entry.getKey());
			}
		}
	}


	public boolean contains(int source) {
		Entry<Integer, Integer> entry = ranges.firstEntry();
		//System.out.println(source);
		while(entry.getValue()<source){
			//System.out.println("entry "+Integer.toString(entry.getKey())+" "+Integer.toString(entry.getValue()));
			entry=ranges.higherEntry(entry.getKey());
		}
		if(entry.getKey()<source){
			return true;
		}
		return false;
	}
	public void print() {
		System.out.println("ranges");
		for(int k:ranges.keySet()){
			System.out.println(Integer.toString(k)+" - "+Integer.toString(ranges.get(k)));
		}

	}
}
