package org.example.ui.dropdown;

import org.example.rule.Rule;
import org.example.rule.RulesSet;

import javax.swing.*;
import java.util.Map;

public class CARuleDropdown extends JComboBox<String> {

    public static final Map<String, Rule> RULES = Map.of(
            "Rule30", RulesSet::rule30,
            "Rule90", RulesSet::rule90,
            "Rule105", RulesSet::rule105,
            "Rule150", RulesSet::rule150
    );

    public CARuleDropdown() {

        for (String s : RULES.keySet()) {
            addItem(s);
        }

        setSelectedIndex(0);
    }


    public Rule getSelectedRule() {
        return RULES.get(getSelectedItem().toString());
    }

}
