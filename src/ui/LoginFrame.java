package ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import app.Main;
import logic.ApplicationService;

public class LoginFrame extends JFrame implements ActionListener {

    private Main navigation;
    private ApplicationService service;

    private JLabel welcomeLabel, usernameLabel, passwordLabel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton signInButton, registerButton;

    public LoginFrame(Main navigation, ApplicationService service) {

        this.navigation = navigation;
        this.service = service;

        setTitle("GPU Management Login");
        setSize(450, 250);

        GridLayout layout = new GridLayout(4, 2);
        setLayout(layout);

        welcomeLabel = new JLabel("GPU Management Login");
        usernameLabel = new JLabel("University ID / Username:");
        passwordLabel = new JLabel("Password:");

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        signInButton = new JButton("Sign In");
        registerButton = new JButton("Register");

        add(welcomeLabel);
        add(new JLabel(""));

        add(usernameLabel);
        add(usernameField);

        add(passwordLabel);
        add(passwordField);

        add(signInButton);
        add(registerButton);

        signInButton.addActionListener(this);
        registerButton.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == signInButton) {

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.equals("") || password.trim().equals("")) {

                JOptionPane.showMessageDialog(this,"Please enter your username and password.");

            } else {

                boolean loggedIn = false;

                try {
                    service.login(username, password);
                    loggedIn = true;

                } catch (IllegalArgumentException exception) {

                    JOptionPane.showMessageDialog(this, exception.getMessage());

                } catch (IllegalStateException exception) {

                    JOptionPane.showMessageDialog( this, exception.getMessage());

                } catch (SecurityException exception) {

                    JOptionPane.showMessageDialog(this, exception.getMessage());
                }

                passwordField.setText("");

                if (loggedIn) {
                    navigation.showHome();
                }
            }

        } else if (e.getSource() == registerButton) {

            passwordField.setText("");
            navigation.showRegistration();
        }
    }
}