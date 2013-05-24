package com.jetbrains.plugins.embeditor;

import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.jediterm.emulator.display.BackBuffer;
import com.jediterm.emulator.display.LinesBuffer;
import com.jediterm.emulator.display.StyleState;
import org.jetbrains.plugins.terminal.JBTerminalPanel;

import java.awt.*;

/**
 * @author traff
 */
public class EmbeddedEditorPanel extends JBTerminalPanel {

  public EmbeddedEditorPanel(BackBuffer backBuffer,
                             LinesBuffer scrollBuffer,
                             StyleState styleState,
                             EditorColorsScheme scheme) {
    super(backBuffer, scrollBuffer, styleState, scheme);
  }

  public void drawError(int x, int y) {

  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
  }

  public Point getShowLookupPoint() {
    int x = (getTerminalCursor().getCoordX())*myCharSize.width;
    int y = (getTerminalCursor().getCoordY()-1)*myCharSize.height;

    return new Point(x, y);
  }
}
