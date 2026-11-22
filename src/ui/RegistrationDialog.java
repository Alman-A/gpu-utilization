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

    private JTextField idField, nameField, emailField, phoneField;
    private JPasswordField passwordField;
    private JButton submitButton, backButton;

    private boolean registrationWaitingForSave;

    public RegistrationDialog(JFrame parent, Main navigation,
                              ApplicationService service) {

        super(parent, true);

        this.service = service;
        registrationWaitingForSave = false;

        setTitle("Student Registration");
        setSize(450, 350);
        setLayout(new GridLayout(7, 2));

        idField = new JTextField();
        nameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();
        passwordField = new JPasswordField();

        submitButton = new JButton("Register");
        backButton = new JButton("Back");

        add(new JLabel("Student Registration"));
        add(new JLabel(""));

        add(new JLabel("University ID:"));
        add(idField);

        add(new JLabel("Full Name:"));
        add(nameField);

        add(new JLabel("Email:"));
        add(emailField);

        add(new JLabel("Phone:"));
        add(phoneField);

        add(new JLabel("Password:"));
        add(passwordField);

        add(submitButton);
        add(backButton);

        submitButton.addActionListener(this);
        backButton.addActionListener(this);

        // X must check unsaved changes just like Back.
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                handleBack();
            }
        });

        if (service.isDirty()) {
            submitButton.setText("Retry Save");
            setFieldsEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == submitButton) {
            handleRegister();

        } else if (e.getSource() == backButton) {
            handleBack();
        }
    }

    private void handleRegister() {

        if (service.isDirty()) {

            try {
                // Save the existing request without registering it again.
                service.saveNow();

                if (registrationWaitingForSave) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Registration saved.\n"
                            + "Your request is pending administrator approval.");

                } else {

                    JOptionPane.showMessageDialog(
                            this, "Changes saved.");
                }

                passwordField.setText("");
                dispose();

            } catch (IOException exception) {

                JOptionPane.showMessageDialog(
                        this,
                        "Saving failed: " + exception.getMessage());
            }

            return;
        }

        // Another part of the app may already have saved the request.
        if (registrationWaitingForSave) {
            passwordField.setText("");
            dispose();
            return;
        }

        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (id.equals("") || name.equals("") || email.equals("")
                || phone.equals("") || password.trim().equals("")) {

            JOptionPane.showMessageDialog(
                    this, "Please fill in all five fields.");

            return;
        }

        try {
            service.register(id, name, email, phone, password);

            JOptionPane.showMessageDialog(
                    this,
                    "Registration submitted.\n"
                    + "Your request is pending administrator approval.");

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

                registrationWaitingForSave = true;
                submitButton.setText("Retry Save");

                passwordField.setText("");
                setFieldsEnabled(false);

                JOptionPane.showMessageDialog(
                        this,
                        "Your request is in memory but was not saved.\n"
                        + "Click Retry Save before continuing.\n"
                        + exception.getMessage());

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Saving failed: " + exception.getMessage());
            }
        }
    }

    private void handleBack() {

        if (service.isDirty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please click Retry Save before going back.");

        } else {

            passwordField.setText("");
            dispose();
        }
    }

    private void setFieldsEnabled(boolean enabled) {

        idField.setEnabled(enabled);
        nameField.setEnabled(enabled);
        emailField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
    }
}
