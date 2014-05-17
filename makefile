JFLAGS= 
JC=javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	PaddedPrimitive.java \
	SkipList.java\
	StopWatch.java \
	Fingerprint.java \
	RandomGenerator.java \
	PacketGenerator.java \
	WaitFreeQueue.java\
	PacketWorker.java \
	SerialFirewall.java\
	Dispatcher.java\
	ParallelDispatcher.java\
	AwesomeParallelFirewall.java\

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
