package core.peer;

import core.Lib;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class Sender implements Runnable
{
    Sender(core.peer.Node send_peer)
    {
        SetState(true);
        this.m_sender = send_peer;
        this.m_queue = new LinkedBlockingQueue<>();
    }

    public void SetState(boolean state)
    {
        this.m_running = state;
    }

    public void AddSendItem(Lib.Pair<String, Integer> p, String s) throws InterruptedException
    {
        m_queue.put(new Lib.Pair<>(p, s));
    }

    @Override
    public void run()
    {
        while(this.m_running)
        {
            Lib.Pair<Lib.Pair<String, Integer>, String> current_item = null;
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
                    String message = current_item.second;
                    this.m_sender.SendPacket(message, ip_address, port);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean m_running;
    private final core.peer.Node m_sender;
    private final LinkedBlockingQueue<Lib.Pair<Lib.Pair<String, Integer>, String>> m_queue;
}
