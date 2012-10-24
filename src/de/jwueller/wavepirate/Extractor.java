package de.jwueller.wavepirate;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Extractor {

    private static final byte[] RIFF_PATTERN = new byte[]{
        (byte) 0x52, // R
        (byte) 0x49, // I
        (byte) 0x46, // F
        (byte) 0x46  // F
    };

    private static final byte[] WAVE_PATTERN = new byte[]{
        (byte) 0x57, // W
        (byte) 0x41, // A
        (byte) 0x56, // V
        (byte) 0x45  // E
    };

    private static final int EOF = 0xFFFFFFFF;

    private File sourceFile;
    private File outputDirectory;
    private DataInputStream source;

    public Extractor(File sourceFile, File outputDirectory) {
        this.sourceFile = sourceFile;
        this.outputDirectory = outputDirectory;
    }

    public int run() throws FileNotFoundException, IOException {
        int extractedFileCount = 0;

        source = new DataInputStream(new BufferedInputStream(new FileInputStream(sourceFile)));

        try {
            // Fast-forward to the next header.
            while (skipToNext()) {
                // The next 4 bytes are a 32bit integer containing the total
                // wave file size in bytes.
                byte[] sizeBuffer = new byte[4];
                byte[] wavePatternBuffer = new byte[4];

                // Abort if the header is incomplete.
                if (source.read(sizeBuffer) == EOF || source.read(wavePatternBuffer) == EOF) {
                    continue;
                }

                // Abort if we are not dealing with a wave file.
                if (wavePatternBuffer[0] != WAVE_PATTERN[0]
                        || wavePatternBuffer[1] != WAVE_PATTERN[1]
                        || wavePatternBuffer[2] != WAVE_PATTERN[2]
                        || wavePatternBuffer[3] != WAVE_PATTERN[3]) {
                    continue;
                }

                // Determine the size of the wave file.
                extractedFileCount++;
                long size = (long) ByteBuffer.wrap(sizeBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt() + RIFF_PATTERN.length + sizeBuffer.length;
                long contentSize = size - RIFF_PATTERN.length - sizeBuffer.length - WAVE_PATTERN.length;
                System.out.println("RIFF WAVE found (" + size + " bytes)");

                // Create the output file.
                File outputFile = new File(outputDirectory, sourceFile.getName() + "_" + extractedFileCount + ".wav");
                FileOutputStream output = new FileOutputStream(outputFile);
                output.write(RIFF_PATTERN);
                output.write(sizeBuffer);
                output.write(WAVE_PATTERN);

                // TODO: This should be done in chunks, but i'll leave this here
                // for testing purposes.
                byte[] waveBuffer = new byte[(int) contentSize];
                source.readFully(waveBuffer);
                output.write(waveBuffer);

                output.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(Extractor.class.getName()).log(Level.SEVERE, null, ex);
            source.close();
            throw ex;
        }

        return extractedFileCount;
    }

    /**
     * Advances the stream until the next pattern is found.
     * 
     * @return boolean whether a pattern was found
     */
    private boolean skipToNext() throws IOException {
        short p = 0;

        while (p < RIFF_PATTERN.length) {
            int bRaw = source.read();
            byte b = (byte) bRaw;

            if (bRaw == EOF) return false;

            if (b == RIFF_PATTERN[p]) {
                p++;
            } else {
                p = 0;
                if (b == RIFF_PATTERN[0]) p++;
            }
        }

        return true;
    }
}
