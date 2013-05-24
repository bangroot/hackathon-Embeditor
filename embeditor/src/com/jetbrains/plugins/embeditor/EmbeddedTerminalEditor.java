package com.jetbrains.plugins.embeditor;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.jediterm.emulator.display.BackBuffer;
import com.jediterm.emulator.display.LinesBuffer;
import com.jediterm.emulator.display.StyleState;
import com.jediterm.emulator.ui.SwingTerminalPanel;
import com.jetbrains.plugins.embeditor.completion.EmbeditorCompletionLookup;
import com.jetbrains.plugins.embeditor.completion.EmbeditorCompletionUtil;
import org.jetbrains.plugins.terminal.JBTerminal;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * @author traff
 */
public class EmbeddedTerminalEditor extends JBTerminal {

  private EmbeddedEditorPanel myEmbeddedEditorPanel;

  private Project myProject;
  private VimInstance myVimInstance;
  private final EmbeditorCompletionLookup myCompletionLookup;

  public EmbeddedTerminalEditor(Project project, VimInstance instance) {
    myProject = project;
    myVimInstance = instance;
    new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          updateHighlightingData();
          try {
            Thread.sleep(5000);
          }
          catch (InterruptedException e) {
          }
        }
      }
    }).start();

    myCompletionLookup = new EmbeditorCompletionLookup(project, this);


  }

  @Override
  protected KeyListener createEmulatorKeyHandler() {
    final KeyListener terminalEmulatorKeyHandler = super.createEmulatorKeyHandler();

    return new KeyListener() {

      @Override
      public void keyTyped(KeyEvent e) {
        if (!handleKey(e)) {
          terminalEmulatorKeyHandler.keyTyped(e);
        }
      }

      @Override
      public void keyPressed(KeyEvent e) {
        if (!handleKey(e)) {
          terminalEmulatorKeyHandler.keyPressed(e);
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
        terminalEmulatorKeyHandler.keyReleased(e);
      }

      private boolean handleKey(KeyEvent e) {
        if (e.getModifiers() == InputEvent.CTRL_MASK) {
          if (e.getKeyChar() == ' ') {
            handleCompletion();
            return true;
          }
        }

        return false;
      }
    };
  }

  @Override
  protected SwingTerminalPanel createTerminalPanel(StyleState styleState, BackBuffer backBuffer, LinesBuffer scrollBuffer) {
    myEmbeddedEditorPanel = new EmbeddedEditorPanel(backBuffer, scrollBuffer, styleState, createBoundColorSchemeDelegate(null));
    return myEmbeddedEditorPanel;
  }

  private void handleCompletion() {
    if (myVimInstance.canExecuteCompletion()) {
      Pair<Integer, Integer> cursor = myVimInstance.getCursorPosition();

      String content = myVimInstance.getContent();

      Pair<LookupElement[], Integer> variants =
        EmbeditorCompletionUtil.getCompletionVariants(myVimInstance.getFilePath(), content, cursor.first - 1, cursor.second);

      myCompletionLookup.setVariants(Lists.newArrayList(variants.first));
      myCompletionLookup.setPrefixLength(variants.second);

      if (!myCompletionLookup.isShown()) {
        myCompletionLookup.show(myEmbeddedEditorPanel);
      }
    }
  }

  public void updateHighlightingData() {
    //myEmbeddedEditorPanel.drawError();
  }
}
