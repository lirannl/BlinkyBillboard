package ControlPanel;

import Client.ClientConnector;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class EditExistingUser {
    protected JPanel editExistingUserPanel;
    protected JButton backButton;
    protected JTextField usernameTextField;
    protected JPasswordField passwordField;
    protected JPasswordField confirmPasswordField;
    protected JPanel credentialsPanel;
    protected JPanel titlePanel;
    protected JLabel editExistingUserLabel;
    protected JLabel permissionsLabel;
    protected JLabel usernameLabel;
    protected JLabel passwordLabel;
    protected JLabel confirmPasswordLabel;
    protected JCheckBox createBillboardsCheckBox;
    protected JCheckBox editAllBillboardsCheckBox;
    protected JCheckBox scheduleBillboardsCheckBox;
    protected JCheckBox editUsersCheckBox;
    private JButton saveUserButton;

    public EditExistingUser(JFrame frame, ClientConnector connector) {
        backButton.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             *
             * @param e the event to be processed
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setContentPane(new EditUsers(frame, connector).editUsersPanel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });

        saveUserButton.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             *
             * @param e the event to be processed
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!Arrays.equals(confirmPasswordField.getPassword(), passwordField.getPassword())) {
                    JOptionPane.showMessageDialog(null, "Passwords do not match. Try again");
                    return;
                }
            }
        });
    }
}