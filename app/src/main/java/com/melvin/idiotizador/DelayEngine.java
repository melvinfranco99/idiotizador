package com.melvin.idiotizador;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Process;

/**
 * Captura audio del micrófono y lo reproduce con un retardo configurable
 * mediante una línea de retardo circular.
 */
public class DelayEngine {

    private static final int SAMPLE_RATE = 44100;
    private static final int MAX_DELAY_MS = 2000;

    private volatile boolean running;
    private volatile int delayMs = 500;
    private Thread thread;

    public void setDelayMs(int ms) {
        delayMs = Math.max(0, Math.min(MAX_DELAY_MS, ms));
    }

    public boolean isRunning() {
        return running;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::loop, "DelayEngine");
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        int minRec = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int minPlay = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minRec * 2);

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(minPlay * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        // Tamaño de bloque pequeño (~10 ms) para minimizar la latencia añadida.
        int chunk = Math.max(SAMPLE_RATE / 100, minRec / 4);
        short[] in = new short[chunk];
        short[] out = new short[chunk];

        // Latencia aproximada del propio sistema (buffers de entrada y salida),
        // que se descuenta para que el retardo total percibido se acerque al pedido.
        int systemLatency = chunk + minPlay / 2;

        int ringSize = SAMPLE_RATE * MAX_DELAY_MS / 1000 + chunk + 1;
        short[] ring = new short[ringSize];
        int writePos = 0;

        try {
            record.startRecording();
            track.play();

            while (running) {
                int n = record.read(in, 0, chunk);
                if (n <= 0) continue;

                int delaySamples = Math.max(0,
                        (int) ((long) SAMPLE_RATE * delayMs / 1000) - systemLatency);

                for (int i = 0; i < n; i++) {
                    ring[writePos] = in[i];
                    int readPos = writePos - delaySamples;
                    if (readPos < 0) readPos += ringSize;
                    out[i] = ring[readPos];
                    writePos++;
                    if (writePos == ringSize) writePos = 0;
                }
                track.write(out, 0, n);
            }
        } finally {
            try { record.stop(); } catch (IllegalStateException ignored) { }
            try { track.stop(); } catch (IllegalStateException ignored) { }
            record.release();
            track.release();
            running = false;
        }
    }
}
