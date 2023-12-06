package core.peer;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

public abstract class Runner implements Runnable
{
    Runner()
    {
        SetState(true);
        this.m_togglelink = new Semaphore(1);
        this.m_toggled = false;
    }

    public final void SetState(boolean state) { this.m_running = state; }

    public void ToggleLink()
    {
        if(!m_toggled)
            this.m_togglelink.acquireUninterruptibly();
        else this.m_togglelink.release();

        m_toggled = !m_toggled;
    }

    protected void Toggle()
    {
        if(m_toggled)
        {
            m_togglelink.acquireUninterruptibly();
            m_togglelink.release();
        }
    }

    protected byte[] AddMagicValuePrefix(byte[] bytes)
    {
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length + Integer.BYTES);
        buffer.put(IntToByteArray(MAGIC_VALUE));
        buffer.put(bytes);
        return buffer.array();
    }

    protected static byte[] IntToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    @Override
    public abstract void run();
    protected boolean m_toggled;
    protected Semaphore m_togglelink;

    protected boolean m_running;
    protected final int MAGIC_VALUE = 0xF00DB33F;
}
