package net.minesky.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import net.minesky.MineSkyDiscord;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class PIXUtils {

    public static final String chavePix = MineSkyDiscord.config.getString("auto-pix.chave-pix");
    public static final String nomePessoa = MineSkyDiscord.config.getString("auto-pix.name"); // minesky
    public static final String cidadePessoa = MineSkyDiscord.config.getString("auto-pix.city");

    //               CHANNEL ID |
    public static HashMap<String, String> pixCodes = new HashMap<>();

    private static String gerarPix(String valor) {
        // Formata o valor para sempre ter duas casas decimais
        valor = String.format("%.2f", Double.parseDouble(valor)).replace(',', '.');

        // Monta o payload do Pix
        String pixSemCRC = "000201" +  // Payload Format Indicator
                "01021126580014br.gov.bcb.pix01" + String.format("%02d", chavePix.length()) + chavePix + // Chave Pix
                "52040000" + // Merchant Category Code
                "5303986" +  // Moeda BRL (986)
                "54" + String.format("%02d", valor.length()) + valor + // Valor da transação
                "5802BR" +  // Código do país
                "59" + String.format("%02d", nomePessoa.length()) + nomePessoa + // Nome do recebedor
                "60" + String.format("%02d", cidadePessoa.length()) + cidadePessoa + // Cidade do recebedor
                "62070503***"; // Campo adicional opcional

        // Calcula o CRC16
        String crc16 = calcularCRC16(pixSemCRC + "6304");
        return pixSemCRC + "6304" + crc16;
    }

    private static String calcularCRC16(String payload) {
        int crc = 0xFFFF;
        int polinomio = 0x1021;

        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ polinomio;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF; // Mantém apenas os 16 bits inferiores
            }
        }

        return String.format("%04X", crc); // Retorna em maiúsculas com 4 caracteres
    }

    public static CompletableFuture<File> gerarQRCodeAsync(String pix, int tamanho) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File f = new File(MineSkyDiscord.getInstance().getDataFolder(), "qrcode_pix.png");
                BitMatrix matrix = new MultiFormatWriter().encode(pix, BarcodeFormat.QR_CODE, tamanho, tamanho);
                BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
                ImageIO.write(image, "png", f);
                return f;
            } catch (WriterException | IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public static String getCodigoPix(String valor) {
        return gerarPix(valor);
    }

}
