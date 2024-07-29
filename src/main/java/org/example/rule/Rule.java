package org.example.rule;

@FunctionalInterface
public interface Rule {
    boolean step(boolean left, boolean middle, boolean right);
}
