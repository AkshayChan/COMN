import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class Receiver2b {
    public static void main(String[] args) throws IOException {

    //If we have exactly three arguments
    if (args.length == 3) {

        //Get the address
        InetAddress ip = InetAddress.getByName("localhost");

        //Port comes in as a string, need to parse it to an integer
        int port = Integer.parseInt(args[0]);
        String filenm = args[1];

        //The window size
        int wsize = Integer.parseInt(args[2]);

        //Socket opened to connect to the specfic address and port
        DatagramSocket socket = new DatagramSocket(port, ip);

        System.out.println("Receiver socket running...");

        //Here we create the file object from the name of the file and open a connection to it
        File f = new File(filenm);
        FileOutputStream fstream = new FileOutputStream(f);
        int fsize = (int) f.length();

        //To store the incoming packets to write to a file.
        HashMap<Integer, byte[]> packets = new HashMap<Integer, byte[]>();

        //To increment the sequence number for every packet
        int sequence = 0;

        //To check if the sequence numbers are in order
        int base = -1;

        //To check if we have reached the end of the file
        int eof = 0;

        //To check if we recieve all the packets
        int check = 0;

        //While we have got all the packets
        while(check != 1) {

            //Including the 16 bit sequence number and 8 bit eof as 3 header bytes and 2 bytes offset and octet
            byte[] receive = new byte[1029];

            //Setup the packet to receive the data
            DatagramPacket packet = new DatagramPacket(receive, receive.length);
            socket.receive(packet);
            receive = packet.getData();

            //System.out.println("Got packet:" + sequence);
            //System.out.println("base:" + base);
            //We left shift the byte by 8 to get the value of the second lowest byte and add the value
            //of the lowest byte to get the sequence number
            sequence = ((receive[2] & 0xFF)  << 8) + (receive[3] & 0xFF);

            //3rd bit is the end of file
            eof = receive[4];

            byte[] out = new byte[1024];

            //If the packet received is in the window
            if (sequence>=base && sequence<=(base+wsize)) {

                //If we are going in order
                if (sequence == (base + 1)) {
                    //Remove padding from end of file packet
                    if (eof == 1) {
                        check = 1;
                        for (int i = 0; i<1024; i++) {
                            out[i] = receive[i+5];
                            //System.out.print(out[i]);
                            //System.out.print(" ");
                        }
                        //System.out.println();
                        //System.out.println();

                        int j = out.length - 1;
                        while (out[j] == 0)
                        {
                            --j;
                        }

                        byte[] outc = new byte[j+1];

                        for (int k = 0; k<j+1; k++) {
                            outc[k] = out[k];
                            //System.out.print(outc[k]);
                            //System.out.print(" ");
                        }
                        //Store the packets in a buffer
                        packets.put(sequence, outc);
                    }
                    else  {
                        //Create the file
                        for (int i = 0; i<1024; i++) {
                            out[i] = receive[i+5];
                        }
                        //Store the packets in a buffer
                        packets.put(sequence, out);
                    }

                    //System.out.println("Received packet: " + sequence);

                    //Create the ackp and send it back to the address and port we got the packet from
                    byte[] ack = new byte[2];
                    ack[0] = (byte) (sequence >> 8);
                    ack[1] = (byte) (sequence >> 0);

                    //Information about where to send packet handled by packet itself
                    DatagramPacket ackp = new DatagramPacket(ack, ack.length, packet.getAddress(), packet.getPort());
                    socket.send(ackp);

                    //Update the base
                    base = sequence;
                }
                //Simply buffer the packet if we are not going in order
                else {

                  //Create the packet
                  for (int i = 0; i<1024; i++) {
                      out[i] = receive[i+5];
                  }

                  //Store the packets in a buffer
                  packets.put(sequence, out);

                  byte[] ack = new byte[2];
                  ack[0] = (byte) (sequence >> 8);
                  ack[1] = (byte) (sequence >> 0);

                  //Information about where to send packet handled by packet itself
                  DatagramPacket ackp = new DatagramPacket(ack, ack.length, packet.getAddress(), packet.getPort());
                  socket.send(ackp);
                }
            //Increment the sequence number
            //sequence = sequence + 1;
            }

            //According to the algorithm, we do this
            if(sequence>=(base-wsize) && sequence<=(base-1)) {
                byte[] ack = new byte[2];
                ack[0] = (byte) (sequence >> 8);
                ack[1] = (byte) (sequence >> 0);

                //Information about where to send packet handled by packet itself
                DatagramPacket ackp = new DatagramPacket(ack, ack.length, packet.getAddress(), packet.getPort());
                socket.send(ackp);
            }
        }

        //Write out all packets in the buffer
        int writeSeq = 0;
        //While we still have packets to write
        while(packets.get(writeSeq) != null) {
            fstream.write(packets.get(writeSeq));
            packets.remove(writeSeq);
            writeSeq++;
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
