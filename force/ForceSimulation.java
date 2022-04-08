/*
 * Copyright (C) 2021 Vaticle
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.vaticle.force.graph.force;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public class ForceSimulation {
    private double alpha;
    private double alphaMin;
    private double alphaDecay;
    private double alphaTarget;
    private double velocityDecay;
    private final Map<String, Force> forces;
    private final ConcurrentMap<Integer, Node> nodes;

    private static final int INITIAL_RADIUS = 10;
    private static final double INITIAL_ANGLE = Math.PI * (3 - Math.sqrt(5));

    public ForceSimulation() {
        alpha = 1;
        alphaMin = 0.001;
        alphaDecay = 1 - Math.pow(alphaMin, 1.0 / 300);
        alphaTarget = 0;
        velocityDecay = 0.6;
        forces = new HashMap<>();
        nodes = new ConcurrentHashMap<>();
    }

    public synchronized void tick() {
        alpha += (alphaTarget - alpha) * alphaDecay;

        forces.values().forEach(force -> force.apply(alpha));

        for (Node node : nodes.values()) {
            if (node.isXFixed()) node.vx = 0;
            else {
                node.vx *= velocityDecay;
                node.x(node.x() + node.vx);
            }
            if (node.isYFixed()) node.vy = 0;
            else {
                node.vy *= velocityDecay;
                node.y(node.y() + node.vy);
            }
        }
    }

    public synchronized Collection<Node> addNodes(Collection<InputNode> inputNodes) {
        final Stream<Node> nodeStream = inputNodes.stream().map(this::placeNode);
        forces.values().forEach(Force::init);
        return nodeStream.collect(Collectors.toList());
    }

    protected Node placeNode(InputNode inputNode) {
        AtomicBoolean added = new AtomicBoolean(false);
        final Node node = nodes.computeIfAbsent(inputNode.id, (id) -> {
            added.set(true);
            double radius = INITIAL_RADIUS * Math.sqrt(0.5 + inputNode.id);
            double angle = inputNode.id * INITIAL_ANGLE;
            double x = inputNode.fixedX != null ? inputNode.fixedX : radius * Math.cos(angle);
            double y = inputNode.fixedY != null ? inputNode.fixedY : radius * Math.sin(angle);
            return new Node(id, x, y, inputNode.fixedX != null, inputNode.fixedY != null);
        });
        if (!added.get()) throw new IllegalStateException("The node ID " + inputNode.id + " is already contained in the force simulation, so it cannot be added.");
        return node;
    }

    public ConcurrentMap<Integer, Node> nodes() {
        return nodes;
    }

    public double alpha() {
        return alpha;
    }

    public ForceSimulation alpha(double value) {
        alpha = value;
        return this;
    }

    public double alphaMin() {
        return alphaMin;
    }

    public ForceSimulation alphaMin(double value) {
        alphaMin = value;
        return this;
    }

    public double alphaDecay() {
        return alphaDecay;
    }

    public ForceSimulation alphaDecay(double value) {
        alphaDecay = value;
        return this;
    }

    public double alphaTarget() {
        return alphaTarget;
    }

    public ForceSimulation alphaTarget(double value) {
        alphaTarget = value;
        return this;
    }

    public double velocityDecay() {
        return velocityDecay;
    }

    public ForceSimulation velocityDecay(double value) {
        velocityDecay = value;
        return this;
    }

    public Force force(String name) {
        return forces.get(name);
    }

    public ForceSimulation force(String name, Force force) {
        if (force != null) {
            forces.put(requireNonNull(name), force);
            force.init();
        } else {
            forces.remove(name);
        }
        return this;
    }

    public synchronized void clear() {
        forces.clear();
        nodes.clear();
    }

    public static class InputNode {
        final int id;
        final Double fixedX;
        final Double fixedY;

        public InputNode(int id) {
            this(id, null, null);
        }

        public InputNode(int id, Double fixedX, Double fixedY) {
            this.id = id;
            this.fixedX = fixedX;
            this.fixedY = fixedY;
        }
    }

    public static class InputLink {
        final int source;
        final int target;

        public InputLink(int source, int target) {
            this.source = source;
            this.target = target;
        }
    }
}