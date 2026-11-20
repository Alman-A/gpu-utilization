import java.io.IOException;

public interface DataStore {

    AppState load() throws IOException;

    void save(AppState state) throws IOException;
}
