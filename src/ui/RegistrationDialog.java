package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

import app.Main;
import logic.ApplicationService;

public class RegistrationDialog extends JDialog
        implements ActionListener {

    private ApplicationService service;

    private JLabel idLabel, nameLabel, emailLabel;
    private JLabel phoneLabel, passwordLabel;

    private JTextField idField, nameField, emailField, phoneField;
    private JPasswordField passwordField;

    private JButton submitButton, backButton;

    public RegistrationDialog(JFrame parent, Main navigation,ApplicationService service) {

        super(parent, true);

        this.service = service;

        setTitle("Student Registration");
        setSize(450, 350);

        GridLayout layout = new GridLayout(7, 2);
        setLayout(layout);

        idLabel = new JLabel("University ID:");
        nameLabel = new JLabel("Full Name:");
        emailLabel = new JLabel("Email:");
        phoneLabel = new JLabel("Phone:");
        passwordLabel = new JLabel("Password:");

        idField = new JTextField();
        nameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();
        passwordField = new JPasswordField();

        submitButton = new JButton("Register");
        backButton = new JButton("Back");

        add(new JLabel("Student Registration"));
        add(new JLabel(""));

        add(idLabel);
        add(idField);

        add(nameLabel);
        add(nameField);

        add(emailLabel);
        add(emailField);

        add(phoneLabel);
        add(phoneField);

        add(passwordLabel);
        add(passwordField);

        add(submitButton);
        add(backButton);

        submitButton.addActionListener(this);
        backButton.addActionListener(this);

        if (service.isDirty()) {
            submitButton.setText("Retry Save");
        }
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == submitButton) {

            // Retry saving an earlier change without registering twice.
            if (service.isDirty()) {

                try {
                    service.saveNow();

                    JOptionPane.showMessageDialog(this,"Changes saved. You can now continue.");

                    submitButton.setText("Register");

                } catch (IOException exception) {

                    JOptionPane.showMessageDialog(this,"Saving failed: " + exception.getMessage());
                }

                return;
            }

            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (id.equals("") || name.equals("")
                    || email.equals("") || phone.equals("")
                    || password.trim().equals("")) {

                JOptionPane.showMessageDialog(this,"Please fill in all five fields.");

            } else {

                try {
                    service.register(id, name, email, phone, password);

                    JOptionPane.showMessageDialog(this,"Registration submitted.\n"+ "Your request is pending administrator approval.");

                    passwordField.setText("");
                    dispose();

                } catch (IllegalArgumentException exception) {

                    JOptionPane.showMessageDialog(
                            this, exception.getMessage());

                } catch (IllegalStateException exception) {

                    JOptionPane.showMessageDialog(
                            this, exception.getMessage());

                } catch (SecurityException exception) {

                    JOptionPane.showMessageDialog(
                            this, exception.getMessage());

                } catch (IOException exception) {

                    if (service.isDirty()) {

                        submitButton.setText("Retry Save");

                        JOptionPane.showMessageDialog(this,
                            "Your request is in memory but was not saved.\n"+
                             "Click Retry Save before continuing.");

                    } else {

                        JOptionPane.showMessageDialog(this,"Saving failed: " + exception.getMessage());
                    }
                }
            }

        } else if (e.getSource() == backButton) {

            if (service.isDirty()) {

                JOptionPane.showMessageDialog(this,
                        "Please click Retry Save before going back.");

            } else {

                passwordField.setText("");
                dispose();
            }
        }
    }
}