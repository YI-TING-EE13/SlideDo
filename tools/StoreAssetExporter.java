import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Generates deterministic Google Play store assets without adding runtime
 * dependencies to the Android app.
 */
public final class StoreAssetExporter {
    private static final int FEATURE_WIDTH = 1024;
    private static final int FEATURE_HEIGHT = 500;

    private StoreAssetExporter() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: StoreAssetExporter <feature-graphic-png>");
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());

        BufferedImage image = new BufferedImage(FEATURE_WIDTH, FEATURE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawFeatureGraphic(g);
        } finally {
            g.dispose();
        }

        if (!ImageIO.write(image, "png", output.toFile())) {
            throw new IOException("No PNG writer is available.");
        }
        verifyFeatureGraphic(output);
        System.out.println("Store asset artifact:");
        System.out.println("  " + output.toAbsolutePath());
    }

    private static void drawFeatureGraphic(Graphics2D g) {
        g.setColor(color(0x172033));
        g.fillRect(0, 0, FEATURE_WIDTH, FEATURE_HEIGHT);
        fillRoundRect(g, 36, 36, 952, 428, 36, color(0x1F2937));

        g.setColor(color(0xF59E0B));
        g.fillOval(878, 54, 68, 68);
        g.setColor(color(0x34D399));
        g.fillOval(806, 380, 92, 92);

        drawTitle(g);
        drawBoard(g);
        drawPills(g);
    }

    private static void drawTitle(Graphics2D g) {
        int x = 86;
        int y = 138;
        drawText(g, "SlideDo", x, y, 78, Font.BOLD, color(0xF9FAFB));
        drawText(g, "Sliding number puzzles", x + 4, y + 64, 31, Font.BOLD, color(0xD1D5DB));
        drawText(g, "3x3 / 4x4 / 5x5", x + 4, y + 112, 26, Font.BOLD, color(0xA7F3D0));
        drawText(g, "Offline play, local records, guided practice", x + 4, y + 154, 24, Font.PLAIN, color(0xE5E7EB));
    }

    private static void drawBoard(Graphics2D g) {
        int originX = 602;
        int originY = 78;
        fillRoundRect(g, originX, originY, 320, 320, 24, color(0x0F172A));

        int[][] tiles = {
                {1, 0xF8FAFC, 0x111827},
                {2, 0xF8FAFC, 0x111827},
                {3, 0x38BDF8, 0x0F172A},
                {4, 0xF8FAFC, 0x111827},
                {5, 0x34D399, 0x052E16},
                {6, 0xF8FAFC, 0x111827},
                {7, 0xF8FAFC, 0x111827},
                {8, 0xF59E0B, 0x1F1300},
                {0, 0x1F2937, 0x1F2937}
        };

        g.setFont(new Font("Segoe UI", Font.BOLD, 42));
        for (int i = 0; i < tiles.length; i++) {
            int row = i / 3;
            int col = i % 3;
            int x = originX + 22 + col * 96;
            int y = originY + 22 + row * 96;
            fillRoundRect(g, x, y, 84, 84, 16, color(tiles[i][1]));
            if (tiles[i][0] != 0) {
                drawCenteredText(g, Integer.toString(tiles[i][0]), x, y, 84, 84, color(tiles[i][2]));
            }
        }

        g.setColor(color(0x34D399));
        g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(840, 434, 744, 434);
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(764, 394);
        arrow.lineTo(722, 434);
        arrow.lineTo(764, 474);
        g.draw(arrow);
    }

    private static void drawPills(Graphics2D g) {
        int y = 356;
        drawPill(g, 104, y, 120, "Home");
        drawPill(g, 250, y, 174, "Mode Select");
        drawPill(g, 450, y, 146, "Tutorial");
    }

    private static void drawPill(Graphics2D g, int x, int y, int width, String text) {
        fillRoundRect(g, x, y, width, 46, 23, color(0x111827));
        drawText(g, text, x + 20, y + 30, 22, Font.BOLD, color(0xD1D5DB));
    }

    private static void drawText(Graphics2D g, String text, int x, int baseline, int size, int style, Color color) {
        g.setFont(new Font("Segoe UI", style, size));
        g.setColor(color);
        g.drawString(text, x, baseline);
    }

    private static void drawCenteredText(Graphics2D g, String text, int x, int y, int width, int height, Color color) {
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = y + ((height - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setColor(color);
        g.drawString(text, textX, textY);
    }

    private static void fillRoundRect(Graphics2D g, int x, int y, int width, int height, int radius, Color color) {
        g.setColor(color);
        g.fillRoundRect(x, y, width, height, radius, radius);
    }

    private static Color color(int rgb) {
        return new Color(rgb);
    }

    private static void verifyFeatureGraphic(Path output) throws IOException {
        BufferedImage image = ImageIO.read(output.toFile());
        if (image == null) {
            throw new IOException("Generated feature graphic is not a readable image.");
        }
        if (image.getWidth() != FEATURE_WIDTH || image.getHeight() != FEATURE_HEIGHT) {
            throw new IOException("Generated feature graphic must be 1024x500.");
        }
    }
}
