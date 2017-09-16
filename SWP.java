/*===============================================================*
 *  File: SWP.java                                               *
 *                                                               *
 *  This class implements the sliding window protocol            *
 *  Used by VMach class					         *
 *  Uses the following classes: SWE, Packet, PFrame, PEvent,     *
 *                                                               *
 *  Author: Professor SUN Chengzheng                             *
 *          School of Computer Engineering                       *
 *          Nanyang Technological University                     *
 *          Singapore 639798                                     *
 *===============================================================*/

import java.util.Timer;
import java.util.TimerTask;

public class SWP {
    /*========================================================================*
     the following are provided, do not change them!!
     *========================================================================*/
    //the following are protocol constants.
    public static final int MAX_SEQ = 7;
    public static final int NR_BUFS = (MAX_SEQ + 1) / 2;

    // the following are protocol variables
    private int oldest_frame = 0;
    private PEvent event = new PEvent(); //Event that have 5 type
    private Packet out_buf[] = new Packet[NR_BUFS];

    //the following are used for simulation purpose only
    private SWE swe = null;
    private String sid = null;

    //Constructor
    public SWP(SWE sw, String s) {
        swe = sw;
        sid = s;
    }

    //the following methods are all protocol related
    private void init() {
        for (int i = 0; i < NR_BUFS; i++) {
            out_buf[i] = new Packet();
        }
    }

    private void wait_for_event(PEvent e) {
        swe.wait_for_event(e); //may be blocked
        oldest_frame = e.seq; //set timeout frame seq
    }

    private void enable_network_layer(int nr_of_bufs) {
        //network layer is permitted to send if credit is available
        swe.grant_credit(nr_of_bufs);
    }

    private void from_network_layer(Packet p) {
        swe.from_network_layer(p);
    }

    private void to_network_layer(Packet packet) {
        swe.to_network_layer(packet);
    }

    private void to_physical_layer(PFrame fm) {
        System.out.println("SWP: Sending frame: seq = " + fm.seq +
            " ack = " + fm.ack + " kind = " +
            PFrame.KIND[fm.kind] + " info = " + fm.info.data);
        System.out.flush();
        swe.to_physical_layer(fm);
    }

    private void from_physical_layer(PFrame fm) {
        PFrame fm1 = swe.from_physical_layer();
        fm.kind = fm1.kind;
        fm.seq = fm1.seq;
        fm.ack = fm1.ack;
        fm.info = fm1.info;
    }


    /*===========================================================================*
     	implement your Protocol Variables and Methods below:
     *==========================================================================*/

    private boolean no_nak = true; /*no nak has been sent yet*/
    private Timer[] sender_timer = new Timer[NR_BUFS];
    private Timer receiver_timer;

    /**
     * Increment to the next number within the possible sequence number
     * @param num   number to increment from
     */
    public int increment(int num) {
        return (num + 1) % (MAX_SEQ + 1);
    }


    /**
     * Checks the circular condition of the frames
     * Same as between in protocol 5, but shorter and more obscure
     * @param a   frame a
     * @param b   frame b
     * @param c   frame c
     */
    public static boolean between(int a, int b, int c) {
        return ((a <= b) && (b < c)) || ((c < a) && (a <= b)) || ((b < c) && (c < a));
    }


    /**
     * Construct and send a data, ack, or nak frame
     * @param fk             frame kind
     * @param frame_nr       sequence/frame number
     * @param frame_expected sequence/frame number expected
     * @param buffer         buffer Packet(s)
     */
    public void send_frame(int fk, int frame_nr, int frame_expected, Packet buffer[]) {
        PFrame s = new PFrame(); /* scratch variable*/
        s.kind = fk; /* kind == data, ack, or nak */

        if (fk == PFrame.DATA) s.info = buffer[frame_nr % NR_BUFS]; /* if frame kind is data*/
        s.seq = frame_nr; /* only meaningful for data frames*/
        s.ack = (frame_expected + MAX_SEQ) % (MAX_SEQ + 1);
        if (fk == PFrame.NAK) no_nak = false; /*one nak per frame, please */
        to_physical_layer(s); /*transmit the frame*/
        if (fk == PFrame.DATA) start_timer(frame_nr % NR_BUFS);
        stop_ack_timer(); /*no need for separate ack frame*/
    }


    /**
     * Sliding window protocol6 method
     */
    public void protocol6() {
        int ack_expected = 0; /* lower edge of sender’s window & next ack expected on the inbound stream */
        int next_frame_to_send = 0; /* upper edge of sender's window + 1 & number of next outgoing frame */
        int frame_expected = 0; /*lower edge of receiver's window*/
        int too_far = NR_BUFS; /*upper edge of receiver's window +1 */
        int nbuffered = 0; /*how many output buffers currently used & initially no packets are buffered*/

        PFrame r = new PFrame(); /*scratch variable*/
        Packet[] in_buf = new Packet[NR_BUFS]; /* buffers for the inbound stream */
        boolean[] arrived = new boolean[NR_BUFS]; /*inbound bit map*/

        for (int i = 0; i < NR_BUFS; i++) arrived[i] = false;

        init(); /*initialize outbound stream buffer*/
        enable_network_layer(NR_BUFS); /*sender can send up to 4 frames before the ack for the first frame come back*/

        while (true) {
            wait_for_event(event); /*five possibilites: see event_type above*/
            switch (event.type) {
                case (PEvent.NETWORK_LAYER_READY):
                    /*Network Layer Ready & accept, save and transmit a new frame*/
                    nbuffered += 1; /*expand the window*/
                    from_network_layer(out_buf[next_frame_to_send % NR_BUFS]); /*fetch new packets*/
                    send_frame(PFrame.DATA, next_frame_to_send, frame_expected, out_buf); /* transmit the frame */
                    increment(next_frame_to_send); /* advance upper window edge */
                    break;
                case (PEvent.FRAME_ARRIVAL):
                    /* a data or control frame has arrived */
                    from_physical_layer(r); /* fetch incoming frame from physical layer */
                    if (r.kind == PFrame.DATA) { /* An undamaged frame has arrived. */
                        if ((r.seq != frame_expected) && no_nak) //the frame's sequence number is not within the receiver's window
                            send_frame(PFrame.NAK, 0, frame_expected, out_buf);
                        else
                            start_ack_timer(); //time duration that the ack should wait to be piggybacked, else, ack should be sent as a separate frame
                        if (between(frame_expected, r.seq, too_far) && (arrived[r.seq % NR_BUFS] == false)) { /* Frames may be accepted in any order. */
                            arrived[r.seq % NR_BUFS] = true; /* mark buffer as full */
                            in_buf[r.seq % NR_BUFS] = r.info; /* insert data into buffer */
                            while (arrived[frame_expected % NR_BUFS]) { /*Pass frames and advance window. */
                                to_network_layer(in_buf[frame_expected % NR_BUFS]);
                                no_nak = true;
                                arrived[frame_expected % NR_BUFS] = false;
                                frame_expected = increment(frame_expected); /* advance lower edge of receiver’s window */
                                too_far = increment(too_far); /* advance upper edge of receiver’s window */
                                start_ack_timer(); /* to see if a separate ack is needed */
                            }
                        }
                    }
                    if ((r.kind == PFrame.NAK) && between(ack_expected, (r.ack + 1) % (MAX_SEQ + 1), next_frame_to_send))
                        send_frame(PFrame.DATA, (r.ack + 1) % (MAX_SEQ + 1), frame_expected, out_buf);
                    while (between(ack_expected, r.ack, next_frame_to_send)) {
                        enable_network_layer(1); //generate network_layer_ready event
                        stop_timer(ack_expected % NR_BUFS);
                        ack_expected = increment(ack_expected); /* advance lower edge of sender’s window */
                    }
                    break;
                case (PEvent.CKSUM_ERR):
                    if (no_nak)
                        send_frame(PFrame.NAK, 0, frame_expected, out_buf); /* damaged frame */
                    break;
                case (PEvent.TIMEOUT):
                    send_frame(PFrame.DATA, oldest_frame, frame_expected, out_buf); /* we timed out */
                    break;
                case (PEvent.ACK_TIMEOUT):
                    send_frame(PFrame.ACK, 0, frame_expected, out_buf); /* ack timer expired; send ack */
                    break;
                default:
                    System.out.println("SWP: undefined event type = " + event.type);
                    System.out.flush();
            }
        }
    }

    /* Note: when start_timer() and stop_timer() are called,
       the "seq" parameter must be the sequence number, rather
       than the index of the timer array,
       of the frame associated with this timer,
      */

    /**
     * Start a normal timer for frame
     * @param seq sequence no
     */
    private void start_timer(int seq) {
        stop_timer(seq); /* stop timer*/
        sender_timer[seq % NR_BUFS] = new Timer(); /*stop timer*/
        sender_timer[seq % NR_BUFS].schedule(new FrameTimerTask(seq), 200);
    }

    /**
     * Stop a normal timer for frame
     * @param seq sequence no
     */
    private void stop_timer(int seq) {
        if (sender_timer[seq % NR_BUFS] != null) {
            sender_timer[seq % NR_BUFS].cancel();
            sender_timer[seq % NR_BUFS] = null;
        }
    }

    /**
     * Start an ack timer for a frame
     */
    private void start_ack_timer() {
        if (receiver_timer == null) {
            receiver_timer = new Timer(); /*start timer*/
            receiver_timer.schedule(new AckTimerTask(), 50);
        }
    }

    /**
     * Stop an ack timer for a frame
     */
    private void stop_ack_timer() {
        if (receiver_timer != null) {
            receiver_timer.cancel(); /*stop timer*/
            receiver_timer = null;
        }
    }

    class FrameTimerTask extends TimerTask {
        private int seq;
        public FrameTimerTask(int seq) {
            this.seq = seq;
        }
        @Override
        public void run() {
            stop_timer(seq);
            swe.generate_timeout_event(seq);
        }
    }

    class AckTimerTask extends TimerTask {
        @Override
        public void run() {
            stop_ack_timer();
            swe.generate_acktimeout_event();
        }
    }

} //End of class

/* Note: In class SWE, the following two public methods are available:
   . generate_acktimeout_event() and
   . generate_timeout_event(seqnr).

   To call these two methods (for implementing timers),
   the "swe" object should be referred as follows:
     swe.generate_acktimeout_event(), or
     swe.generate_timeout_event(seqnr).
*/
