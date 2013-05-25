package com.jetbrains.plugins.embeditor;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;

import java.net.MalformedURLException;
import java.util.Vector;

/**
 * @author traff
 */
public class VimInstance {
  private final VimXmlRpcClient myClient;
  private VirtualFile myVFile;
  private boolean myQuitSent = false;

  public VimInstance(VirtualFile vFile, Process process, int port) throws MalformedURLException {
    myVFile = vFile;
    myClient = new VimXmlRpcClient(process, port);
  }

  public boolean canExecuteCompletion() {
    try {
      Object o = myClient.execute("can_complete", new Object[0]);

      Boolean b = (Boolean)o;

      return b;
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public Pair<Integer, Integer> getCursorPosition() {
    try {
      Object o = myClient.execute("get_cursor", new Object[0]);

      Vector v = (Vector)o;

      return Pair.create((Integer)v.get(0), (Integer)v.get(1));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return Pair.create(2, 2);
  }

  public String getFilePath() {
    return myVFile.getPath();
  }

  public String getContent() {
    try {
      Object o = myClient.execute("get_content", new Object[0]);

      return (String) o;
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public void navigate(int row, int column) {
    try {
      myClient.execute("navigate", new Object[]{new Integer(row), new Integer(column)});
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void saveAndQuit() {
    if (myQuitSent) {
      return;
    }
    myQuitSent = true;
    try {
      myClient.execute("save_and_quit", new Object[]{});
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
