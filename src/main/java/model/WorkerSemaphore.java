package model;

import java.util.concurrent.Semaphore;

public class WorkerSemaphore {
	private Semaphore mutex;
	private Semaphore fillCount;
	private Semaphore emptyCount;
	
	public WorkerSemaphore(Semaphore mutex, Semaphore fillCount, Semaphore emptyCount){
		this.mutex = mutex;
		this.fillCount = fillCount;
		this.emptyCount = emptyCount;
	}

	public Semaphore getMutex() {
		return mutex;
	}

	public Semaphore getFillCount() {
		return fillCount;
	}

	public Semaphore getEmptyCount() {
		return emptyCount;
	}
}
