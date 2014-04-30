import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface HashTable<T> {
  public void add(int key, T x);
  public boolean remove(int key);
  public boolean contains(int key);
}

class SerialHashTable<T> implements HashTable<T> {
  private SerialList<T,Integer>[] table;
  private int logSize;
  private int mask;
  private final int maxBucketSize;
  @SuppressWarnings("unchecked")
  public SerialHashTable(int logSize, int maxBucketSize) {
    this.logSize = logSize;
    this.mask = (1 << logSize) - 1;
    this.maxBucketSize = maxBucketSize;
    this.table = new SerialList[1 << logSize];
  }
  public void resizeIfNecessary(int key) {
    while( table[key & mask] != null 
          && table[key & mask].getSize() >= maxBucketSize )
      resize();
  }
  private void addNoCheck(int key, T x) {
    int index = key & mask;
    if( table[index] == null )
      table[index] = new SerialList<T,Integer>(key,x);
    else
      table[index].addNoCheck(key,x);
  }
  public void add(int key, T x) {
    resizeIfNecessary(key);
    addNoCheck(key,x);
  }
  public boolean remove(int key) {
    resizeIfNecessary(key);
    if( table[key & mask] != null )
      return table[key & mask].remove(key);
    else
      return false;
  }
  public boolean contains(int key) {
    SerialList<T,Integer>[] myTable = table;
    int myMask = myTable.length - 1;
    if( myTable[key & myMask] != null )
      return myTable[key & myMask].contains(key);
    else
      return false;
  }
  @SuppressWarnings("unchecked")
  public void resize() {
    SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
    for( int i = 0; i < table.length; i++ ) {
      if( table[i] == null )
        continue;
      SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
      while( iterator != null ) {
        if( newTable[iterator.key & ((2*mask)+1)] == null )
          newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
        else
          newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
        iterator = iterator.getNext();
      }
    }
    table = newTable;
    logSize++;
    mask = (1 << logSize) - 1;
  }
  public void printTable() {
    for( int i = 0; i <= mask; i++ ) {
      System.out.println("...." + i + "....");
      if( table[i] != null)
        table[i].printList();
    }
  }
}

class SerialHashTableTest {
  public static void main(String[] args) {  
    SerialHashTable<Integer> table = new SerialHashTable<Integer>(2, 8);
    for( int i = 0; i < 256; i++ ) {
      table.add(i,i*i);
    }
    table.printTable();    
  }
}

class LockingHashTable<T> implements HashTable<T> {
	  private SerialList<T,Integer>[] table;
	  private int logSize;
	  private int mask;
	  private final int maxBucketSize;
	  volatile ReentrantReadWriteLock[] locks;
	  @SuppressWarnings("unchecked")
	  public LockingHashTable(int logSize, int maxBucketSize) {
	    this.logSize = logSize;
	    this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
	    this.table = new SerialList[1 << logSize];
	    this.locks = new ReentrantReadWriteLock[1 << logSize];
	    for (int i = 0; i < locks.length; i++) {
	    	locks[i] = new ReentrantReadWriteLock();
	    }
	  }
	  public void resizeIfNecessary(int key) {
	    while( table[key & mask] != null 
	          && table[key & mask].getSize() >= maxBucketSize )
	      resize();
	  }
	  private void addNoCheck(int key, T x) {
	    int index = key & mask;
	    ReentrantReadWriteLock lock = locks[index%locks.length];
	    lock.writeLock().lock();
	    try {
		    if( table[index] == null )
		      table[index] = new SerialList<T,Integer>(key,x);
		    else
		      table[index].add(key,x);
	    } finally{
	    	lock.writeLock().unlock();
	    
	    }
	  }
	  public void add(int key, T x) {
	    resizeIfNecessary(key);
	    addNoCheck(key,x);
	  }
	  
	  public boolean remove(int key) {
	    resizeIfNecessary(key);
	    int index = key & mask;
	    ReentrantReadWriteLock lock = locks[index%locks.length];
	    lock.writeLock().lock();
	    try {
		    if( table[key & mask] != null )
		      return table[key & mask].remove(key);
		    else
		      return false;
	    } finally{
	    	lock.writeLock().unlock();
	    }
	  }
	  public boolean contains(int key) {
	    SerialList<T,Integer>[] myTable = table;
	    int myMask = myTable.length - 1;
	    int index = key & myMask;
	    ReentrantReadWriteLock lock = locks[index%locks.length];
	    lock.readLock().lock();
	    try {
		    if( myTable[index] != null )
		      return myTable[index].contains(key);
		    else
		      return false;
	    } finally {
	    	lock.readLock().unlock();
	    }
	  }
	  @SuppressWarnings("unchecked")
	  public void resize() {
	    int oldLength = table.length;
	    for (ReentrantReadWriteLock lock: locks) {
	    	lock.writeLock().lock();
	    }
	    try {
	    	if (oldLength != table.length) {
	    		return;
	    	}

		    SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
		    for( int i = 0; i < table.length; i++ ) {
		      if( table[i] == null )
		        continue;
		      SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
		      while( iterator != null ) {
		        if( newTable[iterator.key & ((2*mask)+1)] == null )
		          newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
		        else
		          newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
		        iterator = iterator.getNext();
		      }
		    }
		    table = newTable;
		    logSize++;
		    mask = (1 << logSize) - 1;
	    } finally {
	    	for (ReentrantReadWriteLock lock: locks) {
		    	lock.writeLock().unlock();
		    }
	    }
	  }
	  
	  public void printTable() {
	    for( int i = 0; i <= mask; i++ ) {
	      System.out.println("...." + i + "....");
	      if( table[i] != null)
	        table[i].printList();
	    }
	  }
	}

class OptimisticHashTable<T> implements HashTable<T> {
	  private SerialList<T,Integer>[] table;
	  private int logSize;
	  private int mask;
	  private final int maxBucketSize;
	  volatile ReentrantReadWriteLock[] locks;
	  @SuppressWarnings("unchecked")
	  public OptimisticHashTable(int logSize, int maxBucketSize) {
	    this.logSize = logSize;
	    this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
	    this.table = new SerialList[1 << logSize];
	    this.locks = new ReentrantReadWriteLock[1 << logSize];
	    for (int i = 0; i < locks.length; i++) {
	    	locks[i] = new ReentrantReadWriteLock();
	    }
	  }
	  public void resizeIfNecessary(int key) {
	    while( table[key & mask] != null 
	          && table[key & mask].getSize() >= maxBucketSize )
	      resize();
	  }
	  private void addNoCheck(int key, T x) {
	    int index = key & mask;
	    ReentrantReadWriteLock lock = locks[index%locks.length];
	    lock.writeLock().lock();
	    try {
		    if( table[index] == null )
		      table[index] = new SerialList<T,Integer>(key,x);
		    else
		      table[index].add(key,x);
	    } finally{
	    	lock.writeLock().unlock();
	    
	    }
	  }
	  public void add(int key, T x) {
	    resizeIfNecessary(key);
	    addNoCheck(key,x);
	  }
	  
	  public boolean remove(int key) {
	    resizeIfNecessary(key);
	    int index = key & mask;
	    ReentrantReadWriteLock lock = locks[index%locks.length];
	    lock.writeLock().lock();
	    try {
		    if( table[key & mask] != null )
		      return table[key & mask].remove(key);
		    else
		      return false;
	    } finally{
	    	lock.writeLock().unlock();
	    }
	  }
	  public boolean contains(int key) {
	    SerialList<T,Integer>[] myTable = table;
	    int myMask = myTable.length - 1;
	    int index = key & myMask;
	    ReentrantReadWriteLock lock = locks[index%locks.length];
	    if (myTable[index] == null) {
	    	return false;
	    } else {
	    	if (myTable[index].contains(key))	
	    		return true;
	    	else	    		
			    lock.readLock().lock();
			    try {
			    	return myTable[index].contains(key);
			    } finally {
			    	lock.readLock().unlock();
			    }
	    }
	    
	  }
	  @SuppressWarnings("unchecked")
	  public void resize() {
	    int oldLength = table.length;
	    for (ReentrantReadWriteLock lock: locks) {
	    	lock.writeLock().lock();
	    }
	    try {
	    	if (oldLength != table.length) {
	    		return;
	    	}

		    SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
		    for( int i = 0; i < table.length; i++ ) {
		      if( table[i] == null )
		        continue;
		      SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
		      while( iterator != null ) {
		        if( newTable[iterator.key & ((2*mask)+1)] == null )
		          newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
		        else
		          newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
		        iterator = iterator.getNext();
		      }
		    }
		    table = newTable;
		    logSize++;
		    mask = (1 << logSize) - 1;
	    } finally {
	    	for (ReentrantReadWriteLock lock: locks) {
		    	lock.writeLock().unlock();
		    }
	    }
	  }
	  
	  public void printTable() {
	    for( int i = 0; i <= mask; i++ ) {
	      System.out.println("...." + i + "....");
	      if( table[i] != null)
	        table[i].printList();
	    }
	  }
	}

class LockFreeHashTable<T> implements HashTable<T> {
	  private SerialList<T,Integer>[] table;
	  private int logSize;
	  private int mask;
	  private final int maxBucketSize;
	  volatile ReentrantLock[] locks;
	  @SuppressWarnings("unchecked")
	  public LockFreeHashTable(int logSize, int maxBucketSize) {
	    this.logSize = logSize;
	    this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
	    this.table = new SerialList[1 << logSize];
	    this.locks = new ReentrantLock[1 << logSize];
	    for (int i = 0; i < locks.length; i++) {
	    	locks[i] = new ReentrantLock();
	    }
	  }
	  public void resizeIfNecessary(int key) {
	    while( table[key & mask] != null 
	          && table[key & mask].getSize() >= maxBucketSize )
	      resize();
	  }
	  private void addNoCheck(int key, T x) {
	    int index = key & mask;
	    ReentrantLock lock = locks[index%locks.length];
	    lock.lock();
	    try {
		    if( table[index] == null )
		      table[index] = new SerialList<T,Integer>(key,x);
		    else
		      table[index].add(key,x);
	    } finally{
	    	lock.unlock();
	    
	    }
	  }
	  public void add(int key, T x) {
	    resizeIfNecessary(key);
	    addNoCheck(key,x);
	  }
	  
	  public boolean remove(int key) {
	    resizeIfNecessary(key);
	    int index = key & mask;
	    ReentrantLock lock = locks[index%locks.length];
	    lock.lock();
	    try {
		    if( table[key & mask] != null )
		      return table[key & mask].remove(key);
		    else
		      return false;
	    } finally{
	    	lock.unlock();
	    }
	  }
	  public boolean contains(int key) {
	    SerialList<T,Integer>[] myTable = table;
	    int myMask = myTable.length - 1;
	    int index = key & myMask;
	    if( myTable[index] != null )
	      return myTable[index].contains(key);
	    else
	      return false;

	  }
	  @SuppressWarnings("unchecked")
	  public void resize() {
	    int oldLength = table.length;
	    for (ReentrantLock lock: locks) {
	    	lock.lock();
	    }
	    try {
	    	if (oldLength != table.length) {
	    		return;
	    	}

		    SerialList<T,Integer>[] newTable = new SerialList[2*table.length];
		    for( int i = 0; i < table.length; i++ ) {
		      if( table[i] == null )
		        continue;
		      SerialList<T,Integer>.Iterator<T,Integer> iterator = table[i].getHead();
		      while( iterator != null ) {
		        if( newTable[iterator.key & ((2*mask)+1)] == null )
		          newTable[iterator.key & ((2*mask)+1)] = new SerialList<T,Integer>(iterator.key, iterator.getItem());
		        else
		          newTable[iterator.key & ((2*mask)+1)].addNoCheck(iterator.key, iterator.getItem());
		        iterator = iterator.getNext();
		      }
		    }
		    table = newTable;
		    logSize++;
		    mask = (1 << logSize) - 1;
	    } finally {
	    	for (ReentrantLock lock: locks) {
		    	lock.unlock();
		    }
	    }
	  }
	  
	  public void printTable() {
	    for( int i = 0; i <= mask; i++ ) {
	      System.out.println("...." + i + "....");
	      if( table[i] != null)
	        table[i].printList();
	    }
	  }
	}


class ProbeEntry<T> {
	int key;
	T value;
	int counter;
	boolean exist;
	public ProbeEntry(int key, T x) {
		this.key = key;
		this.value = x;
		this.counter = 0;
		this.exist = true;
	}
	
	public void print() {
		if (exist)
			System.out.println(key+","+value+","+counter);
		else
			System.out.println("Empty");
	}
}
class LinearProbeHashTable<T> implements HashTable<T> {
	  private ProbeEntry<T>[] table;
	  private int logSize;
	  private int mask;
	  private final int maxBucketSize;
	  volatile ReentrantLock[] locks;
	  @SuppressWarnings("unchecked")
	  public LinearProbeHashTable(int logSize, int maxBucketSize) {
	    this.logSize = logSize;
	    this.mask = (1 << logSize) - 1;
	    this.maxBucketSize = maxBucketSize;
	    this.table = new ProbeEntry[1 << logSize];
	    this.locks = new ReentrantLock[1 << logSize];
	    for (int i = 0; i < locks.length; i++) {
	    	locks[i] = new ReentrantLock();
	    }
	  }
	  @SuppressWarnings({ "unchecked", "rawtypes" })
	private int addNoCheck(int key, T x) {
	    int index = key & mask;
	    ReentrantLock lock = locks[index%locks.length];
	    ReentrantLock testLock;
	    lock.lock();
	    if (table[index] == null || table[index].exist == false) {
	    	table[index] = new ProbeEntry<T>(key, x);
	    	lock.unlock();
	    	return 0;
	    }
	    lock.unlock();
		int k = 1;
    	int testIndex = index+1;
	    while (true) {
        if (testIndex >= table.length) {
          resize();
          int myCounter = this.addNoCheck(key, x);
          return myCounter;
        }
		    while (table[testIndex] !=null && table[testIndex].exist) {
		    	testIndex ++;
		    	k ++;
          if (testIndex >= table.length) {
            resize();
            int myCounter = this.addNoCheck(key, x);
            return myCounter;
          }
		    }

		    testLock = locks[testIndex%locks.length];
		    testLock.lock();
			if(table[testIndex]==null) {
				table[testIndex] = new ProbeEntry<T>(key, x);
			    	testLock.unlock();
				    lock.lock();
            if (table[index] == null) {
              ProbeEntry entry = new ProbeEntry<T>(0, null);
              entry.exist = false;
              entry.counter = k;
            }else {
              table[index].counter = Math.max(table[index].counter, k);	
            }
			    	lock.unlock();
			    	return k;
			    
			} else if (!table[testIndex].exist) {
			    	table[testIndex].value = x;
			    	table[testIndex].key = key;
			    	table[testIndex].exist = true;	
			    	testLock.unlock();
				    lock.lock();
            if (table[index] == null) {
              ProbeEntry entry = new ProbeEntry<T>(0, null);
              entry.exist = false;
              entry.counter = k;
            }else {
              table[index].counter = Math.max(table[index].counter, k);	
            }
			    	lock.unlock();
			    	return k;
 
			} else {
				testLock.unlock();
			}
		 
	    }
	  }
	  public void add(int key, T x) {
	    int k = addNoCheck(key,x);
	    if (k >= maxBucketSize) 
	    	resize();
	  }
	  
	  public boolean remove(int key) {
	    int index = key & mask;
      int counter;
      if (table[index]==null) 
        counter=1;
      else
        counter = table[index].counter+1;
	    for (int i=0; i<counter; i++) {
		    ReentrantLock lock = locks[(index+i)%locks.length];
		    lock.lock();
		    try {
			    if( table[index+i]!=null && table[index+i].key == key ) {
			      table[index].exist = false;
			      return true;
			    }
		    } finally{
		    	lock.unlock();
		    }
	    }	    
	    return false;
	  }
	  
	  public boolean contains(int key) {
	    ProbeEntry<T>[] myTable = table;
	    int myMask = myTable.length - 1;
	    int index = key & myMask;
      int counter;
      if (myTable[index]==null) 
        counter=1;
      else
        counter = myTable[index].counter+1;
	    for (int i=0; i<counter; i++) {
		    if( table[index+i]==null) {
		    	return false;
		    }else {
		    	if (table[index+i].exist && table[index+i].key == key) {
		            return true;
		    	}
		    }
		      
	    }
	    return false;

	  }
	  public void resize() {
	    int oldLength = table.length;
	    for (ReentrantLock lock: locks) {
	    	lock.lock();
	    }
	    try {
	    	if (oldLength != table.length) {
	    		return;
	    	}

		    LinearProbeHashTable<T> newTable = new LinearProbeHashTable<T> (logSize+1, this.maxBucketSize);
		    for( int i = 0; i < table.length; i++ ) {
		      if( table[i]==null || !table[i].exist)
		        continue;
		      int key = table[i].key;
		      T value = table[i].value;
		      newTable.addNoCheck(key, value);
		    }
		    table = newTable.table;
		    logSize++;
		    mask = (1 << logSize) - 1;
	    } finally {
	    	for (ReentrantLock lock: locks) {
		    	lock.unlock();
		    }
	    }
	  }
	  
	  public void printTable() {
		    for( int i = 0; i <= mask; i++ ) {
		      System.out.println("...." + i + "....");
		      if( table[i] != null)
		        table[i].print();
		    }
		  }
	}

class LockFreeBucketList<T> {
    static final int WORD_SIZE = 24;
    static final int LO_MASK = 0x00000001;
    static final int HI_MASK = 0x00800000;
    static final int MASK = 0x00FFFFFF;
    Node head;

    /**
     * Constructor
     */
    public LockFreeBucketList() {
        this.head = new Node(0);
        this.head.next = new AtomicMarkableReference<Node>(new Node(Integer.MAX_VALUE), false);
    }

    private LockFreeBucketList(Node e) {
        this.head = e;
    }

    /**
     * Restricted-size hash code
     * @param x object to hash
     * @return hash code
     */
    public static int hashCode(Object x) {
        return x.hashCode() & MASK;
    }

    public boolean add(int oldKey, T x) {
        int key = makeRegularKey(oldKey);
        boolean splice;
        while(true) {
            // find predecessor and current entries
            Window window = find(head, key);
            Node pred = window.pred;
            Node curr = window.curr;
            // is the key present?
            if(curr.key == key) {
                return false;
            } else {
                // splice in new entry
                Node entry = new Node(key, x);
                entry.next.set(curr, false);
                splice = pred.next.compareAndSet(curr, entry, false, false);
                if(splice)
                    return true;
                else
                    continue;
            }
        }
    }

    public boolean remove(int oldKey) {
        int key = makeRegularKey(oldKey);
        boolean snip;
        while(true) {
            // find predecessor and current entries
            Window window = find(head, key);
            Node pred = window.pred;
            Node curr = window.curr;
            // is the key present?
            if(curr.key != key) {
                return false;
            } else {
                // snip out matching entry
                snip = pred.next.attemptMark(curr, true);
                if(snip)
                    return true;
                else
                    continue;
            }
        }
    }

    public boolean contains(int oldKey) {
        int key = makeRegularKey(oldKey);
        Window window = find(head, key);
        Node pred = window.pred;
        Node curr = window.curr;
        return (curr.key == key);
    }

    public LockFreeBucketList<T> getSentinel(int index) {
        int key = makeSentinelKey(index);
        boolean splice;
        while(true) {
            // find predecessor and current entries
            Window window = find(head, key);
            Node pred = window.pred;
            Node curr = window.curr;
            // is the key present?
            if(curr.key == key) {
                return new LockFreeBucketList<T>(curr);
            } else {
                // splice in new entry
                Node entry = new Node(key);
                entry.next.set(pred.next.getReference(), false);
                splice = pred.next.compareAndSet(curr, entry, false, false);
                if(splice)
                    return new LockFreeBucketList<T>(entry);
                else
                    continue;
            }
        }
    }

    public static int reverse(int key) {
        int loMask = LO_MASK;
        int hiMask = HI_MASK;
        int result = 0;
        for(int i = 0; i < WORD_SIZE; i++) {
            if((key & loMask) != 0) { // bit set
                result |= hiMask;
            }
            loMask <<= 1;
            hiMask >>>= 1; // fill with 0 from left
        }
        return result;
    }

    public int makeRegularKey(int key) {
        int code = key & MASK; // take 3 lowest bytes
        return reverse(code | HI_MASK);
    }

    private int makeSentinelKey(int key) {
        return reverse(key & MASK);
    }

    // iterate over Set elements
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    private class Node {
        int key;
        T value;
        AtomicMarkableReference<Node> next;

        Node(int key, T object) { // usual constructor
            this.key = key;
            this.value = object;
            this.next = new AtomicMarkableReference<Node>(null, false);
        }

        Node(int key) { // sentinel constructor
            this.key = key;
            this.next = new AtomicMarkableReference<Node>(null, false);
        }

        Node getNext() {
            boolean[] cMarked = { false }; // is curr marked?
            boolean[] sMarked = { false }; // is succ marked?
            Node entry = this.next.get(cMarked);
            while(cMarked[0]) {
                Node succ = entry.next.get(sMarked);
                this.next.compareAndSet(entry, succ, true, sMarked[0]);
                entry = this.next.get(cMarked);
            }
            return entry;
        }
    }

    class Window {
        public Node pred;
        public Node curr;

        Window(Node pred, Node curr) {
            this.pred = pred;
            this.curr = curr;
        }
    }

    public Window find(Node head, int key) {
        Node pred = head;
        Node curr = head.getNext();
        while(curr.key < key) {
            pred = curr;
            curr = pred.getNext();
        }
        return new Window(pred, curr);
    }

	
}

class AwesomeHashTable<T> implements HashTable<T> {
	  protected LockFreeBucketList<T>[] bucket;
	  protected AtomicInteger bucketSize;
	  protected AtomicInteger setSize;
	  private int mask;
	  int maxBucketSize;
	  @SuppressWarnings("unchecked")
	public AwesomeHashTable(int logSize,int maxBucketSize) {
	    this.bucket = (LockFreeBucketList<T>[]) new LockFreeBucketList[10000000];
	    this.bucket[0] = new LockFreeBucketList<T>();
	    this.bucketSize = new AtomicInteger(2);
	    this.setSize = new AtomicInteger(0);
	    this.mask = (1<<logSize) -1;
	    this.maxBucketSize = maxBucketSize;
	  }
	  public void add (int key, T x) {
	    int myBucket = key % bucketSize.get();
	    LockFreeBucketList<T> b = getLockFreeBucketList(myBucket);
	    if (!b.add(key, x)) {
	      return;
	    }
	    int setSizeNow = setSize.getAndIncrement();
	    int bucketSizeNow = bucketSize.get();
	    if (setSizeNow / bucketSizeNow > maxBucketSize) {
        int newSize = Math.min(2*bucketSizeNow, 10000000);
	      bucketSize.compareAndSet(bucketSizeNow, newSize);
	    }
	    return;
	  }
	  private LockFreeBucketList<T> getLockFreeBucketList(int myBucket) {
		    if (bucket[myBucket] == null) {
		      initializeBucket(myBucket);
		    }
		    return bucket[myBucket];
		  }
	  private void initializeBucket(int myBucket) {
	    int parent = getParent(myBucket);
	    if (bucket[parent] == null) {
	      initializeBucket(parent);
	    }
	    LockFreeBucketList<T> b = bucket[parent].getSentinel(myBucket);
	    if (b != null) {
	      bucket[myBucket] = b;
	    }
	  }
	  private int getParent(int myBucket) {
	    int parent = bucketSize.get();
	    do {
	      parent = parent >> 1;
	    } while (parent > myBucket);
	    parent = myBucket - parent;
	    return parent;
	  }
	

	  public boolean remove (int key) {
	    int myBucket = key  % bucketSize.get();
	    LockFreeBucketList<T> b = getLockFreeBucketList(myBucket);
	    if (!b.remove(key))  {
	      return false;
	    }
	    int setSizeNow = setSize.getAndDecrement();
	    return true;
	  }
	   
	  public boolean contains (int key) {
	    int myBucket = key % bucketSize.get();
	    LockFreeBucketList<T> b = getLockFreeBucketList(myBucket);
	    return b.contains(key);
	  }
}

class AppSpecificHashTable<T> implements HashTable<T> {
	  protected LockFreeBucketList<T>[] bucket;
	  protected AtomicInteger bucketSize;
	  protected AtomicInteger setSize;
	  private int mask;
	  int maxBucketSize = 5;
	  @SuppressWarnings("unchecked")
	public AppSpecificHashTable(int logSize,int maxBucketSize) {
	    this.bucket = (LockFreeBucketList<T>[]) new LockFreeBucketList[10000000];
	    this.bucket[0] = new LockFreeBucketList<T>();
	    this.bucketSize = new AtomicInteger(2);
	    this.setSize = new AtomicInteger(0);
	    this.mask = (1<<logSize) -1;
	    //this.maxBucketSize = maxBucketSize;
	  }
	  public void add (int key, T x) {
	    int myBucket = key % bucketSize.get();
	    LockFreeBucketList<T> b = getLockFreeBucketList(myBucket);
	    if (!b.add(key, x)) {
	      return;
	    }
	    int setSizeNow = setSize.getAndIncrement();
	    int bucketSizeNow = bucketSize.get();
	    if (setSizeNow / bucketSizeNow > maxBucketSize) {
      int newSize = Math.min(2*bucketSizeNow, 10000000);
	      bucketSize.compareAndSet(bucketSizeNow, newSize);
	    }
	    return;
	  }
	  private LockFreeBucketList<T> getLockFreeBucketList(int myBucket) {
		    if (bucket[myBucket] == null) {
		      initializeBucket(myBucket);
		    }
		    return bucket[myBucket];
		  }
	  private void initializeBucket(int myBucket) {
	    int parent = getParent(myBucket);
	    if (bucket[parent] == null) {
	      initializeBucket(parent);
	    }
	    LockFreeBucketList<T> b = bucket[parent].getSentinel(myBucket);
	    if (b != null) {
	      bucket[myBucket] = b;
	    }
	  }
	  private int getParent(int myBucket) {
	    int parent = bucketSize.get();
	    do {
	      parent = parent >> 1;
	    } while (parent > myBucket);
	    parent = myBucket - parent;
	    return parent;
	  }
	

	  public boolean remove (int key) {
	    int myBucket = key  % bucketSize.get();
	    LockFreeBucketList<T> b = getLockFreeBucketList(myBucket);
	    if (!b.remove(key))  {
	      return false;
	    }
	    int setSizeNow = setSize.getAndDecrement();
	    return true;
	  }
	   
	  public boolean contains (int key) {
	    int myBucket = key % bucketSize.get();
	    LockFreeBucketList<T> b = getLockFreeBucketList(myBucket);
	    return b.contains(key);
	  }
}
