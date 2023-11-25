package core;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface Command
{
    public void Parse(String[] command) throws InterruptedException, IOException, NoSuchAlgorithmException;
}
