package parallel;

import java.util.Date;
import model.WorkerType;

public class WorkerLogger {
	private int workerId;
	private WorkerType workerType;
	
	public WorkerLogger(WorkerType workerType, int workerId){
		this.workerType = workerType;
		this.workerId = workerId;
	}
	
	public void logging(String message){
		String format = "%s\t[%s %d] : %s";
		String log = String.format(format, new Date().toString(), workerType.toString(), workerId, message);
		
		System.out.println(log);
	}
}