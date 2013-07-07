package com.jediterm.terminal.ui;

import com.jediterm.terminal.RequestOrigin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author traff
 */
public class TabbedTerminalWidget extends JPanel implements TerminalWidget {
  private static final int FIRST_TAB_NUMBER = 1;

  private int myTabNumber = FIRST_TAB_NUMBER;

  private TerminalPanelListener myTerminalPanelListener = null;

  private JediTermWidget myTermWidget = null;

  private JTabbedPane myTabbedPane;

  public TabbedTerminalWidget() {
    super(new BorderLayout());
  }

  @Override
  public TerminalSession createTerminalSession() {
    JediTermWidget terminal = new JediTermWidget();
    if (myTerminalPanelListener != null) {
      terminal.setTerminalPanelListener(myTerminalPanelListener);
    }

    if (myTermWidget == null && myTabbedPane == null) {
      myTermWidget = terminal;
      Dimension size = terminal.getComponent().getSize();

      add(myTermWidget.getComponent(), BorderLayout.CENTER);
      setSize(size);

      if (myTerminalPanelListener != null) {
        myTerminalPanelListener.onPanelResize(size, RequestOrigin.User);
      }

      onSessionChanged();
    }
    else {
      if (myTabbedPane == null) {
        myTabbedPane = setupTabbedPane();
      }

      addTab(terminal, myTabbedPane);
    }
    return terminal;
  }

  private void addTab(JediTermWidget terminal, JTabbedPane tabbedPane) {
    String tabName = "Terminal " + myTabNumber++;
    tabbedPane.addTab(tabName, null, terminal);

    tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new TabComponent(tabbedPane, terminal));
    tabbedPane.setSelectedComponent(terminal);
  }

  private JTabbedPane setupTabbedPane() {
    final JTabbedPane tabbedPane = createTabbedPane();

    tabbedPane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent event) {
        onSessionChanged();
      }
    });

    myTabNumber = FIRST_TAB_NUMBER;

    remove(myTermWidget);

    addTab(myTermWidget, tabbedPane);

    myTermWidget = null;

    add(tabbedPane, BorderLayout.CENTER);

    return tabbedPane;
  }

  private void onSessionChanged() {
    if (myTerminalPanelListener != null) {
      TerminalSession session = getCurrentSession();
      if (session != null) {
        myTerminalPanelListener.onSessionChanged(session);
      }
    }
  }

  protected JTabbedPane createTabbedPane() {
    return new JTabbedPane();
  }

  private JPopupMenu createPopup(final JediTermWidget terminal) {
    JPopupMenu popupMenu = new JPopupMenu();

    JMenuItem close = new JMenuItem("Close");
    close.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        terminal.close();
        removeTab(terminal);
      }
    });

    popupMenu.add(close);

    return popupMenu;
  }

  private void removeTab(JediTermWidget terminal) {
    if (myTabbedPane.getTabCount() == 2) {
      myTermWidget = getTerminalPanel(0);
      myTabbedPane.removeAll();
      remove(myTabbedPane);
      myTabbedPane = null;
      add(myTermWidget.getComponent(), BorderLayout.CENTER);
      myTermWidget.requestFocusInWindow();

      onSessionChanged();
    }
    else {
      myTabbedPane.remove(terminal);
    }
  }

  private class TabComponent extends JPanel {

    private TabComponent(final @NotNull JTabbedPane pane, final JediTermWidget terminal) {
      super(new FlowLayout(FlowLayout.LEFT, 0, 0));
      setOpaque(false);

      //make JLabel read titles from JTabbedPane
      JLabel label = new JLabel() {
        public String getText() {
          int i = pane.indexOfTabComponent(TabComponent.this);
          if (i != -1) {
            return pane.getTitleAt(i);
          }
          return null;
        }
      };

      //add more space between the label and the button
      label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

      label.addMouseListener(new MouseAdapter() {

        @Override
        public void mouseReleased(MouseEvent event) {
          handleMouse(event);
        }

        @Override
        public void mousePressed(MouseEvent event) {
          handleMouse(event);
        }

        private void handleMouse(MouseEvent event) {
          if (event.isPopupTrigger()) {
            JPopupMenu menu = createPopup(terminal);
            menu.show(event.getComponent(), event.getX(), event.getY());
          }
          else {
            pane.setSelectedComponent(terminal);
          }
        }
      });


      add(label);
    }
  }


  @Override
  public Component getComponent() {
    return this;
  }

  @Override
  public boolean canOpenSession() {
    return true;
  }

  @Override
  public void setTerminalPanelListener(TerminalPanelListener terminalPanelListener) {
    if (myTabbedPane != null) {
      for (int i = 0; i < myTabbedPane.getTabCount(); i++) {
        getTerminalPanel(i).setTerminalPanelListener(terminalPanelListener);
      }
    }
    myTerminalPanelListener = terminalPanelListener;
  }

  @Override
  @Nullable
  public TerminalSession getCurrentSession() {
    if (myTabbedPane != null) {
      return getTerminalPanel(myTabbedPane.getSelectedIndex());
    }
    else {
      return myTermWidget;
    }
  }

  @Nullable
  private JediTermWidget getTerminalPanel(int index) {
    if (index < myTabbedPane.getTabCount() && index >= 0) {
      return (JediTermWidget)myTabbedPane.getComponentAt(index);
    }
    else {
      return null;
    }
  }
}