package com.atlassian.plugin.webresource.transformer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * This class is taken from CommonsIO 2.0.1 and usage should be replaced with that class when the dependency is updated
 */
class WriterOutputStream extends OutputStream
{
    private final Writer writer;
    private final CharsetDecoder decoder;
    private final boolean writeImmediately;
    private final ByteBuffer decoderIn;
    private final CharBuffer decoderOut;

    WriterOutputStream(final Writer writer, final Charset charset, final int bufferSize, final boolean writeImmediately)
    {
        decoderIn = ByteBuffer.allocate(128);

        this.writer = writer;
        decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        decoder.replaceWith("?");
        this.writeImmediately = writeImmediately;
        decoderOut = CharBuffer.allocate(bufferSize);
    }

    WriterOutputStream(final Writer writer, final Charset charset)
    {
        this(writer, charset, 1024, false);
    }

    WriterOutputStream(final Writer writer, final String charsetName, final int bufferSize, final boolean writeImmediately)
    {
        this(writer, Charset.forName(charsetName), bufferSize, writeImmediately);
    }

    WriterOutputStream(final Writer writer, final String charsetName)
    {
        this(writer, charsetName, 1024, false);
    }

    WriterOutputStream(final Writer writer)
    {
        this(writer, Charset.defaultCharset(), 1024, false);
    }

    @Override
    public void write(final byte[] b, int off, int len) throws IOException
    {
        while (len > 0)
        {
            final int c = Math.min(len, decoderIn.remaining());
            decoderIn.put(b, off, c);
            processInput(false);
            len -= c;
            off += c;
        }
        if (writeImmediately)
        {
            flushOutput();
        }
    }

    @Override
    public void write(final byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    @Override
    public void write(final int b) throws IOException
    {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void flush() throws IOException
    {
        flushOutput();
        writer.flush();
    }

    @Override
    public void close() throws IOException
    {
        processInput(true);
        flushOutput();
        writer.close();
    }

    private void processInput(final boolean endOfInput) throws IOException
    {
        decoderIn.flip();
        CoderResult coderResult;
        while (true)
        {
            coderResult = decoder.decode(decoderIn, decoderOut, endOfInput);
            if (!(coderResult.isOverflow()))
            {
                break;
            }
            flushOutput();
        }
        if (!(coderResult.isUnderflow()))
        {
            throw new IOException("Unexpected coder result");
        }

        decoderIn.compact();
    }

    private void flushOutput() throws IOException
    {
        if (decoderOut.position() > 0)
        {
            writer.write(decoderOut.array(), 0, decoderOut.position());
            decoderOut.rewind();
        }
    }
}