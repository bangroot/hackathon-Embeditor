package org.jetbrains.plugins.embeditor;

/**
 * @author traff
 */
public class ResolveOutcome {
  public static final ResolveOutcome NULL = new ResolveOutcome("", -1, -1, "");

  private final String myFilePath;
  private final int myColumn;
  private final int myRow;

  private final String myLinePreview;

  public ResolveOutcome(String path, int row, int column, String linePreview) {
    myFilePath = path;
    myColumn = column;
    myRow = row;
    myLinePreview = linePreview != null ? linePreview : "";
  }

  public String getFilePath() {
    return myFilePath;
  }
  public int getColumn() {
    return myColumn;
  }
  public int getRow() { return myRow; }
  public String getLinePreview() { return myLinePreview; }
}
