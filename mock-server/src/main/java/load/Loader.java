package load;

import model.LTS;
import model.Rule;

import java.util.List;

public interface Loader {
    LTS load(String str) throws LoaderException;
}
