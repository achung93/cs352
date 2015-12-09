import java.util.Scanner;

public class ListenForQuit implements Runnable {

	Tracker tracker;
	public ListenForQuit(Tracker tracker){
		this.tracker=tracker;
	}
	//checks input for quit, saves to file then quits
	boolean currentRun = true;
	public void run(){
		Scanner scanner = new Scanner(System.in);
		while(currentRun){
			//note take out print statements in peer 377/378 to make easier
			if(scanner.nextLine().equalsIgnoreCase("QUIT")){
				scanner.close();
				tracker.urlFinishDownloading("stopped");
				currentRun = false;
				//save data
				System.exit(0);
			}
		}
	}
}
