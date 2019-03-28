import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver1a {
    public static void main(String[] args) throws IOException {

    //If we have exactly two arguments
    if (args.length == 2) {

        //Get the address
        InetAddress ip = InetAddress.getByName("localhost");

        //Port comes in as a string, need to parse it to an integer
        int port = Integer.parseInt(args[0]);
        String filenm = args[1];

        //Socket opened to connect to the specfic address and port
        DatagramSocket socket = new DatagramSocket(port, ip);

        System.out.println("Receiver socket running...");

        //Here we create the file object from the name of the file and open a connection to it
        File f = new File(filenm);
        FileOutputStream fstream = new FileOutputStream(f);
        int fsize = (int) f.length();

        //To increment the sequence number for every packet
        int sequence = 0;

        //To check if we have reached the end of the file
        int eof = 0;

        //We increment by 1024 because we read in 1KB every time
        while(eof != 1) {

            //Including the 16 bit sequence number and 8 bit eof as 3 header bytes plus 2 offset and octet bytes
            byte[] receive = new byte[1029];

            //Setup the packet to receive the data
            DatagramPacket packet = new DatagramPacket(receive, receive.length);
            socket.receive(packet);
            receive = packet.getData();

            //We left shift the byte by 8 to get the value of the second lowest byte and add the value
            //of the lowest byte to get the sequence number
            sequence = ((receive[2] & 0xFF)  << 8) + (receive[3] & 0xFF);

            //3rd bit is the end of file
            eof = receive[4];

            //To output the file
            byte[] out = new byte[1024];

            for (int i = 0; i<1024; i++) {
                out[i] = receive[i+5];
            }

            //Write the file out
            fstream.write(out);
            System.out.println("Received packet: " + sequence);

            //Increment the sequence number
            sequence = sequence + 1;

        }
        //Close the stream and socket
        fstream.close();
        socket.close();
        System.out.println("Received the file successfully");
        }
    else {
        System.out.println("Invalid arguments");
        }
      }
    }
