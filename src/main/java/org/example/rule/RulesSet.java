package org.example.rule;

public class RulesSet {

    public static Rule[] VERTICAL_RULES_ARRAY = new Rule[]{
            RulesSet::rule90, RulesSet::rule150,
    };
    public static Rule[] HORIZONTAL_RULES_ARRAY = new Rule[]{
            RulesSet::rule30, RulesSet::rule105,
    };

    public static boolean rule30(boolean p, boolean q, boolean r) {
        return p ^ (q || r);
    }

    public static boolean rule90(boolean p, boolean q, boolean r) {
        return p ^ r;
    }

    public static boolean rule105(boolean p, boolean q, boolean r) {
        return p ^ q ^ (!r);
    }

    public static boolean rule150(boolean p, boolean q, boolean r) {
        return p ^ q ^ r;
    }


}