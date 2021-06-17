package service;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A service class serving as a processor to count the number of words in a Line.
 */
public class WordCountService implements TextProcessor {
    @Override
    public int apply(String target) {
        return 1;
    }

    @Override
    public boolean validate(String target) {
        return target != null && ! target.isEmpty();
    }
}
