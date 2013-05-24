package com.jetbrains.plugins.embeditor.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.CompletionExtender;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ExpandableItemsHandler;
import com.intellij.ui.HintHint;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.ButtonlessScrollBarUI;
import com.jetbrains.plugins.embeditor.EmbeddedEditorPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.JBTerminal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;

/**
 * @author traff
 */
public class EmbeditorCompletionLookup extends LightweightHint {
  final LookupLayeredPane myLayeredPane = new LookupLayeredPane();

  private final JScrollPane myScrollPane;
  private final JBList myList;
  private final JButton myScrollBarIncreaseButton;

  private volatile int myLookupTextWidth = 200;

  private int myMaximumHeight = Integer.MAX_VALUE;

  private final EmbeditorLookupCellRenderer myCellRenderer;

  private boolean myShown = false;

  public EmbeditorCompletionLookup(Project project, final JBTerminal terminal) {
    super(new JPanel(new BorderLayout()));

    CollectionListModel<LookupElement> model = new CollectionListModel<LookupElement>();

    myList = new JBList(model) {
      @Override
      protected void processKeyEvent(final KeyEvent e) {
        final char keyChar = e.getKeyChar();

        if ((keyChar == KeyEvent.VK_ENTER || keyChar == KeyEvent.VK_TAB)) {
          LookupElement lookupElement = (LookupElement)myList.getSelectedValue();
          try {
            terminal.getEmulator().sendString(lookupElement.getLookupString()); //TODO: handle prefix
          }
          catch (IOException e1) {
            e1.printStackTrace();  //TODO
          }
          doHide();
        }

        if (keyChar == KeyEvent.VK_ESCAPE) {
          doHide();
        }

        super.processKeyEvent(e);
      }

      ExpandableItemsHandler<Integer> myExtender = new CompletionExtender(this);

      @NotNull
      @Override
      public ExpandableItemsHandler<Integer> getExpandableItemsHandler() {
        return myExtender;
      }
    };

    myScrollBarIncreaseButton = new JButton();
    myScrollBarIncreaseButton.setFocusable(false);
    myScrollBarIncreaseButton.setRequestFocusEnabled(false);

    myScrollPane = new JBScrollPane(myList);
    myScrollPane.setViewportBorder(new EmptyBorder(0, 0, 0, 0));
    myScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    myScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(13, -1));
    myScrollPane.getVerticalScrollBar().setUI(new ButtonlessScrollBarUI() {
      @Override
      protected JButton createIncreaseButton(int orientation) {
        return myScrollBarIncreaseButton;
      }
    });
    getComponent().add(myLayeredPane, BorderLayout.CENTER);


    myLayeredPane.mainPanel.add(myScrollPane, BorderLayout.CENTER);
    myScrollPane.setBorder(null);

    myCellRenderer = new EmbeditorLookupCellRenderer(this, terminal.getColorScheme());
    myList.setCellRenderer(myCellRenderer);
  }


  public void setVariants(List<LookupElement> variants) {
    getListModel().removeAll();
    getListModel().add(variants);
  }

  private void doHide() {
    hide();
    myShown = false;
  }

  public boolean isShown() {
    return myShown;
  }

  private CollectionListModel<LookupElement> getListModel() {
    //noinspection unchecked
    return (CollectionListModel<LookupElement>)myList.getModel();
  }

  public void show(EmbeddedEditorPanel panel) {
    if (!myShown) {
      JLayeredPane layeredPane = panel.getRootPane().getLayeredPane();
      Point p = panel.getShowLookupPoint();

      SwingUtilities.convertPointToScreen(p, panel);

      show(layeredPane, p.x, p.y, panel, new HintHint());

      IdeFocusManager.getInstance(null).requestFocus(myList, true);

      myShown = true;
    }
  }


  private class LookupLayeredPane extends JBLayeredPane {
    final JPanel mainPanel = new JPanel(new BorderLayout());

    private LookupLayeredPane() {
      add(mainPanel, 0, 0);
      //add(myIconPanel, 42, 0);
      //add(mySortingLabel, 10, 0);

      setLayout(new AbstractLayoutManager() {
        @Override
        public Dimension preferredLayoutSize(@Nullable Container parent) {
          int maxCellWidth = myLookupTextWidth;//+ myCellRenderer.getIconIndent();
          int scrollBarWidth = myScrollPane.getPreferredSize().width - myScrollPane.getViewport().getPreferredSize().width;
          int listWidth = Math.min(scrollBarWidth + maxCellWidth, UISettings.getInstance().MAX_LOOKUP_WIDTH2);


          int panelHeight = myList.getPreferredScrollableViewportSize().height;
          if (getListModel().getSize() > myList.getVisibleRowCount() && myList.getVisibleRowCount() >= 5) {
            panelHeight -= myList.getFixedCellHeight() / 2;
          }
          return new Dimension(listWidth, Math.min(panelHeight, myMaximumHeight));
        }

        @Override
        public void layoutContainer(Container parent) {
          Dimension size = getSize();
          mainPanel.setSize(size);
          mainPanel.validate();

          //if (!myResizePending) {
          Dimension preferredSize = preferredLayoutSize(null);
          if (preferredSize.width != size.width) {
            UISettings.getInstance().MAX_LOOKUP_WIDTH2 = Math.max(500, size.width);
          }

          int listHeight = myList.getLastVisibleIndex() - myList.getFirstVisibleIndex() + 1;
          if (listHeight != getListModel().getSize() &&
              listHeight != myList.getVisibleRowCount() &&
              preferredSize.height != size.height) {
            UISettings.getInstance().MAX_LOOKUP_LIST_HEIGHT = Math.max(5, listHeight);
          }
          //}

          myList.setFixedCellWidth(myScrollPane.getViewport().getWidth());
          layoutStatusIcons();
          layoutHint();
        }
      });
    }

    private void layoutStatusIcons() {
      Dimension buttonSize = new Dimension(
        AllIcons.Ide.LookupRelevance.getIconWidth(), AllIcons.Ide.LookupRelevance.getIconHeight());
      myScrollBarIncreaseButton.setPreferredSize(buttonSize);
      myScrollBarIncreaseButton.setMinimumSize(buttonSize);
      myScrollBarIncreaseButton.setMaximumSize(buttonSize);
      JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
      scrollBar.revalidate();
      scrollBar.repaint();

      //final Dimension iconSize = myProcessIcon.getPreferredSize();
      //myIconPanel
      //  .setBounds(getWidth() - iconSize.width - (scrollBar.isVisible() ? scrollBar.getWidth() : 0), 0, iconSize.width, iconSize.height);

      //final Dimension sortSize = mySortingLabel.getPreferredSize();
      //final Point sbLocation = SwingUtilities.convertPoint(scrollBar, 0, 0, myLayeredPane);

      //final int sortHeight = Math.max(adHeight, mySortingLabel.getPreferredSize().height);
      //mySortingLabel.setBounds(sbLocation.x, getHeight() - sortHeight, sortSize.width, sortHeight);
    }

    void layoutHint() {
      //if (myElementHint != null && getCurrentItem() != null) {
      //  final Rectangle bounds = getCurrentItemBounds();
      //  myElementHint.setSize(myElementHint.getPreferredSize());
      //  JScrollBar sb = myScrollPane.getVerticalScrollBar();
      //  myElementHint
      //    .setLocation(new Point(bounds.x + bounds.width - myElementHint.getWidth() + (sb.isVisible() ? sb.getWidth() : 0), bounds.y));
      //}
    }
  }
}
