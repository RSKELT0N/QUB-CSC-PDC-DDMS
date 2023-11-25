package core.peer;

import core.Lib;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class Sender extends Runner
{
    Sender(core.peer.Node send_peer)
    {
        super();
        this.m_sender = send_peer;
        this.m_queue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run()
    {
        while(this.m_running)
        {
            Lib.Pair<Lib.Pair<String, Integer>, byte[]> current_item = null;
            try {
                current_item = m_queue.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if(current_item != null)
            {
                try
                {
                    String ip_address = current_item.first.first;
                    int port = current_item.first.second;
                    byte[] message = current_item.second;
                    this.m_sender.SendPacket(message, ip_address, port);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void AddSendItem(Lib.Pair<String, Integer> p, byte[] s) throws InterruptedException
    {
        m_queue.put(new Lib.Pair<>(p, s));
    }

    private final core.peer.Node m_sender;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, byte[]>> m_queue;
}
