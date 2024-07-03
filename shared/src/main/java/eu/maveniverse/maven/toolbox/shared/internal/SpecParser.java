/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

/**
 * Simple spec parser. Parses input string, and produces a tree of {@link Op} and {@link Literal}s. Root must
 * always be {@link Op}. This tree can be transformed into something then...
 */
public final class SpecParser {
    private SpecParser() {}

    public interface Visitor {
        boolean visitEnter(Node node);

        boolean visitExit(Node node);
    }

    public static final class Dump implements Visitor {
        private final ArrayDeque<Node> nodes = new ArrayDeque<>();

        @Override
        public boolean visitEnter(Node node) {
            System.out.println(
                    IntStream.rangeClosed(0, nodes.size()).mapToObj(i -> "  ").collect(Collectors.joining())
                            + node.getValue()
                            + (node instanceof Literal ? " (lit)" : " (op)"));
            nodes.push(node);
            return true;
        }

        @Override
        public boolean visitExit(Node node) {
            if (!nodes.isEmpty()) {
                nodes.pop();
            }
            return true;
        }
    }

    public abstract static class Builder implements Visitor {
        protected final ArrayList<Object> params = new ArrayList<>();
        protected final VersionScheme versionScheme;
        protected final Map<String, ?> properties;

        public Builder(VersionScheme versionScheme, Map<String, ?> properties) {
            this.versionScheme = requireNonNull(versionScheme);
            this.properties = new HashMap<>(properties);
        }

        @Override
        public boolean visitEnter(SpecParser.Node node) {
            return true;
        }

        @Override
        public boolean visitExit(SpecParser.Node node) {
            if (node instanceof SpecParser.Literal) {
                processLiteral(node);
            } else if (node instanceof SpecParser.Op) {
                processOp(node);
            }
            return true;
        }

        protected void processLiteral(Node node) {
            String value = node.getValue();
            if (value.startsWith("${") && value.endsWith("}")) {
                if (properties == null) {
                    throw new IllegalStateException("reference used without properties defined");
                }
                Object referenced = properties.get(value.substring(2, value.length() - 1));
                if (referenced == null) {
                    referenced = "";
                }
                params.add(referenced);
            } else {
                params.add(value);
            }
        }

        /**
         * Implement this, by using "ops" of the thing being built.
         * <p>
         * Important: parameters are REVERSED, so are nibbles but in reversed, so instead {@code [[a,b,c],[d,e]]} we
         * have nibbles reversed: {@code [[c,b,a],[e,d]]}.
         */
        protected abstract void processOp(Node node);

        protected String stringParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return (String) params.remove(params.size() - 1);
        }

        protected List<String> stringParams(String op) {
            ArrayList<String> result = new ArrayList<>();
            while (!params.isEmpty()) {
                if (params.get(params.size() - 1) instanceof String) {
                    result.add(stringParam(op));
                } else {
                    break;
                }
            }
            return result;
        }

        protected boolean booleanParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return Boolean.parseBoolean((String) params.remove(params.size() - 1));
        }

        protected int intParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return Integer.parseInt((String) params.remove(params.size() - 1));
        }

        protected Version versionParam(String op) {
            try {
                return versionScheme.parseVersion(stringParam(op));
            } catch (InvalidVersionSpecificationException e) {
                throw new IllegalArgumentException("invalid version parameter for " + op, e);
            }
        }

        protected <T> T typedParam(Class<T> clazz, String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return clazz.cast(params.remove(params.size() - 1));
        }

        protected <T> List<T> typedParams(Class<T> clazz, String op) {
            ArrayList<T> result = new ArrayList<>();
            while (!params.isEmpty()) {
                if (clazz.isInstance(params.get(params.size() - 1))) {
                    result.add(typedParam(clazz, op));
                } else {
                    break;
                }
            }
            return result;
        }

        public <T> T build(Class<T> clazz) {
            if (params.size() != 1) {
                throw new IllegalArgumentException("bad spec");
            }
            return clazz.cast(params.get(0));
        }
    }

    public static class Node {
        private final String value;
        private final List<Node> children;

        private Node(String value) {
            this.value = value;
            this.children = new ArrayList<>();
        }

        public String getValue() {
            return value;
        }

        public void addChild(Node node) {
            children.add(node);
        }

        public List<Node> getChildren() {
            return children;
        }

        public boolean accept(Visitor visitor) {
            if (visitor.visitEnter(this)) {
                for (Node child : children) {
                    if (!child.accept(visitor)) {
                        break;
                    }
                }
            }

            return visitor.visitExit(this);
        }
    }

    public abstract static class Literal extends Node {
        private Literal(String value) {
            super(value);
        }

        @Override
        public void addChild(Node node) {
            throw new IllegalStateException("string literal is leaf");
        }

        @Override
        public List<Node> getChildren() {
            return Collections.emptyList();
        }
    }

    public static class StringLiteral extends Literal {
        StringLiteral(String value) {
            super(value);
        }
    }

    public static final class Op extends Node {
        Op(String name) {
            super(name);
        }
    }

    /**
     * Spec parsing: spec may be in form of "aaa" when it is string literal, or "aaa()" when it is a function. The
     * spec expression MUST start with function. A function may be {@code 0..n} arguments, that may be string literals
     * or other ops.
     */
    public static Op parse(String spec) {
        requireNonNull(spec);
        Op root = null;
        ArrayDeque<Node> path = new ArrayDeque<>();
        String value = "";
        boolean wasComma = false;
        for (int idx = 0; idx < spec.length(); idx++) {
            char ch = spec.charAt(idx);
            if (!Character.isWhitespace(ch)) {
                if (Character.isAlphabetic(ch)
                        || Character.isDigit(ch)
                        || '*' == ch
                        || ':' == ch
                        || '.' == ch
                        || '-' == ch
                        || '/' == ch
                        || '\\' == ch
                        || '$' == ch
                        || '{' == ch
                        || '}' == ch) {
                    value += ch;
                } else if ('(' == ch) {
                    Op op = new Op(value);
                    if (root == null) {
                        root = op;
                    }
                    value = "";
                    if (!path.isEmpty()) {
                        path.peek().addChild(op);
                    }
                    wasComma = false;
                    path.push(op);
                } else if (')' == ch) {
                    if (!value.isEmpty()) {
                        Literal literal = new StringLiteral(value);
                        value = "";
                        if (!path.isEmpty()) {
                            path.peek().addChild(literal);
                        } else {
                            throw new IllegalArgumentException("misplaced closing braces");
                        }
                    } else if (wasComma) {
                        throw new IllegalArgumentException("misplaced comma");
                    }
                    wasComma = false;
                    path.pop();
                } else if (',' == ch) {
                    if (!value.isEmpty()) {
                        Literal literal = new StringLiteral(value);
                        value = "";
                        if (!path.isEmpty()) {
                            path.peek().addChild(literal);
                        } else {
                            throw new IllegalArgumentException("misplaced comma");
                        }
                    } else if (wasComma) {
                        throw new IllegalArgumentException("misplaced comma");
                    }
                    wasComma = true;
                } else {
                    throw new IllegalArgumentException("invalid character: " + ch);
                }
            }
        }
        if (root == null || !path.isEmpty()) {
            throw new IllegalArgumentException("invalid spec string");
        }
        return root;
    }
}
