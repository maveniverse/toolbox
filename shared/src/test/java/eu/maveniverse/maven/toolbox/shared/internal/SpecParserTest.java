/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SpecParserTest {
    @Test
    void nonOpRootIsError() {
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse("aaa"));
    }

    @Test
    void gibberish() {
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse("a("));
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse(",,,(("));
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse("a(,)"));
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse("a(b,)"));
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse("a(b,,)"));
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse("a)b,,)"));
        assertThrows(IllegalArgumentException.class, () -> SpecParser.parse("a(b"));
    }

    @Test
    void opRoot() {
        SpecParser.Op root = SpecParser.parse("aaa()");
        assertEquals("aaa", root.getValue());
        assertInstanceOf(SpecParser.Op.class, root);
        assertTrue(root.getChildren().isEmpty());

        root.accept(new SpecParser.Dump());
    }

    @Test
    void opRootWithStringLiteral() {
        SpecParser.Op root = SpecParser.parse("aaa(zzz)");
        assertEquals("aaa", root.getValue());
        assertEquals(1, root.getChildren().size());
        SpecParser.Node child = root.getChildren().iterator().next();
        assertEquals("zzz", child.getValue());

        root.accept(new SpecParser.Dump());
    }

    @Test
    void example() {
        SpecParser.Op root = SpecParser.parse("a(b(One, tWo, *), c(${param}), bbb)");
        assertEquals("a", root.getValue());
        assertEquals(3, root.getChildren().size());

        SpecParser.Op b = (SpecParser.Op) root.getChildren().get(0);
        assertEquals("b", b.getValue());
        assertEquals(3, b.getChildren().size());
        assertInstanceOf(SpecParser.Literal.class, b.getChildren().get(0));
        assertEquals("One", b.getChildren().get(0).getValue());
        assertInstanceOf(SpecParser.Literal.class, b.getChildren().get(1));
        assertEquals("tWo", b.getChildren().get(1).getValue());
        assertInstanceOf(SpecParser.Literal.class, b.getChildren().get(2));
        assertEquals("*", b.getChildren().get(2).getValue());

        SpecParser.Op c = (SpecParser.Op) root.getChildren().get(1);
        assertEquals("c", c.getValue());
        assertEquals(1, c.getChildren().size());
        assertInstanceOf(SpecParser.Literal.class, c.getChildren().get(0));
        assertEquals("${param}", c.getChildren().get(0).getValue());

        SpecParser.Literal bbb = (SpecParser.Literal) root.getChildren().get(2);
        assertEquals("bbb", bbb.getValue());

        root.accept(new SpecParser.Dump());
    }
}
