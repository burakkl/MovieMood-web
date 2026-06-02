import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class CircularPicturePanel extends JPanel {
    private BufferedImage image;
    private int diameter;
    private Color backgroundColor;

    public CircularPicturePanel(BufferedImage image, int diameter, Color backgroundColor) {
        this.image = image;
        this.diameter = diameter;
        this.backgroundColor = backgroundColor;
        setPreferredSize(new Dimension(diameter, diameter));
        setMinimumSize(new Dimension(diameter, diameter));
        setMaximumSize(new Dimension(diameter, diameter));
        setOpaque(false);
        setBackground(new Color(0, 0, 0, 0));
    }

    public void updateImage(BufferedImage newImage) {
        this.image = newImage;
    }

    public ImageIcon getImageIcon(int size) {
        if (image == null) return null;
        Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (image != null) {
            g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, diameter, diameter));
            g2d.drawImage(image, 0, 0, diameter, diameter, null);
        } else {
            g2d.setColor(Color.GRAY);
            g2d.fillOval(0, 0, diameter, diameter);
        }

        g2d.dispose();
    }
}

