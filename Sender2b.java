import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;

public class Sender2b {
    public static void main(String[] args) throws IOException {
        //If we have exactly three arguments
        if (args.length == 5) {
            //Get the address for the socket connection from the Remote Host
            InetAddress ip = InetAddress.getByName(args[0]);

            //Port comes in as a string, need to parse it to an integer
            int port = Integer.parseInt(args[1]);
            String filenm = args[2];

            //Timer for the retransmission - changed names because we need the timer variable for something else now
            int timeout = Integer.parseInt(args[3]);

            //Used to schedule the SendPacketTasks (TimerTasks) for each packet
            Timer timer = new Timer();

            //The window size
            int wsize = Integer.parseInt(args[4]);

            DatagramSocket socket = new DatagramSocket();

            //To store each of the packets in sequence and their individual timers
            HashMap<Integer, TimerTask> packets = new HashMap<Integer, TimerTask>();

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

            //To track the number of acked packets
            int base = 0;

            //To check if we have reached the last packet
            int finalSequence = (int) fsize/1024;

            //System.out.println("finalSequence :" + finalSequence);
            //For the eofack
            int eofack = 0;

            //If we haven't reached the end of the file yet
            //Only exit once we receive the ack for the last file too
            while (base != finalSequence) {

                //If we are in the window size and have not iterated all the packets
                while(sequence<=finalSequence && (sequence-base)<=wsize) {
                  //System.out.println("Sent packet:" + sequence);


                    //Including the 16 bit sequence number and 8 bit eof as 3 header bytes and 2 bytes for offset and octet
                    byte[] send = new byte[1029];
                    //Set the first 16 bits to the sequence number, we get the first 8 bits using a right shift
                    //And 2nd 8 bits by not shifting
                    send[2] = (byte) (sequence >> 8);
                    send[3] = (byte) (sequence >> 0);

                    //End of file packet
                    if(sequence == finalSequence) {
                        send[4] = (byte) 1;
                        int currentpack = sequence*1024;
                        //Only send upto the last character in the file
                        for (int i = 0; i<(fsize-currentpack); i++) {
                            send[i+5] = b[currentpack + i];
                        }
                    }
                    //Not the end of the file
                    else {
                        send[4] = (byte) 0;
                        int currentpack = sequence*1024;
                        //Send 1024 bytes - packet size
                        for (int i = 0; i<1024; i++) {
                            send[i+5] = b[currentpack + i];
                        }
                    }

                    //Information about where to send packet handled by packet itself
                    DatagramPacket packet = new DatagramPacket(send, send.length, ip, port);

                    //Schedule the packets at regular intervals
                    SendPacketTask packtask = new SendPacketTask(socket, packet);
                    timer.scheduleAtFixedRate(packtask, 0, timeout);

                    //Add them to our list of packets
                    packets.put(sequence, packtask);

                    //Increment the sequence number
                    sequence = sequence + 1;
                }

                //To track if we are recieving the correct ACK, else retry till timeout
                //boolean rec = false;

                byte[] ack = new byte[2];
                DatagramPacket ackp = new DatagramPacket(ack, ack.length);
                socket.receive(ackp);

                //Similar to how we decode the sequence number previously
                int ackSequence = ((ack[0] & 0xFF)  << 8) + (ack[1] & 0xFF);

                //System.out.println("Got ack:" + ackSequence);

                //If the packet hasn't been acked yet and it is in the window, ack it
                if (packets.get(ackSequence) != null && ackSequence >= base && ackSequence<=(base+wsize)) {

                    //System.out.println("Removed packet:" + ackSequence);
                    packets.get(ackSequence).cancel();
                    packets.remove(ackSequence);

                    //If n is the smallest unAcked packet, advance base to the next smallest unAcked packet
                    if (base == ackSequence) {
                        for (int i = ackSequence; i<=finalSequence; i++) {
                            //If that packet hasn't been Acked yet, move the base
                            //System.out.println(packets.get(i));
                            //System.out.println(i);
                            if (packets.get(i) != null) {
                                //System.out.println("LALALA " + i);
                                base = i;
                                //System.out.println("Updated base to:" + base);
                                break;
                            }
                        }
                    }
                }
                //If we are in the final packet
                if (base == finalSequence-1) {
                    eofack = eofack + 1;
                }

                //To many cycles on the last ack, stop sender
                if (eofack == 100) {
                    base = finalSequence;
                    //packets.get(finalSequence).cancel();
                    //packets.remove(finalSequence);
                }
            }

            //Make sure no packets left with timers
            packets.get(finalSequence).cancel();
            packets.remove(finalSequence);
            timer.cancel();

            //After we have completed transmitting
            long stopTime = System.currentTimeMillis();

            //Close the stream and socket
            fstream.close();
            socket.close();

            //File size in bytes divide by total time taken  in seconds to transfer the file
            //Need to divide the fsize by 1024 and time by 1000 (as in ms) so we divide by 1.024
            double throughput = (double)(fsize/1.024)/(double)((stopTime-startTime));

            System.out.println(throughput);
            //System.out.println("Sent the file successfully");
        }
        else {
            System.out.println("Invalid arguments");
        }
    }
}
    //Perform a task after the delay
class SendPacketTask extends TimerTask {

    DatagramSocket sendsocket;
    DatagramPacket sendpacket;

    public SendPacketTask(DatagramSocket socket, DatagramPacket packet) {
        sendsocket = socket;
        sendpacket = packet;
    }
    public void run() {
      try {
        sendsocket.send(sendpacket);
      } catch(IOException e) {
          e.printStackTrace();
      }

    }
}
