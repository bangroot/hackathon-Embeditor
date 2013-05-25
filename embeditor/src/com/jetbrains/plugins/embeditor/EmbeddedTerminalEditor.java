package com.jetbrains.plugins.embeditor;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jediterm.emulator.display.BackBuffer;
import com.jediterm.emulator.display.LinesBuffer;
import com.jediterm.emulator.display.StyleState;
import com.jediterm.emulator.ui.SwingTerminalPanel;
import com.jetbrains.plugins.embeditor.completion.EmbeditorCompletionLookup;
import com.jetbrains.plugins.embeditor.completion.EmbeditorCompletionUtil;
import org.jetbrains.plugins.embeditor.EmbeditorUtil;
import org.jetbrains.plugins.embeditor.ResolveOutcome;
import org.jetbrains.plugins.terminal.JBTerminal;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

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
        if (e.getModifiers() == InputEvent.CTRL_MASK || e.getModifiers() == InputEvent.META_MASK) {
          if (e.getKeyCode() == 32) {
            handleCompletion();
            return true;
          }
          if (e.getKeyCode() == 66) {
            handleResolve();
            return true;
          }
        }

        return false;
      }
    };
  }

  private void handleResolve() {
    Pair<Integer, Integer> cursor = myVimInstance.getCursorPosition();

    String content = myVimInstance.getContent();


    ResolveOutcome outcome = EmbeditorUtil.getResolveOutcome(myVimInstance.getFilePath(), content, cursor.first - 1, cursor.second);

    if (outcome != ResolveOutcome.NULL) {
      File f = new File(outcome.getFilePath());
      if (!FileUtil.filesEqual(f, new File(myVimInstance.getFilePath()))) {
        FileEditor[] editors = FileEditorManager.getInstance(myProject).openFile(VfsUtil.findFileByIoFile(f, true), true);
      }
      else {
        myVimInstance.navigate(outcome.getRow(), outcome.getColumn());
      }
    }
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
