/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.io.IOConsumer;
import eu.maveniverse.maven.toolbox.shared.io.IOSupplier;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.ContentFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Class that parses and writes possibly manipulated JDOM to file.
 */
public final class JDomDocumentIO implements Closeable {
    private final IOConsumer<String> consumer;
    private final String lineSeparator;
    private final String body;
    private final String head;
    private final String tail;
    private final Document document;

    public JDomDocumentIO(Path file) throws IOException {
        this(
                () -> Files.readString(file, StandardCharsets.UTF_8),
                s -> Files.writeString(file, s, StandardCharsets.UTF_8));
    }

    public JDomDocumentIO(IOSupplier<String> supplier, IOConsumer<String> consumer) throws IOException {
        this(supplier, consumer, System.lineSeparator());
    }

    public JDomDocumentIO(IOSupplier<String> supplier, IOConsumer<String> consumer, String lineSeparator)
            throws IOException {
        requireNonNull(supplier, "supplier");
        this.consumer = requireNonNull(consumer, "consumer");
        this.lineSeparator = requireNonNull(lineSeparator, "lineSeparator");

        this.body = normalizeLineEndings(supplier.get(), lineSeparator);
        SAXBuilder builder = new SAXBuilder();
        builder.setJDOMFactory(new LocatedJDOMFactory());
        try {
            this.document = builder.build(new StringReader(body));
        } catch (JDOMException e) {
            throw new IOException(e);
        }
        normaliseLineEndings(document, lineSeparator);

        int headIndex = body.indexOf("<" + document.getRootElement().getName());
        if (headIndex >= 0) {
            this.head = body.substring(0, headIndex);
        } else {
            this.head = null;
        }
        String lastTag = "</" + document.getRootElement().getName() + ">";
        int tailIndex = body.lastIndexOf(lastTag);
        if (tailIndex >= 0) {
            this.tail = body.substring(tailIndex + lastTag.length());
        } else {
            this.tail = null;
        }
    }

    public Document getDocument() {
        return document;
    }

    @Override
    public void close() throws IOException {
        Format format = Format.getRawFormat();
        format.setLineSeparator(lineSeparator);
        XMLOutputter out = new XMLOutputter(format);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (OutputStream outputStream = output) {
            if (head != null) {
                outputStream.write(head.getBytes(StandardCharsets.UTF_8));
            }
            out.output(document.getRootElement(), outputStream);
            if (tail != null) {
                outputStream.write(tail.getBytes(StandardCharsets.UTF_8));
            }
        }
        String newBody = output.toString(StandardCharsets.UTF_8);
        if (!Objects.equals(body, newBody)) {
            consumer.accept(newBody);
        }
    }

    private static void normaliseLineEndings(Document document, String lineSeparator) {
        for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.COMMENT)); i.hasNext(); ) {
            Comment c = (Comment) i.next();
            c.setText(normalizeLineEndings(c.getText(), lineSeparator));
        }
        for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.CDATA)); i.hasNext(); ) {
            CDATA c = (CDATA) i.next();
            c.setText(normalizeLineEndings(c.getText(), lineSeparator));
        }
    }

    private static String normalizeLineEndings(String text, String separator) {
        String norm = text;
        if (text != null) {
            norm = text.replaceAll("(\r\n)|(\n)|(\r)", separator);
        }
        return norm;
    }
}
