package br.ufvjm.barbearia.system;

public final class Sistema {

    private static int totalOS;

    private Sistema() {
    }

    public static synchronized void incrementarTotalOS() {
        totalOS++;
    }

    public static synchronized int getTotalOS() {
        return totalOS;
    }
}
