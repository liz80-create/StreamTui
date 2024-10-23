//package com.streamtui;
//
//import javax.imageio.ImageIO;
//import javax.swing.*;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//
//public class VideoPlayer extends JPanel {
//    private BufferedImage currentFrame;
//
//    public VideoPlayer() {
//        JFrame frame = new JFrame("Live Streaming");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(1020, 720);
//        frame.add(this);
//        frame.setVisible(true);
//    }
//
//    public void updateFrame(byte[] framedata, int width, int height) {
//        try{
//            ByteArrayInputStream bais = new ByteArrayInputStream(framedata);
//            BufferedImage frameimage = ImageIO.read(bais);
//            if (frameimage != null) {
//                this.currentFrame = frameimage;
//                repaint();  // Trigger re-rendering
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//        if (currentFrame != null) {
//            // Draw the current frame
//            g.drawImage(currentFrame, 0, 0, this.getWidth(), this.getHeight(), null);
//        }
//    }
//
//    public static void startVideoPlayer(VideoPlayer videoPlayer) {
//        JFrame frame = new JFrame("Live Streaming");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.add(videoPlayer);
//        frame.pack();
//        frame.setVisible(true);
//    }
//
//    public static void main(String[] args) {
//        VideoPlayer videoPlayer = new VideoPlayer();
//    }
//}
package com.streamtui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class VideoPlayer extends JPanel {
    private BufferedImage currentFrame;
    private final int width;
    private final int height;
    private final JFrame frame;

    public VideoPlayer(int width, int height) {
        this.width = width;
        this.height = height;

        // Create a compatible BufferedImage for better performance
        this.currentFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Set preferred size based on video dimensions
        setPreferredSize(new Dimension(width, height));

        // Initialize frame
        frame = new JFrame("Live Streaming");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void updateFrame(byte[] frameData, int width, int height) {
        if (frameData == null || frameData.length != width * height * 4) {
            System.err.println("Invalid frame data");
            return;
        }

        // Get the underlying int array of the BufferedImage
        int[] imageData = ((DataBufferInt) currentFrame.getRaster().getDataBuffer()).getData();

        // Convert ABGR byte array to INT_ARGB
        for (int i = 0; i < width * height; i++) {
            int baseIndex = i * 4;
            int red = frameData[baseIndex] & 0xFF;
            int green = frameData[baseIndex + 1] & 0xFF;
            int blue = frameData[baseIndex + 2] & 0xFF;
            int alpha = frameData[baseIndex + 3] & 0xFF;
            imageData[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        // Request repaint on EDT
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentFrame != null) {
            Graphics2D g2d = (Graphics2D) g;
            // Enable bilinear interpolation for smoother scaling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // Draw the frame
            g2d.drawImage(currentFrame, 0, 0, getWidth(), getHeight(), null);
        }
    }

    public static void main(String[] args) {
        // Ensure we're running on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Create a VideoPlayer instance
            VideoPlayer player = new VideoPlayer(1020, 720);

            // Create a test animation thread
            Thread animationThread = new Thread(() -> {
                try {
                    // Create test frame data (showing a moving gradient)
                    byte[] frameData = new byte[1020 * 720 * 4];
                    int offset = 0;

                    while (true) {
                        // Fill frame data with a moving gradient pattern
                        for (int y = 0; y < 720; y++) {
                            for (int x = 0; x < 1020; x++) {
                                int baseIndex = (y * 1020 + x) * 4;
                                frameData[baseIndex] = (byte) 255; // Alpha
                                frameData[baseIndex + 1] = (byte) ((x + offset) % 255); // Blue
                                frameData[baseIndex + 2] = (byte) (y % 255); // Green
                                frameData[baseIndex + 3] = (byte) ((x + y + offset) % 255); // Red
                            }
                        }

                        // Update the frame
                        player.updateFrame(frameData, 1020, 720);

                        // Move the gradient
                        offset = (offset + 1) % 255;

                        // Add a small delay
                        Thread.sleep(16); // Approximately 60 FPS
                    }
                } catch (InterruptedException e) {
                    System.out.println("Animation thread interrupted");
                }
            });

            // Start the animation
            animationThread.start();
        });
    }
}