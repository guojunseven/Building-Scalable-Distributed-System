package model;

/**
 * This is a class for signify something. And its status can be modified to reflect infomation.
 */
public class Signal {

    private boolean sign;

    public boolean get() {
        return sign;
    }

    public void set(boolean sign) {
        this.sign = sign;
    }
}
