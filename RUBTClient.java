import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;


public class RUBTClient {

	public static TorrentInfo torrentInfoObj;
	
	/*
	 * makes byte array from inputted file.
	 * necessary to make torrentinfo object.
	 * strategy to convert taken from here:
	 * http://www.mkyong.com/java/how-to-convert-file-into-an-array-of-bytes/
	 */
	public ArrayList<Peer> a = new ArrayList<Peer>();
	public RUBTClient(){
		TorrentInfo t = torrentInfoObj;
		this.a = null;
	}
	private static byte[] getFileBytes(File file) throws IOException {
		
        byte[] byteFile = new byte[ (int) file.length() ];
        FileInputStream streamFile = null;
        
        try{
        streamFile = new FileInputStream(file);
        streamFile.read(byteFile);
        streamFile.close();
        }catch(Exception e){
        	System.out.println("problem converting file to bytes,"
        			+ "reading file,"
        			+ "or closing file??");
        	System.exit(0);
        }
        
        return byteFile;
    }
	
	
	 /* generates random alpha numeric sequence by generating random number
	 * in certain range then mapping number to ASCII char.
	 * will not have 
	 */ 
	private static String clientGeneratedID(){
		int range = (132 - 97) + 1;
		String retString = "";
		for(int i=0; i< 20; i++){
			int asciiValue = (int)(Math.random() * range);
			if(asciiValue<=25){		//convert to char(in range a-z)
				asciiValue+=97;
			}else if(asciiValue>25){	//convert to char(in range(0-9)
				asciiValue = (132 - 97 - asciiValue) + 48;
			}
			retString+= Character.toString( (char) asciiValue);
		}
		
		//recursive call to avoid clientID that starts with "ru"
		if( (retString.charAt(0)=='r') && (retString.charAt(1) == 'u') ){
			return clientGeneratedID();
		}
		return retString;
	}
 
	
    public static void main(String[] args) {
    	
    	//args check
    	if(args.length != 2){
    		System.out.println("incorrect input- 2 arguments required");
    		System.exit(0);
    	}
    	
    	File torrentFilearg0 = null;
    	torrentFilearg0 = new File(args[0]);
    	if( !(torrentFilearg0.exists()) ){
    		System.out.println("file can not be found");
    		System.exit(0);
    	}
    	
    	if( !(torrentFilearg0.canRead()) ){
    		System.out.println("file does not have read permission");
    		System.exit(0);
    	}
    	
    	//generating parameters for tracker
    	String clientGeneratedPeerID = clientGeneratedID();
    	byte[] fileInBytes = null;
		try {
			fileInBytes = getFileBytes(torrentFilearg0);
		} catch (IOException e2) {
			System.out.println("trouble converting file to bytes");
			System.exit(0);
		}
		
    	try {
			torrentInfoObj = new TorrentInfo(fileInBytes);
		} catch (BencodingException e1) {
			System.out.println("bencoding problem");
			System.exit(0);
		} 
    	
    	//tracker initialization
    	Tracker tracker = new Tracker(torrentInfoObj, clientGeneratedPeerID);
    	
    	//tracker response to get request
    	ByteArrayOutputStream trackerBencoded = null;
		try {
			trackerBencoded = tracker.getTrackerResponse();
		} catch (BencodingException e1) {
			e1.printStackTrace();
		}
		
    	//check if file written stuff already, if it has then increment piece count
    	int pieceNumber = 0;
    	File f = new File(args[1]);
    	if(f.exists()){
    		int totalFileLength = (int)f.length();
    		pieceNumber = totalFileLength/torrentInfoObj.piece_length;
    	}
    	
    	//parsed tracker info- n%0 is id, n%1 is ip, n%2 is port
    	ArrayList<String> peer0 = tracker.getPeer(trackerBencoded);
    	System.out.println(peer0);
    	System.out.println("good up through tracker response");
    	
    	
    	/////////////////////////////////////////////////////////////////
    	
    	
    	
    	ArrayList<Peer> peerList = new ArrayList<Peer>();
    	ArrayList<String> peer000 = new ArrayList<String>();
    	boolean first = true;
    	Peer peer12;
    	for(int i=0; i< peer0.size();i++){
    		if(i%3 == 0 && first==false){
    			
    			peer12 = new Peer(torrentInfoObj, peer000, clientGeneratedPeerID, args[1], pieceNumber, tracker);
    			peerList.add(peer12);
    			peer000.clear();
    			peer000.add(peer0.get(i));
    			continue;
    		}
    		first = false;
    		peer000.add(peer0.get(i));
    
    	}
    	//System.out.println(peer000);
    	peer12 = new Peer(torrentInfoObj, peer000, clientGeneratedPeerID, args[1], pieceNumber, tracker);
    	peerList.add(peer12);
    	//System.exit(0);
    	//System.out.println("HEY");
    	//System.out.println(peer000);
    	//System.0);
    	
    	for(Peer p: peerList){
    		boolean handshaking12 = p.handshaking();
    		System.out.println(handshaking12);    	
    	}
    	
    	for(Peer p: peerList){
    		Thread one = new Thread(p);
        	one.start();
    	}
    	

		ListenForQuit listenForQuit = new ListenForQuit(tracker);
		Thread listenQuit = new Thread(listenForQuit);
		listenQuit.start();
		
		tracker.urlUpdateBeginDownloading();

    	long startTime = System.currentTimeMillis();
    	tracker.setTime(startTime);
    	
    	
    	/////////////////////////////////////////
    	
    	
   /* 	
    	ArrayList<String> peer00 = new ArrayList<String>();
    	peer00.add(peer0.get(0));
    	peer00.add(peer0.get(1));
    	peer00.add(peer0.get(2));
    	System.out.println(peer00);
    	
    	ArrayList<String> peer01 = new ArrayList<String>();
    	peer01.add(peer0.get(3));
    	peer01.add(peer0.get(4));
    	peer01.add(peer0.get(5));
    	System.out.println(peer01);
    	//peer
    	Peer peer = new Peer(torrentInfoObj, peer01, clientGeneratedPeerID, args[1], pieceNumber, tracker);
    	Peer peer1 = new Peer(torrentInfoObj, peer00, clientGeneratedPeerID, args[1], pieceNumber, tracker);
    	
    	boolean handshaking = peer.handshaking();
    	boolean handshaking1 = peer1.handshaking();
    	System.out.println(handshaking);
    	System.out.println(handshaking1);
    	
    	//lets user quit/save data
		ListenForQuit listenForQuit1 = new ListenForQuit(tracker);
		Thread listenQuit1 = new Thread(listenForQuit1);
		listenQuit1.start();
		
    	System.out.println("attempt downloading");
    	//let tracker know you started to download
    	tracker.urlUpdateBeginDownloading();
    	
    	long startTime1 = System.currentTimeMillis();
    	tracker.setTime(startTime1);
    	Thread one = new Thread(peer1);
    	one.start();
    	Thread zero = new Thread(peer);
    	zero.start();
    	
    	
*/
    
    	
    	
    }
    
}
