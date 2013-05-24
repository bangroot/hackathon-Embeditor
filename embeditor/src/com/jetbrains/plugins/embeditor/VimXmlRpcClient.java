package com.jetbrains.plugins.embeditor;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.NetUtils;
import org.apache.xmlrpc.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

/**
 * @author tarff
 */
public class VimXmlRpcClient {
  private XmlRpcClient impl;

  private Process process;

  private static final Logger LOG = Logger.getInstance(VimXmlRpcClient.class.getName());

  private static final long TIME_LIMIT = 60000;


  public VimXmlRpcClient(Process process, int port)
    throws MalformedURLException {

    String hostname = NetUtils.getLocalHostString();

    URL url = new URL("http://" + hostname + ':' + port + "/RPC2");

    XmlRpc.setDefaultInputEncoding("UTF8");
    this.impl = new XmlRpcClientLite(url);
    this.process = process;
  }

  public Object execute(String command, Object[] args) throws XmlRpcException {
    final Object[] result = new Object[]{null};

    //make an async call so that we can keep track of not actually having an answer.
    this.impl.executeAsync(command, new Vector(Arrays.asList(args)), new AsyncCallback() {

      public void handleError(Exception error, URL url, String method) {
        result[0] = new Object[]{error.getMessage()};
      }

      public void handleResult(Object recievedResult, URL url, String method) {
        result[0] = recievedResult;
      }
    });

    long started = System.currentTimeMillis();
    //busy loop waiting for the answer (or having the console die).
    while (result[0] == null && System.currentTimeMillis() - started < TIME_LIMIT) {
      try {
        if (process != null) {
          int exitValue = process.exitValue();
          result[0] = new Object[]{String.format("Console already exited with value: %s while waiting for an answer.\n", exitValue)};
          //ok, we have an exit value!
          break;
        }
      }
      catch (IllegalThreadStateException e) {
        //that's ok... let's sleep a bit
        synchronized (this) {
          try {
            wait(10);
          }
          catch (InterruptedException e1) {
            LOG.error(e1);
          }
        }
      }
    }
    if (result[0] == null) {
      throw new XmlRpcException(-1, "Timeout while connecting to server");
    }
    return result[0];
  }
}
