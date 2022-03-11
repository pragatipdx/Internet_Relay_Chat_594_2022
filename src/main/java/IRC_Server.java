import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class IRC_Server {

    public static HashMap<String, Set<String>> clientsInRoom = new HashMap<>();
    public static HashMap<String, Set<String>> clientRooms = new HashMap<>();
    public static HashMap<String, clientThread> clients = new HashMap<>();

    public static Properties readPropertiesFile(String fileName) throws IOException {
        FileInputStream fis = null;
        Properties prop = null;
        try {
            fis = new FileInputStream(fileName);
            prop = new Properties();
            prop.load(fis);
        } catch(FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            fis.close();
        }
        return prop;
    }


    public static void main(String[] args) throws Exception {

    //     Properties prop = readPropertiesFile(System.getProperty("user.dir")+"\\resources\\connection.properties");
    //    int port= Integer.valueOf(prop.getProperty("default_port"));
        int port=8181;
        ServerSocket server_sock = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        System.out.println("Server running....");
        serverShutdown reader = new serverShutdown();
        reader.start();
        String message_from_client, message_to_client = "";
        while(true) {
            try{
                Socket socket = server_sock.accept(); //Establishing connection between Client and server.
                DataInputStream input_stream = new DataInputStream(socket.getInputStream());
                PrintStream output_stream = new PrintStream(socket.getOutputStream());
                message_from_client = input_stream.readLine();
                if(message_from_client!= null){
                    if(message_from_client.contains( "create")) {
                        String name = message_from_client.split(" ")[1];
                        clients.put(name, new clientThread(name, socket));
                        clients.get(name).start();
                        message_to_client = "Welcome " + name;
                        output_stream.println(message_to_client);
                    }
                }
                output_stream.flush();
            } catch(Exception e){
                break;
            }

        }
        server_sock.close();
    }

    public static class serverShutdown extends Thread{
        @Override
        public void run(){
            String in_operator;
            BufferedReader read_key = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                try{
                    in_operator = read_key.readLine();
                    if(in_operator.startsWith("quit")) {
                        for(String key : clients.keySet()){
                            clients.get(key).logger.println("Server Unavailable");
                            clients.get(key).logger.println("quit");
                            clients.get(key).logger.close();
                            try {
                                clients.get(key).ip_stream.close();
                                clients.get(key).client_socket.close();
                            }
                            catch (Exception ex) {

                            }
                        }
                        System.exit(0);
                    }
                } catch(Exception e){

                }
            }
        }


    }


     public static class clientThread extends Thread{
        PrintStream logger = null;
        String client_name;
        DataInputStream ip_stream = null;
        Socket client_socket = null;


        public clientThread(String name, Socket clientSocket) {
            this.client_socket = clientSocket;
            this.client_name = name;
        }

        @Override
        public void run() {
            try {
                ip_stream = new DataInputStream(client_socket.getInputStream());
                logger = new PrintStream(client_socket.getOutputStream());
                System.out.println("Connection established for: "+client_name);
                String name = ip_stream.readLine().trim();
                synchronized(this){
                    while (true) {
                        String input = ip_stream.readLine();
                        String[] lineArray = input.split(" ");
                        if(lineArray.length > 0){
                            String action = lineArray[0].toLowerCase();
                            switch (action) {
                                case "createroom":     // input: createroom <room/>
                                    createRoom(input);
                                    break;
                                case "joinroom":     // input: joinroom <room/>
                                    joinroom(input);
                                    break;
                                case "listrooms":     //input: listrooms
                                    listrooms();
                                    break;
                                case "listmembers":    // input: listmembers <room/>
                                    listmembers(input);
                                    break;
                                case "messageroom":    // input: messageroom <room/> <message/>
                                    messageroom(input);
                                    break;
                                case "leaveroom":       // input: leaveroom <room/>
                                    leaveroom(input);
                                    break;
                                case "private":       // input: private <client/> <message/>
                                    privatemessage(input);
                                    break;
                                default:
                                    error_message();
                                    if (input.startsWith("quit")) {
                                        client_thread_close_down();
                                        Thread.currentThread().stop();
                                    }
                                    break;

                            }
                        }
                    }
                }
            } catch (Exception e) {
                client_thread_close_down();
            }
        }


        public void client_thread_close_down() {
            try {
                logger.println("Quitting " + client_name);
                System.out.println(client_name + " disconnected!");
                clientRooms.remove(client_name);
                for(String room: clientsInRoom.keySet()) {
                    if(clientsInRoom.get(room).contains(client_name)) {
                        clientsInRoom.get(room).remove(client_name);
                        for(String key: clientsInRoom.get(room))
                            clients.get(key).logger.println(client_name + " leaves " + room);
                    }
                }
                clients.remove(client_name);
                logger.close();
                ip_stream.close();
                client_socket.close();
            } catch (Exception ex) {

            }
        }

        public void error_message() {
            logger.println("Incorrect input");
        }
      

        public void createRoom(String reader) {
            if (reader.contains(" ")) {
                String room = reader.split(" ")[1];

                if (!clientsInRoom.containsKey(room)) {
                    clientsInRoom.put(room, new HashSet<String>());
                    logger.println("Room " + room + " is created");
                    System.out.println("Room " + room + " created by " + client_name);
                } else {
                    logger.println("Room " + room + " already exits, please try new name");
                }

            }
            else{
                error_message();
            }
        }


      
        public void joinroom(String reader){
            if(reader.contains(" ")) {
                String room = reader.split(" ")[1];

                if (!clientsInRoom.containsKey(room)) {
                    logger.println(room + "does not exists");
                    return;
                } else if (!clientRooms.containsKey(client_name)) {
                    clientRooms.put(client_name, new HashSet<String>());
                } else if (clientsInRoom.get(room).contains(client_name) || clientRooms.get(client_name).contains(room)) {
                    logger.println("Room already exists: " + room);
                    return;
                }
                clientRooms.get(client_name).add(room);
                clientsInRoom.get(room).add(client_name);
                logger.println("Joined to Room: " + room);
                for (String key : clientsInRoom.get(room)) {
                    if (!key.equals(this.client_name)) {
                        clients.get(key).logger.println(client_name + " joined the room " + room);
                    }
                }
                System.out.println(client_name + " joined room " + room);
            }
            else{
                error_message();
            }
        }

       
        public void listrooms() {
            for(String reader: clientsInRoom.keySet()) logger.println(reader);
        }

       
        public void listmembers(String reader) {
            if(reader.contains(" ")) {

                String room = reader.split(" ")[1];
                if (!clientsInRoom.containsKey(room)) {
                    logger.println("Room " + room + " doesn't exist!");
                    return;
                }


                for (String client : clientsInRoom.get(room)) logger.println(client);
            }
            else{
                error_message();
            }
        }

       
        public void messageroom(String reader){

            if(reader.contains(" ")) {
                String room = reader.split(" ")[1];
                String message = this.client_name + ":" + reader.substring(reader.indexOf(room));
                if (!clientsInRoom.get(room).contains(this.client_name)) {
                    logger.println("You are not member of this room " + room);
                    return;
                }
                clientsInRoom.get(room).forEach((key) -> {
                    if (key.equals(this.client_name)) {
                        clients.get(key).logger.println("Message Sent");
                    }
                    clients.get(key).logger.println(message);
                });
                System.out.println(this.client_name + " sent a message");
            }
            else{
                error_message();
            }
        }

        
        public void privatemessage(String reader){
            if(reader.contains(" ")) {
                String user = reader.split(" ")[1];
                String message = this.client_name + ":" + reader.substring(reader.indexOf(user) + user.length());

                if (clients.containsKey(user)) clients.get(user).logger.println(message);
                else {
                    logger.println("Client " + user + " doesn't exist ");
                    return;
                }
                clients.get(client_name).logger.println("Message Sent");
                System.out.println(client_name + " sents private message to " + user);
            }
            else{
                error_message();
            }
        }

        
        public void leaveroom(String reader) {
            if(reader.contains(" ")) {
                String room = reader.split(" ")[1];
                if (!clientsInRoom.containsKey(room)) {
                    logger.println("Room " + room + " doesn't exist..");
                    return;
                }

                if (clientRooms.containsKey(client_name) && clientRooms.get(client_name).contains(room)) {
                    clientRooms.get(client_name).remove(room);
                    clientsInRoom.get(room).remove(client_name);
                    logger.println("You left room " + room);
                    for (String key : clientsInRoom.get(room)) {
                        clients.get(key).logger.println(client_name + " left the room " + room);
                    }
                    System.out.println(client_name + " left the room " + room);
                    return;
                }
                logger.println("You are not member of room " + room);
            }
            else{
                error_message();
            }
        }


    }
}
