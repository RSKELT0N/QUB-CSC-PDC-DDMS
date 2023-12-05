package core.peer;

import core.Lib;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

class Sender extends Runner
{
    Sender(Peer peer, core.peer.Node send_peer)
    {
        super();
        m_peer = peer;
        this.m_sender = send_peer;
        this.m_queue = new LinkedBlockingQueue<>();
        this.m_send_count = BigInteger.valueOf(0);
    }

    @Override
    public void run()
    {
        while(this.m_running)
        {
            try
            {
                Toggle();
                Lib.Pair<Peer.RoutingTableEntry, byte[]> current_item = null;
                current_item = m_queue.poll(1000, TimeUnit.MILLISECONDS);

                if (current_item != null)
                {
                    String ip_address = current_item.first.ip_address;
                    int port = current_item.first.port;
                    byte[] message = current_item.second;
                    this.m_sender.SendPacket(message, ip_address, port);
                }
            }
            catch (IOException | InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public Lib.Pair<Semaphore, Receiver.Packet> CreateReceivedItem(byte[] payload)
    {
        String[] tokens = new String(payload).replace("\0", "").split(" ");
        BigInteger command_count = new BigInteger(tokens[0].split(":")[1]);

        m_peer.m_receiver.AddReceivedItem(command_count);
        return m_peer.m_receiver.GetReceivedPackets().get(command_count);
    }

    public BigInteger GetAndIncrementSendCount()
    {
        BigInteger ret;
        synchronized (this)
        {
            ret = this.m_send_count;
            this.m_send_count = this.m_send_count.add(BigInteger.valueOf(1));
        }
        return ret;
    }

    public void AddSendItem(Peer.RoutingTableEntry p, byte[] s) throws InterruptedException
    {
        m_queue.put(new Lib.Pair<>(p, s));
    }

    private Peer m_peer;
    private BigInteger m_send_count;
    private final core.peer.Node m_sender;
    private final LinkedBlockingQueue<Lib.Pair<Peer.RoutingTableEntry, byte[]>> m_queue;
}
