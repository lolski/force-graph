package com.vaticle.force.graph.force;

import com.vaticle.force.graph.api.Vertex;

import java.util.Collection;
import java.util.function.Supplier;

public class YForce extends BaseForce {
    Supplier<Double> y;
    double strength;

    public YForce(Collection<Vertex> vertices, Supplier<Double> y, double strength) {
        super(vertices);
        this.y = y;
        this.strength = strength;
    }

    @Override
    public void apply(double alpha) {
        for (Vertex vertex : vertices()) {
            vertex.vy(vertex.vy() + (y.get() - vertex.y()) * strength * alpha);
        }
    }
}
