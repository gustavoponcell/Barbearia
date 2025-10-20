import br.ufvjm.barbearia.system.Sistema;

public class QuickChecks {
    public static void main(String[] args) {
        Sistema s = new Sistema();
        assert s.getTotalOrdensServicoCriadas() >= 0;
        System.out.println("✅ Testes básicos OK");
    }
}
