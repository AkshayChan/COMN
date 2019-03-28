import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;

public class Sender2a {
    public static void main(String[] args) throws IOException {
        //If we have exactly three arguments
        if (args.length == 5) {
            //Get the address for the socket connection from the Remote Host
            InetAddress ip = InetAddress.getByName(args[0]);

            //Port comes in as a string, need to parse it to an integer
            int port = Integer.parseInt(args[1]);
            String filenm = args[2];

            //Timer for the retransmission
            int timer = Integer.parseInt(args[3]);

            //The window size
            int wsize = Integer.parseInt(args[4]);

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

            //To track the number of acked packets
            int base = -1;

            //To check if we have reached the last packet
            int finalSequence = (int) fsize/1024;

            //For the eofack
            int eofack = 0;

            //If we haven't reached the end of the file yet
            //Only exit once we receive the ack for the last file too
            while (base != finalSequence) {

                //If we are in the window size and have not iterated all the packets
                while(sequence<=finalSequence && sequence-base<=wsize) {

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
                    socket.send(packet);

                    //Increment the sequence number
                    sequence = sequence + 1;
                }

                //To track if we are recieving the correct ACK, else retry till timeout
                boolean rec = false;

                try {
                    while(rec == false) {
                        byte[] ack = new byte[2];
                        DatagramPacket ackp = new DatagramPacket(ack, ack.length);
                        socket.setSoTimeout(timer);
                        socket.receive(ackp);

                        //Similar to how we decode the sequence number previously
                        int ackSequence = ((ack[0] & 0xFF)  << 8) + (ack[1] & 0xFF);

                        //System.out.println("Received ack:" + ackSequence);
                        //If we get the correct sequence number back, change base to the currently acknowledged
                        if (base < ackSequence) {
                            rec = true;
                            base = ackSequence;
                        }
                    }
                }
                catch (SocketTimeoutException exception) {

                    //retransmit all the packets after base in the next
                    //outer loop iteration i.e. packets that have not been acked
                    sequence = base + 1;

                    //Increase the number of retransitions
                    retran = retran + 1;

                    //If we are in the final package
                    if (base == finalSequence-1) {
                        eofack = eofack + 1;
                    }
                }
                //To many cycles on the last ack, stop sender
                if (eofack == 100) {
                    base = finalSequence;
                }
            }

            //After we have completed transmitting
            long stopTime = System.currentTimeMillis();

            //Close the stream and socket
            fstream.close();
            socket.close();

            //File size in bytes divide by total time taken  in seconds to transfer the file
            //Need to divide the fsize by 1024 and time by 1000 (as in ms) so we divide by 1.024
            double throughput = (double)(fsize/1.024)/((stopTime-startTime));

            System.out.println(throughput);
            //System.out.println("Sent the file successfully");
        }
        else {
            System.out.println("Invalid arguments");
        }
    }
}
