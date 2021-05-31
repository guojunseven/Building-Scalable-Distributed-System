package service;

/**
 * This interface defines an abstraction of various text processor class a
 * nd provides method for validating the text and analyzing the text to get result.
 * It is a thread-safe interface.
 */
public interface TextProcessor {

    /**
     * Apply analysis on the current target string.
     * The method must be applied after validating the string.
     * @param target the target string
     * @return the result as a integer
     */
    public int apply(String target);

    /**
     * Validate the string target and
     * return false if the string is not valid for analysis.
     * @param target the target string
     * @return true if the target is valid and vice versa
     */
    public boolean validate(String target);
}
