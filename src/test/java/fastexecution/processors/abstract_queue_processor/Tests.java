package fastexecution.processors.abstract_queue_processor;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import fastexecution.Processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Tests {
	public static final boolean PRINT_THREADS_IN_GROUP = false;
	public static final boolean REPORT_TEST_QUALITY = false;
	
	int initialThreadsInGroup;
		
	public void getThreadsInGroup() {
		initialThreadsInGroup = Thread.activeCount();
	}
		
	public void checkThreadsInGroup() throws Exception {
		//system should manage changed number of threads
		final int maxWaitTime = 1000;
		int t = 0;
		do {
			int n = Thread.activeCount();
			if (n == initialThreadsInGroup) {
				break;
			} else {
				Thread.sleep(10); 
			}
			t++;
		} while (t<maxWaitTime);
		assertEquals(initialThreadsInGroup, Thread.activeCount());
	}
	
	@RepeatedTest(value = 10)
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	public void concurrentRunStopCheck() throws Exception {
		final int actionNumber = 1000; //each thread will start/stop this number of times
		getThreadsInGroup();
		printThreadsInGroup("concurrentRunStopCheck");
		EmptyProcessor processor = new EmptyProcessor();
		ExecutorService testService = Executors.newFixedThreadPool(2);
		Future<Integer> starterResult = testService.submit(()->{
										int n = 0;
										for (int x = 0; x < actionNumber; x++) {
											if (processor.start()) n++;
											Thread.yield();
										}
										return n;
									});
		Future<Integer> stopperResult = testService.submit(()->{
										int n = 0;
										for (int x = 0; x < actionNumber; x++) {
											if (processor.stop()) n++;
											Thread.yield();
										}
										return n;
									});
		//check test quality
		assertTrue(starterResult.get() > 0, "Test is not qualitative, starter did not start processor at all");
		assertTrue(stopperResult.get() > 0, "Test is not qualitative, stopper did not start processor at all");
		if (REPORT_TEST_QUALITY) {
			System.out.println("concurrentRunStopCheck test quality:");
			System.out.println("    started: " + starterResult.get());
			System.out.println("    stopped: " + stopperResult.get());
		}
		testService.shutdown();
		testService.awaitTermination(1, TimeUnit.SECONDS);
		stopProcessor(processor);
		checkThreadsInGroup();
	}
	
	@RepeatedTest(value = 10)
	@Timeout(value = 20, unit = TimeUnit.SECONDS)
	public void concurrentThreadNumberChangeCheck() throws Exception {
		final int maxThreadNumber = 10;
		final int actorNumber = 3; //number of threads, which will modify processor threads
		final int actionNumber = 300; //each thread will modify processor threads this number of times
		getThreadsInGroup();
		printThreadsInGroup("concurrentThreadNumberChangeCheck");
		EmptyProcessor processor = new EmptyProcessor();
		startProcessor(processor);
		//submit actors
		ExecutorService testService = Executors.newFixedThreadPool(actorNumber);
		ArrayList<Future<Integer>> actorResults = new ArrayList<>(actorNumber);
		for (int i = 0; i < actorNumber; i++) {
			Future<Integer> r = testService.submit(()->{
										int n = 0;
										for (int x = 0; x < actionNumber; x++) {
											int d = (int)Math.round(maxThreadNumber*Math.random());
											if (processor.setThreadNumber(d) != -1) n++;
											Thread.yield();
										}
										return n;
									});
			actorResults.add(r);
		}
		//check test quality
		int minSets = actionNumber;
		int maxSets = 0;
		for (var r : actorResults) {
			int s = r.get();
			assertTrue(s > 0, "Test is not qualitative, some actors did not set thread number at all");
			if (s < minSets) {
				minSets = s;
			}
			if (s > maxSets) {
				maxSets = s;
			}
		}
		if (REPORT_TEST_QUALITY) {
			System.out.println("concurrentThreadNumberChangeCheck test quality");
			System.out.println("    min. setts: " + minSets);
			System.out.println("    max. setts: " + maxSets);
		}
		testService.shutdown();
		testService.awaitTermination(1, TimeUnit.SECONDS);
		stopProcessor(processor);
		checkThreadsInGroup();
	}
	
	@RepeatedTest(value = 10)
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	public void increaseThreadNumberCheck() throws Exception {
		final int finalThreadNumber = 20;
		getThreadsInGroup();
		printThreadsInGroup("increaseThreadNumberCheck");
		EmptyProcessor processor = new EmptyProcessor();
		startProcessor(processor);
		//increase extra threads; check that numbers are growing
		int retNum = 0;
		int groupNum = initialThreadsInGroup+1;
		processor.setThreadNumber(finalThreadNumber);
		while(!processor.changedThreadNumber()) {
			int r = processor.getThreadNumber();
			int g = Thread.activeCount();
			assertTrue(r >= retNum);
			assertTrue(g >= groupNum);
			retNum = r;
			groupNum = g;
		}
		//check final numbers
		assertEquals(finalThreadNumber, processor.getThreadNumber());
		assertEquals(initialThreadsInGroup+finalThreadNumber, Thread.activeCount());
		stopProcessor(processor);
		checkThreadsInGroup();
	}
	
	@RepeatedTest(value = 10)
	@Timeout(value = 10, unit = TimeUnit.SECONDS)
	public void decreaseThreadNumberCheck() throws Exception {
		final int initialThreadNumber = 20;
		getThreadsInGroup();
		printThreadsInGroup("decreaseThreadNumberCheck");
		EmptyProcessor processor = new EmptyProcessor();
		startProcessor(processor);
		//set initial number of extra threads
		processor.setThreadNumber(initialThreadNumber);
		while (!processor.changedThreadNumber());
		assertEquals(initialThreadNumber, processor.getThreadNumber());
		//set 0 extra threads; check that numbers are decreasing
		int retNum = initialThreadNumber;
		int groupNum = initialThreadsInGroup+initialThreadNumber+1;
		processor.setThreadNumber(0);
		while(!processor.changedThreadNumber()) {
			int r = processor.getThreadNumber();
			int g = Thread.activeCount();
			assertTrue(r <= retNum);
			assertTrue(g <= groupNum);
			retNum = r;
			groupNum = g;
		}
		//check final numbers
		assertEquals(1, processor.getThreadNumber()); //1 thread should stay active
		stopProcessor(processor);
		checkThreadsInGroup();
	}
	
	protected void startProcessor(Processor processor) {
		while (!processor.start());
		assertEquals(1, processor.getThreadNumber());
	}
	
	protected void stopProcessor(Processor processor) {
		while (!processor.stop());
		while (!processor.changedThreadNumber());
		assertEquals(0, processor.getThreadNumber());
	}
	
	void printThreadsInGroup(String testName) {
		if (PRINT_THREADS_IN_GROUP) {
			Thread[] tarray = new Thread[(int)(Thread.activeCount()*1.5)+1];
			int num = Thread.enumerate(tarray);
			System.out.println("Threads in group during " + testName + ": " + num);
			for (int i = 0; i < num; i++) {
				Thread t = tarray[i];
				System.out.println("    " + t.getName());
			}
		}
	}
}
