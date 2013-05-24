package com.jetbrains.plugins.embeditor;

import com.intellij.codeInsight.lookup.impl.CompletionExtender;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.AbstractLayoutManager;
import com.intellij.util.ui.ButtonlessScrollBarUI;
import com.jediterm.emulator.ui.SwingJediTerminal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private boolean myShown = false;

  public EmbeditorCompletionLookup(final SwingJediTerminal terminal) {
    super(new JPanel(new BorderLayout()));

    CollectionListModel<String> model = new CollectionListModel<String>();

    myList = new JBList(model) {
      @Override
      protected void processKeyEvent(final KeyEvent e) {
        final char keyChar = e.getKeyChar();

        if ((keyChar == KeyEvent.VK_ENTER || keyChar == KeyEvent.VK_TAB)) {
          String text = (String) myList.getSelectedValue();
          try {
            terminal.getEmulator().sendString(text); //TODO: handle prefix
          }
          catch (IOException e1) {
            e1.printStackTrace();  //TODO
          }
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
  }


  public void setVariants(List<String> variants) {
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

  private CollectionListModel<String> getListModel() {
    //noinspection unchecked
    return (CollectionListModel<String>)myList.getModel();
  }

  void show(EmbeddedEditorPanel panel) {
    if (!myShown) {
      JLayeredPane layeredPane = panel.getRootPane().getLayeredPane();
      Point p = panel.getShowLookupPoint();

      Dimension size = new Dimension(0, 0);

      if (isRealPopup()) {
        final Point editorCorner = panel.getLocation();
        SwingUtilities.convertPointToScreen(editorCorner, layeredPane);
        final Point point = new Point(p);
        SwingUtilities.convertPointToScreen(point, layeredPane);
        final Rectangle editorScreen = ScreenUtil.getScreenRectangle(point.x, point.y);

        SwingUtilities.convertPointToScreen(p, layeredPane);
        final Rectangle rectangle = new Rectangle(p, size);
        ScreenUtil.moveToFit(rectangle, editorScreen, null);
        p = rectangle.getLocation();
        SwingUtilities.convertPointFromScreen(p, layeredPane);
      }
      else if (layeredPane.getWidth() < p.x + size.width) {
        p.x = Math.max(0, layeredPane.getWidth() - size.width);
      }


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
