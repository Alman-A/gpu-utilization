package app;

import java.awt.event.*;
import java.io.IOException;

import javax.swing.*;

import logic.ApplicationService;

import model.Student;
import model.User;

import storage.FileDataStore;

import ui.*;

public class Main implements Runnable, WindowListener {

    private ApplicationService service;
    private JFrame activeFrame;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Main());
    }

    @Override
    public void run() {
        start();
    }

    public void start() {

        try {

            service = new ApplicationService(
                new FileDataStore("data/simulation.txt")
            );

            showLogin();

        } catch (IOException exception) {

            showError(
                "The saved application data could not be loaded.\n"
                + exception.getMessage()
            );

        } catch (RuntimeException exception) {

            showError(exception.getMessage());
        }
    }

    public void showLogin() {

        if (service.isDirty()) {

            showError(
                "Changes are unsaved. "
                + "Click Retry save before logging out."
            );

            return;
        }

        if (service.getCurrentUser() != null) {
            service.logout();
        }

        openFrame(new LoginFrame(this, service));
    }

    public void showHome() {

        User user = service.getCurrentUser();

        if (user == null) {
            showLogin();
            return;
        }

        if ("ADMIN".equals(user.getRole())) {

            if (!service.isClockInitialized()) {
                showClockSetup();
                return;
            }

            openFrame(new AdminFrame(this, service));

        } else if (user instanceof Student) {

            openFrame(new StudentFrame(service));

        } else {

            showError("This account has an unknown role.");
        }
    }

    public void showRegistration() {

        if (service.getCurrentUser() != null) {
            throw new SecurityException(
                "Log out before registering an account."
            );
        }

        RegistrationDialog dialog = new RegistrationDialog(
            activeFrame,
            this,
            service
        );

        dialog.setLocationRelativeTo(activeFrame);
        dialog.setVisible(true);
    }

    public void showClockSetup() {

        User user = service.getCurrentUser();

        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("Only the administrator can set the clock.");
        }

        if (service.isClockInitialized()) {
            showHome();
            return;
        }

        if (service.isDirty()) {

            showError(
                "Save existing changes before setting the clock."
            );

            return;
        }

        ClockSetupDialog dialog = new ClockSetupDialog(
            activeFrame,
            service
        );

        dialog.setVisible(true);

        if (dialog.isSuccessful()) {
            showHome();
        } else {
            showLogin();
        }
    }

    private void openFrame(JFrame nextFrame) {

        JFrame previous = activeFrame;

        activeFrame = nextFrame;

        nextFrame.setDefaultCloseOperation(
            JFrame.DO_NOTHING_ON_CLOSE
        );

        nextFrame.addWindowListener(this);

        if (previous != null) {

            previous.removeWindowListener(this);
            previous.dispose();
        }

        nextFrame.setLocationRelativeTo(null);
        nextFrame.setVisible(true);
    }

    private void closeApplication() {

        if (service.isDirty()) {

            int choice = JOptionPane.showConfirmDialog(
                activeFrame,
                "There are unsaved changes.\n"
                + "Save them before exiting?",
                "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION
            );

            if (choice == JOptionPane.CANCEL_OPTION
                    || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }

            if (choice == JOptionPane.YES_OPTION) {

                try {

                    service.saveNow();

                } catch (IOException exception) {

                    showError(
                        "Saving failed. "
                        + "The application will stay open.\n"
                        + exception.getMessage()
                    );

                    return;
                }
            }
        }

        JFrame closing = activeFrame;

        activeFrame = null;

        if (closing != null) {

            closing.removeWindowListener(this);
            closing.dispose();
        }
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
            activeFrame,
            message,
            "GPU application",
            JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void windowClosing(WindowEvent event) {

        if (event.getWindow() != activeFrame) {
            return;
        }

        if (activeFrame instanceof StudentFrame) {
            return;
        }

        closeApplication();
    }

    @Override
    public void windowClosed(WindowEvent event) {

        if (event.getWindow() != activeFrame) {
            return;
        }

        activeFrame = null;

        if (service.getCurrentUser() == null) {
            showLogin();
        }
    }

    @Override
    public void windowOpened(WindowEvent event) {
    }

    @Override
    public void windowIconified(WindowEvent event) {
    }

    @Override
    public void windowDeiconified(WindowEvent event) {
    }

    @Override
    public void windowActivated(WindowEvent event) {
    }

    @Override
    public void windowDeactivated(WindowEvent event) {
    }
}
