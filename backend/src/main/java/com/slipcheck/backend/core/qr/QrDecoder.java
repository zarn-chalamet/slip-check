package com.slipcheck.backend.core.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Local QR decoding with ZXing. Tries the original image first, then variations
 * (grayscale, upscaled, 90/180/270 rotations) with TRY_HARDER enabled.
 * No external calls; the image never leaves memory.
 */
@Component
public class QrDecoder {

    public Optional<String> decode(byte[] imageBytes) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            return Optional.empty();
        }
        if (image == null) {
            return Optional.empty();
        }
        for (BufferedImage variant : variants(image)) {
            Optional<String> text = tryDecode(variant);
            if (text.isPresent()) {
                return text;
            }
        }
        return Optional.empty();
    }

    private List<BufferedImage> variants(BufferedImage original) {
        List<BufferedImage> list = new ArrayList<>(8);
        list.add(original);
        list.add(toGrayscale(original));
        list.add(upscale(original, 2));
        list.add(rotate(original, 90));
        list.add(rotate(original, 180));
        list.add(rotate(original, 270));
        list.add(upscale(toGrayscale(original), 2));
        return list;
    }

    private Optional<String> tryDecode(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE));
            Result result = new MultiFormatReader().decode(bitmap, hints);
            String text = result.getText();
            return (text == null || text.isBlank()) ? Optional.empty() : Optional.of(text);
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    private static BufferedImage upscale(BufferedImage src, int factor) {
        int w = src.getWidth() * factor;
        int h = src.getHeight() * factor;
        BufferedImage up = new BufferedImage(w, h, src.getType() == 0 ? BufferedImage.TYPE_INT_RGB : src.getType());
        Graphics2D g = up.createGraphics();
        g.drawImage(src.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return up;
    }

    private static BufferedImage rotate(BufferedImage src, int degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);
        BufferedImage rotated = new BufferedImage(newW, newH,
                src.getType() == 0 ? BufferedImage.TYPE_INT_RGB : src.getType());
        Graphics2D g = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newW - w) / 2.0, (newH - h) / 2.0);
        at.rotate(radians, w / 2.0, h / 2.0);
        g.setTransform(at);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rotated;
    }
}
