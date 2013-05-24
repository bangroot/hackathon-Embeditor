package org.jetbrains.plugins.embeditor;

/**
 * @author traff
 */
public class ResolveOutcome {
  private final String myFilePath;
  private final int myColumn;
  private final int myRow;
  public static final ResolveOutcome NULL = new ResolveOutcome("", -1, -1);

  public ResolveOutcome(String path, int columnn, int row) {
    myFilePath = path;
    myColumn = columnn;
    myRow = row;
  }

  public String getFilePath() {
    return myFilePath;
  }

  public int getColumn() {
    return myColumn;
  }

  public int getRow() {
    return myRow;
  }
}
