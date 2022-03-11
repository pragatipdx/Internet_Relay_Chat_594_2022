import java.io.*;
import java.net.Socket;
import java.util.Properties;

public class IRC_Client {
    static Socket socket = null;
    static String clientName = clientDynamicGenerator();
    static InputStream ip_stream = null;//accepts input bytes and receives them
    static PrintStream logger = null;//adds functionality to output stream
    static OutputStream output_stream = null;//accepts output bytes and sends them
    static DataInputStream irc_Server_input = null;
    static BufferedReader irc_Client_input = null;


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

    public static String clientDynamicGenerator() {
        return  "Client_" + (int)(Math.random() * 20);
    }

    public static void main(String[] args) throws Exception{

     //   Properties prop = readPropertiesFile(System.getProperty("user.dir")+"\\resources\\connection.properties");
     //   int portNumber= Integer.valueOf(prop.getProperty("default_port"));
        int portNumber=8181;
        socket = new Socket("localhost", portNumber);

        output_stream = socket.getOutputStream();
        logger = new PrintStream(output_stream);
        logger.println("create " + clientName);
        logger.flush();
        logger.println();

        ip_stream = socket.getInputStream();
        irc_Server_input = new DataInputStream(ip_stream);
        irc_Client_input = new BufferedReader(new InputStreamReader(System.in));

        ClientRead input = new ClientRead();
        input.start();

        HearMessage msg_print = new HearMessage();
        msg_print.start();


    }
    //Reads the inputs from client and sends them to server
    static class ClientRead extends Thread{
        @Override
        public void run(){
            String message_to_server;
            while(true){
                try{
                    message_to_server = irc_Client_input.readLine();
                    logger.println(message_to_server);
                    if(message_to_server.equals("quit")) System.exit(0); //exit(0) is used to indicate successful termination
                    logger.flush();
                } catch(Exception e){
                    break;
                }
            }
        }
    }

    //Recieves the keyboard input from client
    static class HearMessage extends Thread{
        @Override
        public void run() {
            String message_from_server;
            while(true) {
                try{
                    message_from_server = irc_Server_input.readLine();
                    if(message_from_server.startsWith("quit")) System.exit(0);
                    System.out.println(message_from_server);
                } catch(Exception e) {
                    System.err.println("Server stopped working, exiting to handle server crash gracefully");
                    System.exit(0);
                }
            }
        }
    }
}
