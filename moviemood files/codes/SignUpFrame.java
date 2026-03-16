import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

public class SignUpFrame extends JFrame {

    private JTextField firstNameField, lastNameField, emailField;
    private JPasswordField passwordField;
    private JButton signUpButton;

    // Colors
    private Color darkGray = new Color(25, 25, 25);
    private Color lightGray = new Color(211, 211, 211);

    public SignUpFrame() {
        setTitle("Movie Mood - Sign Up");
        setSize(1200, 900);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(darkGray);
        setLayout(new BorderLayout());

        initComponents();

        setVisible(true);
    }

    private void initComponents() {
        // Create the center panel with gray background
        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(lightGray);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 30, 40, 30));

        // Create form components
        JLabel firstNameLabel = new JLabel("First Name:");
        firstNameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        firstNameLabel.setForeground(Color.BLACK);
        firstNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        firstNameField = new JTextField();
        firstNameField.setFont(new Font("Arial", Font.PLAIN, 16));
        firstNameField.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        firstNameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lastNameLabel = new JLabel("Last Name:");
        lastNameLabel.setFont(new Font("Arial", Font.BOLD, 24));
        lastNameLabel.setForeground(Color.BLACK);
        lastNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        lastNameField = new JTextField();
        lastNameField.setFont(new Font("Arial", Font.PLAIN, 16));
        lastNameField.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        lastNameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emailLabel = new JLabel("Mail:");
        emailLabel.setFont(new Font("Arial", Font.BOLD, 24));
        emailLabel.setForeground(Color.BLACK);
        emailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emailField = new JTextField();
        emailField.setFont(new Font("Arial", Font.PLAIN, 16));
        emailField.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        emailField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 24));
        passwordLabel.setForeground(Color.BLACK);
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setMaximumSize(new Dimension(Short.MAX_VALUE, 50));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);

        signUpButton = new JButton("Sign Up");
        signUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String firstName = firstNameField.getText();
                String lastName = lastNameField.getText();
                String email = emailField.getText();
                char[] password = passwordField.getPassword();

                boolean success = false;

                try {
                    if (UserController.register(email, firstName, lastName, new String(password))) {
                        success = true;
                    }
                    if (success) {

                        JOptionPane.showMessageDialog(SignUpFrame.this,
                                "Registration successful!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        // Registration failed
                        JOptionPane.showMessageDialog(SignUpFrame.this,
                                "Sign Up was not successful",
                                "Registration Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } finally {

                    java.util.Arrays.fill(password, '0');
                }
            }
        });

        signUpButton.setFont(new Font("Arial", Font.PLAIN, 16));
        signUpButton.setBackground(Color.WHITE);
        signUpButton.setForeground(Color.BLACK);
        signUpButton.setFocusPainted(false);
        signUpButton.setMaximumSize(new Dimension(350, 70));
        signUpButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        signUpButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(firstNameLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(firstNameField);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        centerPanel.add(lastNameLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(lastNameField);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        centerPanel.add(emailLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(emailField);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        centerPanel.add(passwordLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(passwordField);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 50)));

        centerPanel.add(signUpButton);

        JPanel leftMargin = new JPanel();
        leftMargin.setBackground(darkGray);
        leftMargin.setPreferredSize(new Dimension(120, 0));

        JPanel rightMargin = new JPanel();
        rightMargin.setBackground(darkGray);
        rightMargin.setPreferredSize(new Dimension(120, 0));

        add(leftMargin, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightMargin, BorderLayout.EAST);
    }

    public JButton getSignUpButton() {
        return signUpButton;
    }
}