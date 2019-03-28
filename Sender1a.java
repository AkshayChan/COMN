import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender1a {
  public static void main(String[] args) throws IOException {

  //If we have exactly three arguments
  if (args.length == 3) {
      //Get the address for the socket connection from the Remote Host
      InetAddress ip = InetAddress.getByName(args[0]);

      //Port comes in as a string, need to parse it to an integer
      int port = Integer.parseInt(args[1]);
      String filenm = args[2];

      DatagramSocket socket = new DatagramSocket();

      //Here we create the file object from the name of the file and open a connection to it
      File f = new File(filenm);
      FileInputStream fstream = new FileInputStream(f);
      int fsize = (int) f.length();

      byte[] b = new byte[fsize];

      //Read in as many bytes from the filstream as the length of the file in the byte array
      fstream.read(b);

      //To increment the sequence number for every packet
      int sequence = 0;

      //We increment by 1024 because we read in 1KB every time
      for (int i = 0; i<fsize; i=i+1024) {

          //Including the 16 bit sequence number and 8 bit eof as 3 header bytes plus 2 more header bytes
          byte[] send = new byte[1029];
          //Set the first 16 bits to the sequence number, we get the first 8 bits using a right shift
          //And 2nd 8 bits by not shifting
          send[2] = (byte) (sequence >> 8);
          send[3] = (byte) (sequence >> 0);

          //If we reach the end of the file, we send 1 as the eof byte and send all the remaining
          //data in the file, else we send 0 and only send 1 KB
          if ((i + 1024) >= fsize) {
              System.out.println("Reached end of file");
              send[4] = (byte) 1;
              for (int j = 0; j<(fsize - i); j++) {
                send[j+5] = b[i+j];
              }
          }
          else {
              send[4] = (byte) 0;
              for (int j = 0; j<1024; j++) {
                send[j+5] = b[i+j];
              }
          }

          //Information about where to send packet handled by packet itself
          DatagramPacket packet = new DatagramPacket(send, send.length, ip, port);
          socket.send(packet);

          System.out.println("Sent out packet: " + sequence);

          //Increment the sequence number
          sequence = sequence + 1;

          //Adding a 10ms delay after every transmission
          try {
               Thread.sleep(10);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
      }
      //Close the stream and socket
      fstream.close();
      socket.close();
      System.out.println("Sent the file successfully");
      }
  else {
      System.out.println("Invalid arguments");
      }
    }
}
