package de.olafklischat.esmapper.json;

public class NumbersHolder {
    private int x, y, z;
    private int[] numbers;
    public NumbersHolder() {
    }
    public NumbersHolder(int x, int y, int z, int[] numbers) {
        super();
        this.x = x;
        this.y = y;
        this.z = z;
        this.numbers = numbers;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getZ() {
        return z;
    }
    public int[] getNumbers() {
        return numbers;
    }
}
