import java.io.*; 
import java.util.*;
import java.net.*;

public class dvapp extends Thread{

	ServerSocket listener;
	static int port;
	static int time;
	String ip;
	public static Node myNode = null;
	ArrayList<ConnectionThread> active;
	static int yourID = 0;
	static Map<Node, Integer> myRoutingTable = new HashMap<Node, Integer>();
	static Set<Node> neighbors = new HashSet<Node>();
	public static Map<Node,Node> hopSequence = new HashMap<Node,Node>();

	static List<Node> nodes = new ArrayList<Node>();

	public dvapp (int port) throws IOException {
		listener = new ServerSocket();
		listener.bind(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port));
		this.port = listener.getLocalPort();
		this.ip = InetAddress.getLocalHost().getHostAddress();
		this.active = new ArrayList<ConnectionThread>();
	}

	public static Node getNodeFromId(int id){
		for(Node node:nodes) {
			if(node.getId() == id) {
				return node;
			}
		}
		return null;
	}

	public static void readTopologyFile(String filename) {

		File file = new File("./"+filename);

		try {
			Scanner scanner = new Scanner(file);
			int numOfServers = scanner.nextInt();
			int numberOfNeighbors = scanner.nextInt();

			scanner.nextLine();

			for(int i = 0; i<numOfServers; i++) {
				String line = scanner.nextLine();
				String[] split = line.split(" ");

				Node node = new Node(Integer.parseInt(split[0]), split[1],
						Integer.parseInt(split[2]));
				nodes.add(node);

				int cost = Integer.MAX_VALUE;
				if(i == 0) {
					yourID = Integer.parseInt(split[0]);
					myNode = node;
					cost = 0;
					port = node.getPort();
					hopSequence.put(node, myNode);
				}
				else{
					hopSequence.put(node, null);
				}

				myRoutingTable.put(node, cost);
			}

			for(int i = 0; i<numberOfNeighbors ; i++) {
				if(scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String[] split = line.split(" ");
					int fromNode = Integer.parseInt(split[0]);
					int toNode = Integer.parseInt(split[1]); 
					int cost = Integer.parseInt(split[2]);

					if(fromNode == yourID) {
						Node to = getNodeFromId(toNode);
						myRoutingTable.put(to, cost);
						neighbors.add(to);
						hopSequence.put(to, to);
					}

					if(toNode == yourID) {
						Node from = getNodeFromId(fromNode);
						myRoutingTable.put(from, cost);
						neighbors.add(from);
						hopSequence.put(from, from);
					}
				}
			}

			scanner.close();
		} catch (FileNotFoundException e) {
			System.out.println("** "+file.getAbsolutePath()+" not found. **");
		}

	}

	void removeTerminatedDataConnections() {

		ConnectionThread[] c = active.toArray(new ConnectionThread[active.size()]);

		ArrayList<ConnectionThread> newlist = new ArrayList<ConnectionThread>();
		for(int m=0; m<c.length; m++){
			if(c[m].getSocket() != null){
				newlist.add(c[m]);
			}
		}

		active = newlist;
	}

	private static void helpMethodDisplay() {
		System.out.println("\n");
		System.out.println("******Distance Vector Routing Protocol*******");
		System.out.println("--> Commands available");
		System.out.println("1. server <topology-file> -i <time-interval-in-seconds>");
		System.out.println("2. update <server-id1> <server-id2> <new-cost>");
		System.out.println("3. step");
		System.out.println("4. display");
		System.out.println("5. disable <server-id>");
		System.out.println("6. crash");
		System.out.println("7. help");

	}

	// program starts execution here
	public static void main (String[] args) {
		int count = 0;
		Timer timer = new Timer();

		try {

			while(true) {

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				System.out.println("->>");
				Scanner input = new Scanner(System.in);
				String userCommand = input.nextLine();
				String[] commands = userCommand.split(" ");

				switch(commands[0]) {

				// server -t <topology-file-name> -i <routing-update-interval>
				case "server":
					if(commands.length!=5){
						System.out.println("** Incorrect length of command. Please try again. **");
						break;
					}
					try{
						if(Integer.parseInt(commands[4])>0){
						}
					}catch(NumberFormatException nfe){
						System.out.println("** Please input an integer for the interval. **");
						break;
					}
					if((!commands[1].equals("-t") || commands[2]=="" || !commands[3].equals("-i") || commands[4]=="")){
						System.out.println("** Incorrect format of command. Please try again. **");
						break;
					}

					else {
						String filename = commands[2];

						if(count == 0) {
							readTopologyFile(filename);
							System.out.println("** Starting to listen on port "+port+" **");
							dvapp mainApp = new dvapp(port);
							Thread t1 = new Thread(mainApp);
							t1.start();

							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							for(Node node : nodes) {

								boolean duplicate = false;
								for(int x = 0; x<mainApp.getActive().size(); x++) {
									if(node.getPort() == mainApp.getActive().get(x).getPort() &&
											node.getIpAddress() == mainApp.getActive().get(x).getIp()) {
										duplicate = true;
										continue;
									}
								}

								if(!duplicate && node.getPort()!= port) {
									ConnectionThread newC = new ConnectionThread(node.getIpAddress(), node.getPort(), null);
									if(!mainApp.getActive().contains(newC)) {
										mainApp.getActive().add(newC);
										Thread tc = new Thread(newC);
										tc.start();
									}
								}

							}

							time = Integer.parseInt(commands[4]);
							timer.scheduleAtFixedRate(new TimerTask() {

								@Override
								public void run() {
									//									System.out.println("** Sending table to neighbors **");
								}

							}, time*1000, time*1000);

						}
					}

					count ++;
					break;

				case "update":
					update(Integer.parseInt(commands[1]),Integer.parseInt(commands[2]),Integer.parseInt(commands[3]));
					break;

				case "step":
					System.out.println("step");
					break;

				case "display":
					display();
					break;

				case "disable":

					break;

				case "crash":
					System.out.println("Couldn't finish it");
					break;

				case "help":
					helpMethodDisplay();
					break;

				default:
					System.out.println("Invalid Command. Use \"help\" command to see available options.");
					break;

				}

			}

		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	private static void display() {
		System.out.println("Source"+ " Next-Hop"+" Cost");
		Collections.sort(nodes,new NodeComparator());
		for(Node node:nodes){
			int cost = myRoutingTable.get(node);
			String costStr = ""+cost;
			if(cost==Integer.MAX_VALUE){
				costStr = "infinity";
			}
			String nextHopID = "N.A";
			if(hopSequence.get(node)!=null){
				nextHopID = ""+hopSequence.get(node).getId(); 
			}
			System.out.println(""+node.getId()+" "+nextHopID+" "+costStr);
		}		
	}

	private static void update(int ser1, int ser2, int cost) {

		if(ser1 == yourID) {
			Node to = getNodeFromId(ser2);

		}

	}

	private static void establishConnections() {


	}

	public void run() {

		Socket socket = null;
		ConnectionThread client = null;
		System.out.println("Started listening for data on "+ this.ip + " port " + this.port + "...\nUse \"help\" for available commands.\n->>");

		while(true) {

			try {
				socket = listener.accept();
				client = new ConnectionThread("", 0 ,socket);

				active.add(client);
				removeTerminatedDataConnections();

				DataInputStream incoming = new DataInputStream(socket.getInputStream());
				String msg;

				while( (msg = incoming.readUTF()) != null) {
					System.out.println("\nRECEIVED A MESSAGE FROM SERVER AT "+
							socket.getRemoteSocketAddress().toString().substring(1, socket.getRemoteSocketAddress().toString().indexOf(":")) + " " + msg + "\n->>");
				}

				client.start();
			}
			catch ( IOException e) {
				System.out.println("** IP: "+ 
						socket.getRemoteSocketAddress().toString().substring(1, socket.getRemoteSocketAddress().toString().indexOf(":")) +
						" has left the network. **\n->>");
			}
			finally {
				client.terminate();
			}

		}

	}

	public ServerSocket getListener() {
		return listener;
	}

	public void setListener(ServerSocket listener) {
		this.listener = listener;
	}

	public static int getPort() {
		return port;
	}

	public static void setPort(int port) {
		dvapp.port = port;
	}

	public static int getTime() {
		return time;
	}

	public static void setTime(int time) {
		dvapp.time = time;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public ArrayList<ConnectionThread> getActive() {
		return active;
	}

	public void setActive(ArrayList<ConnectionThread> active) {
		this.active = active;
	}

	public static int getMyID() {
		return yourID;
	}

	public static void setMyID(int myID) {
		dvapp.yourID = myID;
	}

	public static Map<Node, Integer> getMyRoutingTable() {
		return myRoutingTable;
	}

	public static void setMyRoutingTable(Map<Node, Integer> myRoutingTable) {
		dvapp.myRoutingTable = myRoutingTable;
	}

	public static Set<Node> getNeighbors() {
		return neighbors;
	}

	public static void setNeighbors(Set<Node> neighbors) {
		dvapp.neighbors = neighbors;
	}

	public static List<Node> getNodes() {
		return nodes;
	}

	public static void setNodes(List<Node> nodes) {
		dvapp.nodes = nodes;
	}

}


class ConnectionThread extends Thread{

	private String ip;
	private int port;
	private Socket socket;

	public ConnectionThread(String ip, int port, Socket client) throws UnknownHostException, IOException{
		if(client == null) {
			System.out.println("Connected to "+ip +" on "+port);
			this.socket = new Socket(ip, port);
			this.ip = ip;
			this.port = port;
		}
		else {
			System.out.println("Connected to "+client.getInetAddress().getHostAddress().toString() +" on "+client.getPort());
			this.socket = client;
			this.ip = client.getInetAddress().getHostAddress().toString();
			this.port = client.getPort();
		}

	}

	public boolean terminate() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void run() {
		try {

			DataInputStream incoming = new DataInputStream(socket.getInputStream());
			String msg;

			while( (msg = incoming.readUTF()) != null) {
				System.out.print("\nReceived message from IP: "+ 
						socket.getRemoteSocketAddress().toString().substring(1, socket.getRemoteSocketAddress().toString().indexOf(":")) +
						"\nSender's port number: "+ socket.getPort() + "\nMessage: " + msg + "\n->>");
			}
		}

		catch (EOFException eofe){
			System.out.print("\n** "+ this.ip +" may have left the network. **" + "\n->>");
		}

		catch (SocketException e) {
			System.out.println("\n->>");
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		catch (NullPointerException npe){
			System.out.println("\nNull Pointer Exception throws by the process.\n->>");
		}

		finally{

			try {
				socket.close();
				socket = null;
			}

			catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

}

class Node {

	private int id;
	private String ipAddress;
	private int port;

	public Node(int id, String ipAddress, int port) {
		super();
		this.id = id;
		this.ipAddress = ipAddress;
		this.port = port;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

}

class NodeComparator implements Comparator<Node> {
	@Override
	public int compare(Node n1, Node n2) {
		Integer id1 = n1.getId();
		Integer id2 = n2.getId();
		return id1.compareTo(id2);
	}
}


