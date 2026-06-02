import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;

public class MovieMoodLoginUI extends JFrame {
    // Components
    private JPanel leftPanel, rightPanel, formPanel;
    private JLabel titleLabel, subtitleLabel, questionLabel;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton signInButton, signUpButton;

    // Modern Color Palette - Netflix Red Theme
    private Color primary = new Color(229, 9, 20); // Netflix red
    private Color primaryDark = new Color(153, 6, 13); // Darker Netflix red
    private Color accent = new Color(255, 40, 40); // Bright red accent
    private Color darkBg = new Color(18, 18, 18); // Deep black
    private Color textPrimary = new Color(255, 255, 255);

    public MovieMoodLoginUI() {
        setTitle("Movie Mood");
        setSize(900, 600);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));

        // Set dark background
        getContentPane().setBackground(darkBg);

        initComponents();
        layoutComponents();

        setVisible(true);
    }

    private void initComponents() {
        // Left Panel with Gradient
        leftPanel = new GradientPanel(primaryDark, primary);
        leftPanel.setLayout(new BorderLayout());

        // Right Panel with dark gradient
        rightPanel = new GradientPanel(darkBg, new Color(25, 25, 35));
        rightPanel.setLayout(new BorderLayout());

        // Form Panel
        formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        // Title Label with modern font
        titleLabel = new JLabel("Movie Mood");
        titleLabel.setForeground(textPrimary);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtitle
        subtitleLabel = new JLabel("Welcome back!");
        subtitleLabel.setForeground(new Color(255, 255, 255, 180));
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Question Label
        questionLabel = new JLabel("DON'T HAVE AN ACCOUNT?");
        questionLabel.setForeground(textPrimary);
        questionLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Styled Text Fields
        emailField = createStyledTextField("Enter your email");
        passwordField = createStyledPasswordField("Enter your password");

        // Styled Buttons
        signInButton = createPrimaryButton("Sign In");
        signUpButton = createAccentButton("Create Account");

        // Add action listeners
        signInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String mail = emailField.getText();
                char[] password = passwordField.getPassword();

                try {
                    User newUser = UserController.login(mail, new String(password));
                    if (newUser != null) {
                        FrontendStaticUser.frontEndStaticUser = newUser;
                        JOptionPane.showMessageDialog(MovieMoodLoginUI.this,
                                "Login successful!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);

                        setVisible(false);
                        new ProfileFrame(newUser);
                        emailField.setText("");
                        passwordField.setText("");
                    } else {
                        JOptionPane.showMessageDialog(MovieMoodLoginUI.this,
                                "No user exists with these credentials",
                                "Login Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(MovieMoodLoginUI.this,
                            "Error during login: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    java.util.Arrays.fill(password, '0');
                }
            }
        });

        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SignUpFrame();
            }
        });
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded background
                g2.setColor(new Color(255, 255, 255, 250));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                super.paintComponent(g);
                g2.dispose();
            }
        };

        field.setMaximumSize(new Dimension(400, 50));
        field.setPreferredSize(new Dimension(400, 50));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setForeground(new Color(50, 50, 50));
        field.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        field.setOpaque(false);

        // Placeholder effect
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(new Color(50, 50, 50));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });

        return field;
    }

    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded background
                g2.setColor(new Color(255, 255, 255, 250));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                super.paintComponent(g);
                g2.dispose();
            }
        };

        field.setMaximumSize(new Dimension(400, 50));
        field.setPreferredSize(new Dimension(400, 50));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setForeground(new Color(50, 50, 50));
        field.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        field.setOpaque(false);
        field.setEchoChar((char) 0);

        // Placeholder effect
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setEchoChar('•');
                    field.setForeground(new Color(50, 50, 50));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getPassword().length == 0) {
                    field.setEchoChar((char) 0);
                    field.setText(placeholder);
                    field.setForeground(Color.GRAY);
                }
            }
        });

        return field;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Gradient background
                GradientPaint gp = new GradientPaint(
                        0, 0, getModel().isRollover() ? accent : primary,
                        0, getHeight(), getModel().isRollover() ? primary : primaryDark);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                // Shadow effect
                if (!getModel().isPressed()) {
                    g2.setColor(new Color(0, 0, 0, 50));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 25, 25);
                }

                // Draw text
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        button.setMaximumSize(new Dimension(400, 50));
        button.setPreferredSize(new Dimension(400, 50));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(textPrimary);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    private JButton createAccentButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    // Gradient background on hover
                    GradientPaint gp = new GradientPaint(
                            0, 0, accent,
                            0, getHeight(), primary);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                } else {
                    // Outline style
                    g2.setColor(accent);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 25, 25);
                }

                // Draw text
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        button.setMaximumSize(new Dimension(400, 50));
        button.setPreferredSize(new Dimension(400, 50));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 16));
        button.setForeground(textPrimary);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    private void layoutComponents() {
        // Left panel with form
        formPanel.add(Box.createVerticalGlue());
        formPanel.add(titleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(subtitleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 60)));
        formPanel.add(emailField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(passwordField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 35)));
        formPanel.add(signInButton);
        formPanel.add(Box.createVerticalGlue());

        leftPanel.add(formPanel, BorderLayout.CENTER);

        // Right panel content
        JPanel rightContentPanel = new JPanel();
        rightContentPanel.setLayout(new BoxLayout(rightContentPanel, BoxLayout.Y_AXIS));
        rightContentPanel.setOpaque(false);
        rightContentPanel.setBorder(BorderFactory.createEmptyBorder(50, 60, 50, 60));

        rightContentPanel.add(Box.createVerticalGlue());
        rightContentPanel.add(questionLabel);
        rightContentPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        rightContentPanel.add(signUpButton);
        rightContentPanel.add(Box.createVerticalGlue());

        rightPanel.add(rightContentPanel, BorderLayout.CENTER);

        add(leftPanel);
        add(rightPanel);
    }

    public boolean signInSuccsesfully(String name, char[] password) {
        return true;
    }

    // Custom Panel with Gradient Background
    class GradientPanel extends JPanel {
        private Color color1;
        private Color color2;

        public GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth();
            int h = getHeight();

            GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, w, h);
        }
    }
}
