import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class P2 {
	static String hostName;
	static String hostAddress;
	static String port;
	static int hostNumber;
	static Set<String> allHosts = new HashSet<>();
	
	@SuppressWarnings("static-access")
	P2(String hostName, String hostAddress, String port) {
		this.hostName = hostName;
		this.hostAddress = hostAddress;
		this.port = port;
	}
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		hostName = args[0];
	    Scanner in = new Scanner(System.in);
		/*----Find available port and display it on the screen*/
	    ServerSocket serverSocket =  new ServerSocket(0); 
	    
	    InetAddress addr = InetAddress.getLocalHost();
	    hostAddress = String.valueOf(addr.getHostAddress());
		port = String.valueOf(serverSocket.getLocalPort());
		System.out.println(hostAddress + " at port number: " + port);
		
		System.out.println("Please add this host and port on another machine using:\nadd (" 
		+ hostName + " ," + hostAddress + ", " + port +")");
		
		allHosts.add(hostName + " " + hostAddress + " " +  port);
		
		/*************Create the directories for nets and tuples*****************/
		String dirPath = "/tmp/xwei1/linda/" + hostName;
		String netsPath = dirPath+ "/nets.txt";
		String tuplesPath = dirPath+ "/tuples.txt";
		String backupPath = dirPath+ "/backup.txt";
		
		File file = new File(dirPath);
		if(file.exists()) {
			System.out.println("Old Machine");
	    	ArrayList<String> hosts = Restart.getNetsFileFromBackup(netsPath, hostName, hostAddress, port);
	    	hostNumber = hosts.size();
	    	int hostId = Restart.getHostId(hosts, hostName);
	    	int backupId = Utils.getBackupId(hostId, hostNumber);
	    	String[] backupHostInfo = hosts.get(backupId).split(" ");
			Utils.updateTuplesFile(tuplesPath, backupHostInfo[1], backupHostInfo[2]);
			
			int backHostId = Utils.getBackHostId(hostId, hostNumber);
			String[] backHostInfo = hosts.get(backHostId).split(" ");
			Utils.updateBackupFile(backupPath, backHostInfo[1], backHostInfo[2]);
			
			String netsFile = Restart.getNetsfileFromHosts(hosts);
			Utils.updateAllNetsFile(hosts, netsFile);
			
		}else{
			System.out.println("New Machine");
			file.mkdirs();
	 	    file = new File(netsPath);
	 	    file.createNewFile();
	 	    file = new File(tuplesPath);
	 	    file.createNewFile();
		    file = new File(backupPath);
		    file.createNewFile();
		}
		
	    /****************Start Server******************/
	    try{
	    	Thread t = new Server(serverSocket, hostName, hostAddress, port, netsPath, tuplesPath, backupPath);
  	         t.start();
  	    }catch(IOException e){
  	         e.printStackTrace();
  	    }
	    
	    System.out.print("linda>");
	    /**************************************************/
	    
	    String command = in.nextLine();
	    command.trim();
	 
	    while(true) {
	    	
	    	/*************************If the command is "add"******************/
	    	if(command.startsWith("add")) {
	    		allHosts = Utils.getAllHostInfoIntoSet(netsPath);
	    		allHosts.add(hostName + " " + hostAddress + " " + port);
	    		
	    		String[] newHostInfo = Utils.getAddHostsInfo(command);//stored as {[h1, a1, p1], [h2, a2, p2]}
	    		Client client = new Client();
	    		
    			for(int i = 0; i < newHostInfo.length; ++i) {
    				String[] currentHostInfo = newHostInfo[i].split(" ");
    				String netsFile = client.getNetsfile(currentHostInfo[1], currentHostInfo[2]);
    				String[] netsFileArr = netsFile.split("\n");
    				for(String item: netsFileArr) {
    					if(!item.equals("\n") && item != null) {
    						allHosts.add(item);
    					}
    				}
    			}
    			
    			//Get the nets file and tuples from all the hosts
    			hostNumber = allHosts.size();
    			ArrayList<String> hostsArray = new ArrayList<>();
    			String[] dataTobeAllocated = Utils.getDataTobeAllocated(allHosts, hostsArray, hostNumber);
    			String netsFile = Utils.getAllNetsFile(hostsArray);
    			
    			Utils.updateAllNetsFile(hostsArray, netsFile);
    			
    			for(int i = 0; i < hostNumber; ++i) {
		    		String[] ori = hostsArray.get(i).split(" ");
	    			client.setTuples(ori[1], ori[2], dataTobeAllocated[i]);
		    		int backupId = Utils.getBackupId(i, hostNumber);
		    		String[] backup = hostsArray.get(backupId).split(" ");
		    		client.setBackup(backup[1], backup[2], dataTobeAllocated[i]);
		    	}
    			System.out.print("linda>");
    			
		    }else if(command.startsWith("delete")) {
		    	String[] hostsToBeDeleted =  Utils.getDeleteHostsInfo(command);
		    	
		    	allHosts = Utils.getAllHostInfoIntoSet(netsPath);
		    	hostNumber = allHosts.size();
		    	if(hostNumber == 1) {
		    		System.out.println("Only one host remains and it can't be deleted");
		    	}else{
		    		int newHostNumber = hostNumber - hostsToBeDeleted.length;
			    	System.out.println("newHostNumber: " + newHostNumber);
		    		
			    	ArrayList<String> hostsArray = new ArrayList<>();
	    			String[] dataTobeAllocated = Utils.getDataTobeAllocated(allHosts, hostsArray, newHostNumber);
	    			
	    			Client client = new Client();
			    	String remainNetsFile = "";
			    	ArrayList<String> remainHosts = new ArrayList<>();
			    	for(String item: allHosts) {
			    		String[] hostInfo = item.split(" ");
			    		int i = 0;
			    		for(i = 0; i < hostsToBeDeleted.length; ++i) {
			    			if(hostInfo[0].equals(hostsToBeDeleted[i])) {
			    				break;
			    			}
			    		}
			    		if(i == hostsToBeDeleted.length) {
			    			remainNetsFile += item + "\n";
			    			remainHosts.add(item);
			    		}else{
			    			client.deleteFile(hostInfo[1], hostInfo[2]);
			    		}
			    	}
			    	hostNumber = newHostNumber;
			    	for(int i = 0; i < newHostNumber; ++i) {
			    		String[] ori = remainHosts.get(i).split(" ");
			    		client.add(ori[1], ori[2], remainNetsFile);
			    		client.setTuples(ori[1], ori[2], dataTobeAllocated[i]);
			    		
			    		int backupId = Utils.getBackupId(i, hostNumber);
			    		String[] backup = remainHosts.get(backupId).split(" ");
			    		client.setBackup(backup[1], backup[2], dataTobeAllocated[i]);
			    	}
		    	}
		    	System.out.print("linda>");
		    	
		    }else if(command.startsWith("out")) {
		    	hostNumber = Utils.getHostNumber(netsPath);
		    	ArrayList<String> hosts = Utils.getAllHostInfoIntoList(netsPath);
		    	String strToOut = Utils.preprocess(command);//get the string to store
		    	
		    	//Get the host to store the tuple by hashing and send the "out tuple" request to corresponding host
		    	int hostId = Hash.md5(strToOut, hostNumber);
		    	String[] targetInfo = hosts.get(hostId).split(" ");
	    		int backupId = Utils.getBackupId(hostId, hostNumber);
	    		String[] backupHostInfo = hosts.get(backupId).split(" ");
	    		
	    		Client client = new Client();
	    		if(client.checkServerStatus(targetInfo[1], targetInfo[2])) {
	    			client.out(targetInfo[1], targetInfo[2], strToOut);
	    			if(client.checkServerStatus(backupHostInfo[1], backupHostInfo[2])) {
	    				client.outBackup(backupHostInfo[1], backupHostInfo[2], strToOut);
	    			}
	    		}else{
	    			System.out.println("connection to original failed and try to connect to backup host");
	    			client.outBackup(backupHostInfo[1], backupHostInfo[2], strToOut);
	    		}
	    		System.out.print("linda>");
	    		
		    }else if(command.startsWith("in")) {
		    	hostNumber = Utils.getHostNumber(netsPath);
		    	ArrayList<String> hosts = Utils.getAllHostInfoIntoList(netsPath);
		    	String strToIn = Utils.preprocess(command);//get the string to delete
		    	
		    	if(!strToIn.contains("?")) {//If exact match
		    		//Get the host to store the tuple by hashing and send the "out tuple" request to corresponding host
		    		int hostId = Hash.md5(strToIn, hostNumber);
			    	String[] targetInfo = hosts.get(hostId).split(" ");
			    	int backupId = Utils.getBackupId(hostId, hostNumber);
		    		String[] backupHostInfo = hosts.get(backupId).split(" ");
		    		
		    		Client client = new Client();
		    		if(client.checkServerStatus(targetInfo[1], targetInfo[2])) {
		    			while(!client.rdo(targetInfo[1], targetInfo[2], strToIn)) {
		    				Thread.sleep(1000);
			    		}
		    			client.ino(targetInfo[1], targetInfo[2], strToIn);
		    			if(client.checkServerStatus(backupHostInfo[1], backupHostInfo[2])) {
		    				client.inBackup(backupHostInfo[1], backupHostInfo[2], strToIn);
		    			}
		    		}else{
		    			System.out.println("connection to original failed and try to connect to backup host");
		    			client.inBackup(backupHostInfo[1], backupHostInfo[2], strToIn);
		    		}
		    		
		    		System.out.print("linda>");
		    	}else{
		    		/* Create n threads to broadcast the message to all the hosts. 
		    		 * If not found, current host will block, waiting for available tuple at all the hosts.
		    		 * If found, random host will be chosen and send back the host machine to current host
		    		 * Then current machine will delete the tuple
		    		 */
		    		Thread[] broadcastThread = new BroadcastThread[hostNumber];
		    		SharedInfo sharedInfo = new SharedInfo();
		    		Utils.broadCast(netsPath, hostNumber, broadcastThread, sharedInfo, strToIn);
		    		
	    			Client client = new Client();
	    			System.out.println("The tuple to be deleted is: [" + sharedInfo.tuples + "]");
		    		System.out.println("The tuple will be removed from: " + sharedInfo.hostAddress + ". Port number is: " + sharedInfo.port);
		    		client.ino(sharedInfo.hostAddress, sharedInfo.port, sharedInfo.tuples);// Remove the tuple using exact match command
		    		System.out.print("linda>");
		    	}
		    	
		    }else if(command.startsWith("rd")) {
		    	hostNumber = Utils.getHostNumber(netsPath);
		    	ArrayList<String> hosts = Utils.getAllHostInfoIntoList(netsPath);
		    	String strToRd = Utils.preprocess(command);//get the string to read
		    	
		    	if(!strToRd.contains("?")) {
		    		//Get the host to store the tuple by hashing and send the "out tuple" request to corresponding host
		    		int hostId = Hash.md5(strToRd, hostNumber);
			    	String[] targetInfo = hosts.get(hostId).split(" ");
		    		int backupId = Utils.getBackupId(hostId, hostNumber);
		    		String[] backupHostInfo = hosts.get(backupId).split(" ");
		    		
		    		Client client = new Client();
		    		if(client.checkServerStatus(targetInfo[1], targetInfo[2])) {
		    			while(!client.rdo(targetInfo[1], targetInfo[2], strToRd)) {
			    			Thread.sleep(1000);
			    		}
		    			System.out.println("tuple found on " + targetInfo[0]);;
		    		}else{
		    			System.out.println("connection to original failed and try to connect to backup host");
		    			while(!client.rdu(backupHostInfo[1], backupHostInfo[2], strToRd)) {
			    			
			    		}
		    			System.out.println("tuple found on backup machine" + backupHostInfo[0]);;
		    		}
		    		System.out.print("linda>");
		    	}else{
		    		Thread[] broadcastThread = new BroadcastThread[hostNumber];
		    		SharedInfo sharedInfo = new SharedInfo();
		    		Utils.broadCast(netsPath, hostNumber, broadcastThread, sharedInfo, strToRd);
		    		System.out.println("The tuple to be read is: [" + sharedInfo.tuples + "]");
		    		System.out.println("The tuple will be read from: " + sharedInfo.hostAddress + ". Port number is: " + sharedInfo.port);
		    		System.out.print("linda>");
		    	}
		    }else{
		    	System.out.println("Command Wrong! Must start with add, out, in or rd. Please input again.");
		    }
		    command = in.nextLine();
		    command.trim();
	    }
	    
	}
	
}
