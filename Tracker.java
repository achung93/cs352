import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class Tracker {

	TorrentInfo torrentInfoObj;
	String peerID;
	BufferedInputStream trackerResponse;
	URL url;
	HttpURLConnection connection;
	boolean[] pieces = null;
	
	public Tracker(TorrentInfo tracker, String peerID){
		this.torrentInfoObj = tracker;
		this.peerID = peerID;
		this.trackerResponse = null;
		this.url = null;
		pieces = new boolean[torrentInfoObj.piece_length*3];
	}
	
	public synchronized boolean checkPiece(int pieceNum){
		return pieces[pieceNum];
	}
	
	public synchronized void setPiece(int pieceNum){
		pieces[pieceNum] = true;
	}

	public void closeAll(){
		try {
			trackerResponse.close();
		} catch (IOException e) {
			System.exit(0);
		}
		
		connection.disconnect();
		
	}
	/*
	 * converts byte array to url safe string.
	 * prolly a better way of doing this.
	 * not sure why doesnt work all the time
	 */
	public String Conversion(byte X[]){		
		byte curr = 0x00;
		String[] hexvalue = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"}; 
		String URLE = "";
		
		
		for(int i = 0; i < X.length; i++)
		{
			curr = (byte)((byte)((byte)(X[i] & 0xF0) >>> 4) & 0x0F); 
			URLE = URLE + "%"+hexvalue[(int)curr];
			curr = (byte)(X[i] & 0x0F); 
			URLE = URLE + hexvalue[(int)curr];
		}
		System.out.println("URLE is " + URLE);
		return URLE;
	}
	
	public String toUrlS(byte URL[]) throws BencodingException
	{
		
		if (URL == null || URL.length <=0){
			return null;
		}
		
		else{
		return Conversion(URL);
		}
	}
	
	/*public String byteArrayToURLString(byte toURL[])
	{
		//alternative way, doesn't work rn.
		/*
		String urlReady = "";
		String decryptedBytes = new String(toURL);
		System.out.println(decryptedBytes);
		for(int i = 0; i < decryptedBytes.length(); i++){
			String hexVal = (Integer.toHexString(decryptedBytes.charAt(i)));
			//without if statement, get torrent prohibited message
			if (hexVal.length() == 1){	//eg- F turns to 0F
				hexVal = 0 + hexVal;
			}
			urlReady+= "%" + hexVal;
		}
		return urlReady;
		*/
		//String s = new String(toURL);
		//return URLEncoder.encode(s);
	//}
	
	/*
	 * update url with field'event' field and tells event was started
	 */
	public void urlUpdateBeginDownloading(){
		try {
			url.openConnection().addRequestProperty("event", "started");
			trackerResponse = new BufferedInputStream((url.openConnection()).getInputStream());
		} catch (IOException e2) {
			System.out.println("error in url");
		}
	}
	/*
	 * updates url with field depending on download success
	 */
	public void urlFinishDownloading(String eventType){
		try {
			url.openConnection().setRequestProperty("event", eventType);
			trackerResponse = new BufferedInputStream(connection.getInputStream());
		} catch (IOException e2) {
			System.out.println("error in url");
		}
	}
	
	public ByteArrayOutputStream getTrackerResponse() throws BencodingException{
		
		String fileName = torrentInfoObj.announce_url.getFile() + "?";
		
		String info_hash = "info_hash=" + toUrlS(torrentInfoObj.info_hash.array());
		
		String peer_id = "&peer_id=" + toUrlS(peerID.getBytes());
		
		String port ="&port=" + "6882";
		
		String uploaded = "&uploaded=" + "0";
		
		String downloaded = "&downloaded=" + "0";
		
		String left = "&left=" + Integer.toString(torrentInfoObj.file_length);	//-1??
		String prefix = "http";
		String hostURL = torrentInfoObj.announce_url.getHost();
		int portURL = torrentInfoObj.announce_url.getPort();
		String all_parameters = fileName + info_hash + peer_id + port + uploaded + downloaded + left;
		
		try {
			url = new URL(prefix, hostURL, portURL, all_parameters);
		} catch (MalformedURLException e) {
			System.out.println("done screwed up on the url");
			System.exit(0);
		}
		
		
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			trackerResponse = new BufferedInputStream(connection.getInputStream());
		} catch (IOException e2) {
			System.out.println("error in url");
		}
		
		//reads stream byte by byte, saves to array output stream.
		//http://www.tutorialspoint.com/java/io/inputstream_read_byte.htm
		ByteArrayOutputStream trackerResponseSave = new ByteArrayOutputStream();
		byte[] buffer = new byte[1];
		try {
			while (trackerResponse.read(buffer) != -1) // read the response
			{
				for(byte b: buffer){
				trackerResponseSave.write(b);
				}
			}
		} catch (IOException e1) {
			System.out.println("trouble reading tracker response");
		}
		
		try {
			trackerResponse.close();
		} catch (IOException e) {
			System.out.println("problem closing the reader");
		}
		
		return trackerResponseSave;
	}
	
	/*
	 * method selects peer with appropriate peer id.
	 * input is outputstream from tracker.
	 * output is arraylist that contains peerID in 0th spot,
	 * peer ip in 1st spot, peer port in 2nd spot.  if more than 1
	 * valid peer, will continue like that
	 * (
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getPeer(ByteArrayOutputStream trackerResponse1){
		
		byte[] trackerResponseArray = trackerResponse1.toByteArray();
		Map<Object, Object> trackerResponseDecode = null;
		ArrayList<String> peers = new ArrayList<String>();
		
		try {
			trackerResponseDecode = (Map<Object, Object>) Bencoder2.decode(trackerResponseArray);
		} catch (BencodingException e) {
			System.out.println("decoding trakcer response error");
			System.exit(0);
		}
		
		List<Map<Object, Object>> peersList = (List<Map<Object, Object>>) trackerResponseDecode.
				get(ByteBuffer.wrap("peers".getBytes()));
		
		for (Map<Object, Object> rawPeer : peersList) {
			String ip = new String(((ByteBuffer) rawPeer.get(ByteBuffer.wrap("ip".getBytes()))).array());
			int peerPort = (int) (rawPeer.get(ByteBuffer.wrap("port".getBytes())));
			String peerIDString = new String(((ByteBuffer) rawPeer.get(ByteBuffer.wrap("peer id".getBytes()))).array());
			
			
			if(peerIDString.substring(1, 3).equals("RU") || peerIDString.substring(0, 2).equals("RU")){
				peers.add(peerIDString);
				peers.add(ip);
				peers.add(Integer.toString(peerPort));
			}
			
		}
		
		if(peers.size() <3){
			System.out.println("could not find peer");
			System.exit(0);
		}
		return peers;
	}
	
	/*
	 * not used rn/not completed
	 * nicer way of finding list of peers with acceptable id
	 */
	public ArrayList<String> quickRegexPeers(ArrayList<String> peerIDList, ArrayList<String> totalList){
		return null;
		
	}
	
	public long startTime;
	public void setTime(long start){
		startTime = start;
	}
	
}
