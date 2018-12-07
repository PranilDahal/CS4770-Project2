import java.io.*; 
import java.util.*; 
import java.net.*; 

public class ChatApp extends Thread{ 

	ServerSocket listener;

	ArrayList<ClientThread> active;

	int port;

	String ip;

	public ChatApp (int port) throws IOException {
		listener = new ServerSocket();
		listener.bind(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port));
		this.port = listener.getLocalPort();
		this.ip = InetAddress.getLocalHost().getHostAddress();
		System.out.println("Waiting for connections on "+ this.ip + " port " + this.port + "...\nEnter \"help\" to see available connections.");
		this.active = new ArrayList<ClientThread>();
	}

	// This is where the program starts from
	public static void main(String[] args){

		String startPort = "";
		if(args.length != 1){
			System.out.println(" ** Please provide the port number to listen on.");
			System.exit(0);
		}

		// Grab the port from the commandline args and parse it to int
		startPort = args[0];
		int port = 0;
		try {
			port = Integer.parseInt(startPort);
		}
		catch(NumberFormatException e) {
			System.out.println(" ** Please enter a valid port nuber.");
			System.exit(0);
		}

		try {
			ChatApp chat = new ChatApp(port);

			Thread t1 = new Thread(chat);
			chat.start();

			while(true) {

				System.out.print("->>");
				Scanner input = new Scanner(System.in);
				String userCommand = input.nextLine();
				String[] commands = userCommand.split(" ");

				switch(commands[0]){

				case "connect":
					boolean duplicate = false;
					chat.removeTerminatedConnections();

					if(commands.length == 3) {

						for(int x = 0; x < chat.getActive().size(); x++){
							if(commands[1].equalsIgnoreCase(chat.getActive().get(x).getIp()) && 
									commands[2].equalsIgnoreCase(Integer.toString(chat.getActive().get(x).getPort()))){
								System.out.println("** You are already connected to that peer **");
								duplicate = true;
								break;
							}
						}
						if(!duplicate) {
							ClientThread newClient = new ClientThread(true, commands[1], Integer.parseInt(commands[2]), null);
							if(!chat.getActive().contains(newClient)){
								chat.getActive().add(newClient);
								Thread tc = new Thread(newClient);
								tc.start();
							}
						}
					}
					else {
						System.out.println("** You entered invalid form of command. Please look at the \"help\" dialouges for list of available commands.\n->");
					}
					break;

				case "send":
					chat.removeTerminatedConnections();;

					try{
						int peer = Integer.parseInt(commands[1]);
						if(peer <= chat.getActive().size() && peer >0){
							ClientThread connect = chat.getActive().get(Integer.parseInt(commands[1]) - 1);
							if(connect.getSocket() != null){
								String message = "";
								for(int x = 2; x < commands.length; x++){
									message += commands[x] + " ";
								}
								try{
									connect.sendMessage(message);
								}catch(SocketException e){
									System.out.println("**Failed to send message. Either the client isnt listening or you entered the wrong connection number. "
											+ "\n Look at \"list\" command output for available connections. **");
								}
							}
						}
						else {
							System.out.println("** You might not have entered the correct connection number. **"
									+ "\nFailed to send message. Either the client isnt listening or you entered the wrong connection number. "
									+ "\n Look at \"list\" command output for available connections.");
						}
					}
					catch(NumberFormatException e){
						System.out.println("** You might not have entered the correct connection number. **"
								+ "\nFailed to send message. Either the client isnt listening or you entered the wrong connection number. "
								+ "\n Look at \"list\" command output for available connections.");
					}
					break;

				case "terminate":
					chat.removeTerminatedConnections();

					if(commands.length >2 || commands.length <2) {
						System.out.println("** Incorrect use of terminate command. Use \"help\" command to see available options.  **");
						break;
					}

					int peer;

					try {
						peer = Integer.parseInt(commands[1]);
					}catch(NumberFormatException e) {
						System.out.println("** Incorrect use of terminate command. Use \"help\" command to see available options.  **");
						break;
					}

					if(peer <= chat.getActive().size() && peer >0) {
						ClientThread connect = chat.getActive().get(peer -1);
						if(connect.terminate()){
							connect.interrupt();
							System.out.println("** Connection to " + connect.getIp()+" has been terminated. **");
							chat.getActive().remove(connect);
						}else{
							System.out.println("** Error trying to close connection **");
						}
					}
					else {
						System.out.println("** You might not have entered the correct connection number. **"
								+ "\nFailed to send message. Either the client isnt listening or you entered the wrong connection number. "
								+ "\n Look at \"list\" command output for available connections.");
					}
					break;

				case "list":
					if(commands.length !=1){
						System.out.println("** You entered invalid form of command. Please look at the \"help\" dialouges for list of available commands. **");
						break;
					}
					chat.removeTerminatedConnections();;
					try {
						List<ClientThread> availableConnections = chat.getActive();
						System.out.println("** Available connections in this chat:");
						int i=0;
						for (ClientThread client : availableConnections) {
							Socket temp = client.getSocket();
							if(temp != null &&!temp.isClosed())
								System.out.println(++i + "\t"+ client.getIp() + "\t\t" + client.getPort());
						}
					}
					catch (ConcurrentModificationException e) {
						System.out.println("** Closed 1 connection. **");
					}
					break;

				case "help":
					helpMethodDisplay();
					break;


				case "myport":
					System.out.println("Your server is listening on port: "+ chat.getPort());
					break;


				case "myip":
					System.out.println("Your computer's IP address is: "+ chat.getIp());
					break;

				case "exit":
					if(commands.length !=1){
						System.out.println("** You entered invalid form of command. Please look at the \"help\" dialouges for list of available commands. **");
						break;
					}
					chat.removeTerminatedConnections();

					for (ClientThread client : chat.getActive()) {
						if(client.terminate()){
							client.interrupt();
							System.out.println("** Connection to " + client.getIp()+" has been terminated. **");
						}else{
							System.out.println("** Error trying to close connection **");
						}
					}
					System.out.println("** You will now exit. **");
					System.exit(0);
					break;

				default:
					System.out.println("Invalid Command. Use \"help\" command to see available options.");
					break;

				}
			}

		}catch(IOException e) {
			System.out.println("** IOException is thrown. Something went wrong.**\n->");
		}
	}

	private static void helpMethodDisplay() {
		System.out.println("help --->\t\t\t Use this command to display the help screen\n");
		System.out.println("myip --->\t\t\t Use this command to display your IP address\n");
		System.out.println("myport --->\t\t\t Command to display your server port.\n");
		System.out.println("connect <ip> <port> ---> \t Connect to the given <ip> on given <port>\n");
		System.out.println("list --->\t\t\t List all connections with id# that can be used to interact with the connection.\n");
		System.out.println("terminate <id>  --->\t\t Terminate the connection based on the <id> given. Use \"list\" command to view available connections.\n ");
		System.out.println("send <id> <message>  --->\t Send message to connection of id <id>. Use \"list\" command to view available connections.\n ");
		System.out.println("exit --->\t\t\t Close all connections and exit.\n");

	}

	void removeTerminatedConnections() {

		// Changing arraylist to an array to avoid ConcurrentModificationException in multithreading

		ClientThread[] c = active.toArray(new ClientThread[active.size()]);

		ArrayList<ClientThread> newlist = new ArrayList<ClientThread>();
		for(int m=0; m<c.length; m++){
			if(c[m].getSocket() != null){
					newlist.add(c[m]);
				}
		}

		active = newlist;

	}

	public void run() {
		Socket socket = null;
		ClientThread client = null;
		// Keep listening to new connections on a background thread
		while(true) {

			try {
				socket = listener.accept();
				client = new ClientThread(false, "", 0,socket);

				active.add(client);
				removeTerminatedConnections();

				System.out.print("** You are now connected to " + 
						socket.getRemoteSocketAddress().toString().substring(1, socket.getRemoteSocketAddress().toString().indexOf(":")) +
						" on port: " + socket.getPort()+". **\n->>");

				DataInputStream incoming = new DataInputStream(socket.getInputStream());
				String msg;

				while( (msg = incoming.readUTF()) != null) {
					System.out.print("\nReceived message from IP: "+ 
							socket.getRemoteSocketAddress().toString().substring(1, socket.getRemoteSocketAddress().toString().indexOf(":")) +
							"\nSender's port number: "+ socket.getPort() + "\nMessage: " + msg + "\n->>");
				}

				client.start();
			}

			catch (IOException e) {
				System.out.println("** IP: "+ 
						socket.getRemoteSocketAddress().toString().substring(1, socket.getRemoteSocketAddress().toString().indexOf(":")) +
						" has disconnected. **\n->>");
			}finally {
				active.remove(client);
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

	public ArrayList<ClientThread> getActive() {
		return active;
	}

	public void setActive(ArrayList<ClientThread> active) {
		this.active = active;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

}

class ClientThread extends Thread{

	private String ip;
	private int port;
	private Socket socket;

	public ClientThread(boolean initiatedByMe, String ip, int port, Socket client ) throws UnknownHostException, IOException{
		if (initiatedByMe) {
			this.ip = ip;
			this.socket = new Socket(ip, port);
			System.out.println("** You are now connected to " + ip + " on port " + port+". **");
			this.port = socket.getPort();
		}
		else {
			this.socket = client;
			this.ip = client.getInetAddress().getHostAddress().toString();
			this.port = client.getPort();
		}
	}

	public void sendMessage(String message) throws IOException, NullPointerException, SocketException{
		DataOutputStream outgoing = new DataOutputStream(socket.getOutputStream());
		outgoing.writeUTF(message);
		outgoing.flush();
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
			System.out.print("\n** "+ this.ip +" may closed connection with you. **" + "\n->>");
		}

		catch (SocketException e) {
			System.out.println("\n->>");
		}

		catch (IOException e) {
			e.printStackTrace();
		}

		catch (NullPointerException npe){
			System.out.println("\nNull Pointer Exception throws by the process.\n->");
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


