package ui;

import java.awt.*;
import java.awt.event.*;

import java.io.IOException;
import java.text.ParseException;

import java.time.LocalDateTime;

import java.util.Calendar;
import java.util.Date;

import javax.swing.*;

import logic.ApplicationService;

public class ClockSetupDialog extends JDialog
        implements ActionListener, WindowListener {

    private ApplicationService service;

    private JSpinner dateTimeSpinner;

    private JButton startButton;
    private JButton cancelButton;

    private boolean successful;

    public ClockSetupDialog(JFrame parent,
                            ApplicationService service) {

        super(parent, "Set simulation clock", true);

        if (service == null) {
            throw new IllegalArgumentException("Application service is required.");
        }

        if (service.getCurrentUser() == null
                || !"ADMIN".equals(
                    service.getCurrentUser().getRole())) {

            throw new SecurityException("Only the administrator can set the clock.");
        }

        if (service.isClockInitialized()) {
            throw new IllegalStateException("The clock is already initialized.");
        }

        if (service.isDirty()) {
            throw new IllegalStateException("Save existing changes before setting the clock.");
        }

        this.service = service;
        successful = false;

        buildUi();

        setLocationRelativeTo(parent);
    }

    private void buildUi() {

        setSize(540, 190);
        setResizable(false);

        setLayout(new GridLayout(3, 2, 8, 8));

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        SpinnerDateModel model = new SpinnerDateModel(
            new Date(),
            null,
            null,
            Calendar.MINUTE
        );

        dateTimeSpinner = new JSpinner(model);

        JSpinner.DateEditor editor = new JSpinner.DateEditor(
            dateTimeSpinner,
            "dd/MM/yyyy HH:mm"
        );

        editor.getFormat().setLenient(false);

        dateTimeSpinner.setEditor(editor);

        startButton = new JButton("Start clock");
        cancelButton = new JButton("Cancel");

        startButton.addActionListener(this);
        cancelButton.addActionListener(this);

        add(new JLabel("Choose the starting date and time."));
        add(new JLabel("Then use Advance Minute."));

        add(new JLabel("Date/time (dd/MM/yyyy HH:mm):"));
        add(dateTimeSpinner);

        add(startButton);
        add(cancelButton);
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        if (event.getSource() == startButton) {
            handleStart();

        } else if (event.getSource() == cancelButton) {
            handleCancel();
        }
    }

    private void handleStart() {

        try {

            if (service.isClockInitialized()) {
                service.saveNow();

            } else {

                dateTimeSpinner.commitEdit();

                Calendar selected = Calendar.getInstance();

                selected.setTime(
                    (Date) dateTimeSpinner.getValue()
                );

                LocalDateTime start = LocalDateTime.of(
                    selected.get(Calendar.YEAR),
                    selected.get(Calendar.MONTH) + 1,
                    selected.get(Calendar.DAY_OF_MONTH),
                    selected.get(Calendar.HOUR_OF_DAY),
                    selected.get(Calendar.MINUTE)
                );

                service.initializeClock(start);
            }

            successful = true;

            dispose();

        } catch (ParseException exception) {

            showError(
                "Enter a valid date and time using dd/MM/yyyy HH:mm."
            );

        } catch (IOException exception) {

            dateTimeSpinner.setEnabled(false);
            startButton.setText("Retry save");

            showError(
                "The clock was initialized but could not be saved.\n"
                + "Click Retry save; do not initialize it again.\n"
                + exception.getMessage()
            );

        } catch (IllegalArgumentException exception) {

            showError(exception.getMessage());

        } catch (IllegalStateException exception) {

            showError(exception.getMessage());

        } catch (SecurityException exception) {

            showError(exception.getMessage());
        }
    }

    private void handleCancel() {

        if (service.isDirty()) {

            showError(
                "The clock has unsaved changes.\n"
                + "Click Retry save before closing."
            );

            return;
        }

        dispose();
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
            this,
            message,
            "Clock setup",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public void windowClosing(WindowEvent event) {
        handleCancel();
    }

    @Override
    public void windowOpened(WindowEvent event) {
    }

    @Override
    public void windowClosed(WindowEvent event) {
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