package testhelpers;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import anime_parsing.Parser;
import anime_parsing.EmfHolder;

import static org.mockito.ArgumentMatchers.any;

public final class TestHelpers {
    private TestHelpers() {}

    public static MockedStatic<Parser> mockParserDoNothing() {
        MockedStatic<Parser> ms = Mockito.mockStatic(Parser.class);
        ms.when(() -> Parser.saveAnimeToDB(any())).thenAnswer(invocation -> null);
        return ms;
    }
}