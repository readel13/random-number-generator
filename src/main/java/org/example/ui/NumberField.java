package org.example.ui;

import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;


@NoArgsConstructor
public class NumberField extends JTextField {

    private NumberDocument defaultModel = null;


    public NumberField(int columns, int number) {
        super(columns);
        setText(String.valueOf(number));
    }

    @Override
    protected Document createDefaultModel() {
        this.defaultModel = new NumberDocument();
        return defaultModel;
    }

    private void setMin(int min) {
        defaultModel.setMin(min);
    }

    private void setMax(int max) {
        defaultModel.setMax(max);
    }

    @Setter
    @NoArgsConstructor
    static class NumberDocument extends PlainDocument {
        private int max = 100;
        private int min = 0;

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (StringUtils.isNumericSpace(str)) {

                int number = Integer.parseInt(str);

                if (number >= min && number <= max) {
                    super.insertString(offs, str, a);
                }
            }
        }
    }
}