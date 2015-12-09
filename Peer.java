import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;


public class Peer implements Runnable{

	TorrentInfo torrentInfoObj = null;
	ArrayList<String> peer = null;
	String peerID, peerIP, clientGeneratedID = null;
	int peerPort = -1;
	ByteArrayOutputStream handshakeOutputStream;
	Socket socket;
	Tracker tracker =null;
	String args1;
	
	DataOutputStream clientOutput;
	DataInputStream peerInput;
	int pieceIndex;
	int pieceNumber;
	
	public Peer(TorrentInfo torrObj, ArrayList<String> peer0, String clientGeneratedID, String args1, int pieceNumber, Tracker tracker){
		this.torrentInfoObj = torrObj;
		this.peerID = peer0.get(0);
		this.peerIP = peer0.get(1);
		this.peerPort = Integer.parseInt(peer0.get(2));
		this.clientGeneratedID = clientGeneratedID;
		this.socket = null;
		this.clientOutput = null;
		this.peerInput = null;
		this.handshakeOutputStream = new ByteArrayOutputStream();
		this.args1 = args1;
		this.pieceNumber = pieceNumber;
		this.tracker = tracker;
	}
	/*
	 * conducts handshaking between peer and clinet.
	 * handshaking described here: 
	 * https://wiki.theory.org/BitTorrentSpecification
	 * ctf+F: The handshake is a required message and must
	 */
	public boolean handshaking(){
		
		//make connections to peer
		try {
			socket = new Socket(peerIP, peerPort);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.out.println("bad/incorrect peer contact");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("possbily bad port format: " + "peerPort");
			System.exit(0);
		}  
		
		//handshake: <pstrlen><pstr><reserved><info_hash><peer_id>
		
		byte pstr[] = "BitTorrent protocol".getBytes();
		
		byte pstrlen = (byte)pstr.length;
		
		byte reserved[] = new byte[8];
		Arrays.fill(reserved, (byte)0);
		
		byte info_hash[] = torrentInfoObj.info_hash.array();
		
		byte peer_id[] = clientGeneratedID.getBytes();
	
		handshakeOutputStream.write(pstrlen);
		
		try {
			handshakeOutputStream.write(pstr);
		} catch (IOException e) {
			System.out.println("pstr problem handshake");
			System.exit(0);
		}
		try {
			handshakeOutputStream.write(reserved);
		} catch (IOException e) {
			System.out.println("reserved problem handshake");
			System.exit(0);
		}
		try {
			handshakeOutputStream.write(info_hash);
		} catch (IOException e) {
			System.out.println("info_hash error handshake");
			System.exit(0);
		}
		try {
			handshakeOutputStream.write(peer_id);
		} catch (IOException e) {
			System.out.println("peerid problem handshake");
			System.exit(0);
		}
		byte handshake[] = handshakeOutputStream.toByteArray( );
		
		//set up streams
		try {
			clientOutput = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			peerInput = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			clientOutput.write(handshake);
			clientOutput.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		byte[] recievedByteArr = new byte[handshake.length];
		for(int i =0; i < handshake.length; i++){
				try {
					recievedByteArr[i] = (byte)peerInput.read();
				} catch (IOException e) {
					System.out.println("problem with handshake response");
				}
		}


		
		byte[] clientID = peerID.getBytes();
		
		byte[] peerResponseID = new byte[20];
		int clientIpeerInputdex = 0;
		for(int i =0; i < handshake.length; i++){
			if(i < handshake.length - 20){
				if(handshake[i] != recievedByteArr[i]){
				
					return false;
				}
			
			}else{
				peerResponseID[clientIpeerInputdex] = recievedByteArr[i];
				clientIpeerInputdex++;
			}
		}
		
		String clientIDString = new String(clientID);
		String peerIDString = new String(peerResponseID);
		
		if(!clientIDString.equals(peerIDString)){
			return false;
		}
		return true;
	}
	
	/*
	 * helper method for message-
	 * converts an integer into byte array in big endian form
	 * http://stackoverflow.com/questions/1936857/convert-integer-into-byte-array-java
	 */
	public byte[] bigEndian(int intToBigEnd) {
		ByteBuffer bigEndArray = ByteBuffer.allocate(4);
		bigEndArray.order(ByteOrder.BIG_ENDIAN);
		bigEndArray.putInt(intToBigEnd);
		return bigEndArray.array();
	}
	
	/*
	 * helper method
	 * converts byte array in big endian form into integer
	 * http://stackoverflow.com/questions/5616052/how-can-i-convert-a-4-byte-array-to-an-integer
	 */
	public int BigEndianToInt(byte[] toConvert){
		return java.nio.ByteBuffer.wrap(toConvert).getInt();
	}
	
	/*
	 * builds a message that will be sent to peer based on parameters described by protocol here:
	 * https://wiki.theory.org/BitTorrentSpecification#Messages
	 * code length could be reduced by grouping similarly parametrized methods,
	 * but with this it's very easy to understand how the message is built.
	 */
	public byte[] message(int length_prefix, int message_ID, int index, int begin, int lengthOrBlock) throws IOException{
		//note that in this current project, only 1,2, 4 and 6 were used/tested 
		byte[] messageToSend = null;
		switch(message_ID){
		   
			case -1:	//keep alive
				ByteArrayOutputStream keepAlive = new ByteArrayOutputStream();
				keepAlive.write(bigEndian(0));
				messageToSend = keepAlive.toByteArray();
			case 0:	//choke: <length prefix> is 1 and message ID is 0. There is no payload.
				ByteArrayOutputStream zero = new ByteArrayOutputStream();
				zero.write(bigEndian(1));
				zero.write((byte)0);
				messageToSend = zero.toByteArray();
				
			case 1: // unchoke: <length prefix> is 1 and the message ID is 1. There is no payload.
				ByteArrayOutputStream one = new ByteArrayOutputStream();
				one.write(bigEndian(1));
				one.write((byte)1);
				messageToSend = one.toByteArray();
				break;
			case 2:	//interested
				ByteArrayOutputStream two = new ByteArrayOutputStream();
				two.write(bigEndian(1));
				two.write((byte)2);
				messageToSend = two.toByteArray();
				break;
			case 3:	//not interested
				ByteArrayOutputStream three = new ByteArrayOutputStream();
				three.write(bigEndian(1));
				three.write((byte)3);
				messageToSend = three.toByteArray();
				break;
			case 4: //have
				ByteArrayOutputStream four = new ByteArrayOutputStream();
				four.write(bigEndian(5));
				four.write((byte)4);
				four.write(bigEndian(index));
				messageToSend = four.toByteArray();
				break;
			case 5: //bitfield
				ByteArrayOutputStream five = new ByteArrayOutputStream();
				five.write(bigEndian(1 + lengthOrBlock));
				five.write((byte)5);
				five.write(bigEndian(index));
				messageToSend = five.toByteArray();
				break;
			case 6:	//request piece
				ByteArrayOutputStream six = new ByteArrayOutputStream();
				six.write(bigEndian(13));
				six.write((byte)6);
				six.write(bigEndian(index));
				six.write(bigEndian(begin));
				six.write(bigEndian(lengthOrBlock));
				messageToSend = six.toByteArray();
				break;
			case 7:	//send piece
				ByteArrayOutputStream seven = new ByteArrayOutputStream();
				seven.write(bigEndian(9 + lengthOrBlock));
				seven.write((byte)7);
				seven.write(bigEndian(index));
				messageToSend = seven.toByteArray();
				break;
			case 8:
				ByteArrayOutputStream eight = new ByteArrayOutputStream();
				eight.write(bigEndian(13));
				eight.write((byte)8);
				eight.write(bigEndian(index));
				eight.write(bigEndian(begin));
				eight.write(bigEndian(lengthOrBlock));
				messageToSend = eight.toByteArray();
			case 9:
				ByteArrayOutputStream nine = new ByteArrayOutputStream();
				nine.write(bigEndian(3));
				nine.write((byte)9);
				nine.write(bigEndian(index));
				messageToSend = nine.toByteArray();
		}
		if(messageToSend==null){
			System.out.println("error: meesageID out of range");
			System.exit(0);
		}
		return messageToSend; 
	}
	
	public boolean verifySHA1(ByteArrayOutputStream building_piece, int pieceIndex){
		
		ByteBuffer indHash  = torrentInfoObj.piece_hashes[pieceIndex];
		byte[] ind = indHash.array();
		
		ByteBuffer builder = ByteBuffer.wrap(building_piece.toByteArray());
		byte[] pieceHash=null;
		try {
			pieceHash = Bencoder2.encode(builder);
						
		} catch (BencodingException e) {
			System.out.println("oh well");
		}
		if(ind == pieceHash){
			return true;
		}
		
		return true;
	}
	

	public boolean downloadFile() throws Exception {
		System.out.println("in download");
		//contact tracker to know that downloading has begun		
		ArrayList<byte[]> all_pieces = new ArrayList<byte[]>();	//will have arrayList of all pieces
		ByteArrayOutputStream building_piece = new ByteArrayOutputStream();	//used to store partial pieces
		
		byte[] piecePart = null;
		
		//clear bytes from stream
		byte[] pass = new byte[1];
		boolean notDone = true;
		while(notDone){
			pass[0] = peerInput.readByte();
			if (pass[0] == (byte)-1 ) {
				notDone = false;
				}
		}
		 //interest message
		clientOutput.write(message(0,2, 0, 0, 0));
		clientOutput.flush();
		System.out.println("here");

		//unchoke response after interest??
		//not being recieved rn, not sure if necessary
		byte[] unchokeCheck = new byte[1];
		for (int i = 0; i < 5; i++) {
			unchokeCheck[0] = peerInput.readByte();
			if (unchokeCheck[0]==(byte)1 && i==4 ) {
				System.out.println("got the unchoke");
				}
		}
		
		//set up variables to handle special case of partial last piece
		int nonLastPieces = torrentInfoObj.piece_hashes.length - 1;
		int lastPieceSize = torrentInfoObj.file_length % torrentInfoObj.piece_length;
		
		//needs to be args[1]
		FileOutputStream fileoutput = new FileOutputStream(new File(args1));
		int begin = 0;
		pieceIndex = 0;
		
		for(pieceIndex = pieceNumber; pieceIndex < nonLastPieces; pieceIndex++){
			//all pieces split into 2 blocks/messages
			for(int j=0; j<2; j++){
					//request piece to peer
				    //int length_prefix, int message_ID, int index, int begin, int lengthOrBlock
					byte[] messageToPeer = message(13, 6, pieceIndex, begin, 16384);
					
					clientOutput.write(messageToPeer);
					clientOutput.flush();

					//leading bytes before data
					//13 total bytes- 4 in len, 1 in id, 4 in index, 4 in begin
					byte[] returnedInfo = new byte[13];
					for (int i = 0; i < 13; i++) {
						returnedInfo[i] = peerInput.readByte();
					}
					
					

					piecePart = new byte[16384];
					for (int i = 0; i < 16384; i++) {
						piecePart[i] = peerInput.readByte();
					}
					
					//write data out to inputted file
					if(begin == 16384){
						if(!tracker.checkPiece(pieceIndex*2 + 1)){
							fileoutput.write(piecePart);
							tracker.setPiece(pieceIndex*2+1);
						}
					}else{
						if(!tracker.checkPiece(pieceIndex*2)){
							fileoutput.write(piecePart);
							tracker.setPiece(pieceIndex*2);
						}
					}

					if (begin == 16384) {	//basically if j==1/completed piece
						begin = 0;
						building_piece.write(piecePart);
						all_pieces.add(building_piece.toByteArray());
						//notify peer of completing the piece
						if (verifySHA1(building_piece, pieceIndex)){
						clientOutput.write(message(5, 4, pieceIndex, 0, 0));
						}
						building_piece.flush();
						System.out.println();
						System.out.println(pieceIndex);
					} else if(begin == 0){	//if j==0/partial piece
						building_piece.write(piecePart);
						begin = 16384;
					}
			}				
		}
		
		
		/*
		 * last piece case which may be irregular size.
		 * note that this code is redundant but it cuts down on comparisons/
		 * saves on time and also makes it a bit easier to follow what's going on.
		 */
		int part1size = -1, part2size = -1;
		if (lastPieceSize == 0){	//lastpiece is a full block
			part1size = 16384;
			part2size = 16384;
		}else{
			int lastPart = lastPieceSize/16384;
			if(lastPart == 1){
				part1size = 16384;
				part2size = lastPieceSize%16384;
			}
			if(lastPart == 0){
				part1size=lastPieceSize%16384;
				part2size = 0;
			}
		}
		
		//note here that the 1st subpiece can not be 0
		int blockLength = -1;
		for(int j=0; j<2; j++){
			
				if(j ==1 && part2size==0){ //no message to send in this case
					building_piece.write(piecePart);
					all_pieces.add(building_piece.toByteArray());
					if (verifySHA1(building_piece, j)){
						clientOutput.write(message(5, 4, pieceIndex, 0, 0));
						clientOutput.flush();
						}
					System.out.println(pieceIndex);
					building_piece.flush();
					//do stuff and continue which will kick out of the for loop
					continue;
				}
				
				//find required block length
				if(j == 0){
					blockLength = Math.min(16384, part1size);
				}else if(j == 1){
					blockLength = Math.min(16384, part2size);
				}
				
				byte[] messageToPeer = message(13, 6, pieceIndex, begin, blockLength);
				
				clientOutput.write(messageToPeer);
				clientOutput.flush();
				
				byte[] returnedInfo = new byte[13];
				for (int i = 0; i < 13; i++) {
					returnedInfo[i] = peerInput.readByte();
				}
				
				piecePart = new byte[blockLength];
				for (int i = 0; i < blockLength; i++) {
					piecePart[i] = peerInput.readByte();
				}
				
				//write the subset to the file
				fileoutput.write(piecePart);
				
				if (j == 1) {  //done no matter what here
					System.out.println(pieceIndex);
					building_piece.write(piecePart);
					all_pieces.add(building_piece.toByteArray());
					
					if (verifySHA1(building_piece, j)){
						clientOutput.write(message(5, 4, pieceIndex, 0, 0));
						clientOutput.flush();
						}
					building_piece.flush();
				} else if(j == 0){  //save to temp, check piece 2
					building_piece.write(piecePart);
					begin = 16384;
				}
		}
		
		
		
		long endTime = System.currentTimeMillis();
		System.out.println("total Download Time: " + (endTime - tracker.startTime));
		tracker.urlFinishDownloading("completed");
		
		//closing stuff
		fileoutput.close();
		clientOutput.close();
		peerInput.close();
		socket.close();
		
		
		//need to print the time here 
		//close file n stuff, verify sha1
		return true;
	}
	@Override
	public void run() {
		try {
			downloadFile();
		} catch (Exception e) {
			return;
		}
		
	}
	public boolean isThisChoked() {
		
		return false;
	}
	
}
