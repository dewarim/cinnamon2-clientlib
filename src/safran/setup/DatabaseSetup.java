package safran.setup;

public interface DatabaseSetup {
	boolean initializeDatabase();
	void reloadCinnamon();
}
