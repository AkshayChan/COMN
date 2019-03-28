import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;

public class Sender1b {
  public static void main(String[] args) throws IOException {

  //If we have exactly three arguments
  if (args.length == 4) {
      //Get the address for the socket connection from the Remote Host
      InetAddress ip = InetAddress.getByName(args[0]);

      //Port comes in as a string, need to parse it to an integer
      int port = Integer.parseInt(args[1]);
      String filenm = args[2];

      //Timer for the retransmission
      int timer = Integer.parseInt(args[3]);

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

      //Number of retransmissions
      int retran = 0;

      //To calculate the throughput
      long startTime = System.currentTimeMillis();

      //We increment by 1024 because we read in 1KB every time
      for (int i = 0; i<fsize; i=i+1024) {

          //Including the 16 bit sequence number and 8 bit eof as 3 header bytes and 2 bytes for offset and octet
          byte[] send = new byte[1029];
          //Set the first 16 bits to the sequence number, we get the first 8 bits using a right shift
          //And 2nd 8 bits by not shifting
          send[2] = (byte) (sequence >> 8);
          send[3] = (byte) (sequence >> 0);

          //If we reach the end of the file, we send 1 as the eof byte and send all the remaining
          //data in the file, else we send 0 and only send 1 KB
          if ((i + 1024) >= fsize) {
              //System.out.println("Reached end of file");
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

          //System.out.println("Sent out packet: " + sequence);

          //To check if we receive the correct non corrupt ACK
          int ackCorrect = 0;

          //Check if aren't recieving the last ack
          int eofack = 0;

          while(ackCorrect != 1) {
            byte[] ack = new byte[2];
            DatagramPacket ackp = new DatagramPacket(ack, ack.length);
            try {
                socket.setSoTimeout(timer);
                socket.receive(ackp);

                //Similar to how we decode the sequence number previously
                int ackSequence = ((ack[0] & 0xFF)  << 8) + (ack[1] & 0xFF);
                //If we get the correct sequence number back
                if (ackSequence == sequence) {
                    ackCorrect = 1;
                }
                else {
                    //Retransmit the packet, wrong ack
                    //System.out.println("retransmitted packet: " + sequence);
                    socket.send(packet);
                    retran = retran + 1;
                    if (send[4] == 1) {
                        eofack = eofack + 1;
                    }
                }
            }
            catch (SocketTimeoutException exception) {
                //System.out.println("timeout retransmitted packet: " + sequence);
                //Timed out, resend the packet
                socket.send(packet);
                retran = retran + 1;
                if (send[4] == 1) {
                    eofack = eofack + 1;
                }
            }

            //Break out if we don't get ack after 10 trial
            if (eofack == 10) {
                //System.out.println("Handled");
                ackCorrect = 1;
            }
          }

          //Increment the sequence number
          sequence = sequence + 1;

      }

      //After we have completed transmitting
      long stopTime = System.currentTimeMillis();

      //Close the stream and socket
      fstream.close();
      socket.close();


      //File size in bytes divide by total time taken  in seconds to transfer the file
      double throughput = (double)(fsize/1024)/(double)((stopTime-startTime)/1000);

      System.out.println(retran + " " + throughput);

      //System.out.println("Sent the file successfully");
      }
  else {
      System.out.println("Invalid arguments");
      }
    }
}
